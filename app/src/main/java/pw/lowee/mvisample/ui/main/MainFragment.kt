package pw.lowee.mvisample.ui.main

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.broadcast
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import pw.lowee.mvikungfu.*
import pw.lowee.mvisample.EitherE
import pw.lowee.mvisample.Left
import pw.lowee.mvisample.R
import pw.lowee.mvisample.Right
import pw.lowee.mvisample.data.DataSource
import pw.lowee.mvisample.models.GifObject
import pw.lowee.mvisample.ui.common.DelegateAdapter
import pw.lowee.mvisample.ui.common.ImageLoader

private typealias MainMsg = Msg<MainCtx, MainState>
private typealias MainEffect = Effect<MainCtx, MainState>
private typealias MainView = MainFragment
private typealias MainRender = Render<MainView, MainState>

class MainRepository(private val dataSource: DataSource) {

    var searchJob: Job? = null

    suspend fun search(query: String, offset: Int, limit: Int): EitherE<List<GifObject>> {
        searchJob?.cancel()
        return coroutineScope { async { dataSource.search(query, offset, limit) } }.also { searchJob = it }.await()
    }

}

interface MainCtx {
    val repository: MainRepository
    val failToast: (Exception) -> Unit
}

@Parcelize
data class MainState(
    val query: String = "",
    val results: List<GifObject> = emptyList(),
    val resultsQuery: String = "",
    val loading: Boolean = false
) : Parcelable

fun MainController(ctx: MainCtx) = object : ElmController<MainCtx, MainState>(MainState()) {
    override fun effectContext(): MainCtx = ctx
    override fun init(): MainMsg = msgState { it }
}

class MainFragment : Fragment() {

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisor)

    private var saveState: (Bundle) -> Unit = {}

    private val effectCtx = object : MainCtx {
        override val repository = MainRepository(get())
        override val failToast: (Exception) -> Unit =
            { ex -> scope.launch { context?.let { Toast.makeText(it, ex.message, Toast.LENGTH_LONG).show() } } }
    }
    private val controller by lazy { MainController(effectCtx) }
    private val renders: List<MainRender> = listOf(
        renderItems(),
        renderRefreshing(),
        renderLoading(),
        renderQuery()
    )

    private val imageLoader: ImageLoader by inject()
    val adapter: DelegateAdapter<MainElement> = DelegateAdapter(
        loaderDelegate<MainElement, MainElement.Loader>(),
        itemDelegate(imageLoader)
    )
    val queryWatcher = textWatcher { controller.messages.offer(msgQuery(it)) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope.launch {
            while (!controller.states.isEmpty) controller.states.receive()
            val os = controller.states.broadcast(1024)::openSubscription
            val (rs, ss, ls) = Triple(os(), os(), os())
            launch { for (r in renderStates(rs, renders)) r(this@MainFragment) }
            launch {
                for (s in ss) saveState = {
                    it.putParcelable("state", s)
                    Log.d("State", "Saved: $s")
                }
            }
            launch { for (s in ls) Log.d("State", "$s") }
            savedInstanceState
                ?.getParcelable<MainState>("state")
                ?.also { Log.d("State", "Restored: $it") }
                ?.let { s -> controller.messages.send { s to emptyList() } }
            controller.messages.send(msgEffect(renderStateEffect(this@MainFragment, renders)))
        }
        view.refresh.setOnRefreshListener { controller.messages.offer(msgRefresh()) }
        view.search.setOnClickListener { controller.messages.offer(msgSearch()) }
        view.query.addTextChangedListener(queryWatcher)
        view.recycler.adapter = adapter
        view.recycler.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        view.recycler.addOnScrollListener(loadMoreListener { controller.messages.offer(msgLoadMore()) })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        supervisor.cancelChildren()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }
}

private fun loadMoreListener(threshold: Int = 3, loadMore: () -> Unit) = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        (recyclerView.layoutManager as? LinearLayoutManager)?.let { lm ->
            if (lm.findLastVisibleItemPosition() > (recyclerView.adapter?.itemCount ?: threshold) - threshold) {
                loadMore()
            }
        }
    }
}

private fun textWatcher(textChanged: (String) -> Unit) = object : TextWatcher {
    override fun afterTextChanged(s: Editable) {
        textChanged(s.toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

}

private fun msgLoadMore(): MainMsg = msgEffects(
    { s -> s.copy(loading = true) },
    { s -> listOf(effectLoadMore()).takeIf { !s.loading }.orEmpty() }
)

private fun msgRefresh(): MainMsg = msgEffects(
    { it.copy(loading = true) },
    { listOf(effectSearch()) })

private fun msgQuery(query: String): MainMsg = msgState { it.copy(query = query) }

private fun msgSearch(): MainMsg = msgEffects(
    { s -> s.copy(loading = true, resultsQuery = s.query) },
    { listOf(effectSearch()) }
)

private fun msgFound(items: List<GifObject>): MainMsg = msgState { s -> s.copy(loading = false, results = items) }

private fun msgLoaded(items: List<GifObject>): MainMsg = msgState { s ->
    s.copy(loading = false, results = s.results + items)
}

private fun msgFail(e: Exception): MainMsg = msgEffects(
    { it.copy(loading = false, results = emptyList()) },
    { listOf(effectFail(e)) }
)

private fun effectSearch(): MainEffect = { ctx, s ->
    val msg = when (val r = ctx.repository.search(s.resultsQuery, 0, 20)) {
        is Right -> msgFound(r.v)
        is Left -> msgFail(r.v)
    }
    messages.send(msg)
}

private fun effectFail(e: Exception): MainEffect = { ctx, _ ->
    ctx.failToast(e)
}

private fun effectLoadMore(): MainEffect = { c, s ->
    val msg = when (val r = c.repository.search(s.resultsQuery, s.results.count(), 20)) {
        is Right -> msgLoaded(r.v)
        is Left -> msgFail(r.v)
    }
    messages.send(msg)
}

private fun renderItems() = renderT<MainView, MainState, List<GifObject>>(
    { it.results },
    { f, s ->
        f.run {
            adapter.items = s.map { MainElement.Item(it) }
            adapter.notifyDataSetChanged()
        }
    }
)

private fun renderRefreshing() = renderT<MainView, MainState, Boolean>(
    { it.loading && it.results.isEmpty() },
    { f, s -> f.refresh.isRefreshing = s }
)

private fun renderLoading() = renderT<MainView, MainState, Boolean>(
    { it.loading && it.results.isNotEmpty() },
    { f, s ->
        when (s) {
            true -> {
                f.adapter.items = f.adapter.items + MainElement.Loader
                f.adapter.notifyItemInserted(f.adapter.itemCount - 1)
            }
            false -> {
                f.adapter.items = f.adapter.items.filter { it !is MainElement.Loader }
                f.adapter.notifyDataSetChanged()
            }
        }
    }
)

private fun renderQuery() = renderT<MainView, MainState, String>(
    { it.query },
    { f, s ->
        when (f.query.text.toString()) {
            s -> Unit
            else -> f.query.apply {
                removeTextChangedListener(f.queryWatcher)
                setText(s)
                setSelection(s.length)
                addTextChangedListener(f.queryWatcher)
            }
        }
    }
)