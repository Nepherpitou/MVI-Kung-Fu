package pw.lowee.mvisample.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import pw.lowee.mvikungfu.*
import pw.lowee.mvisample.R
import pw.lowee.mvisample.ui.common.DelegateAdapter
import pw.lowee.mvisample.ui.common.ImageLoader


class MainFragment : Fragment() {

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisor)

    private var saveState: (Bundle) -> Unit = {}

    private val elmContext = MainContext()
    private val elmController = defaultController(MainState()) { elmContext }

    private val imageLoader: ImageLoader by inject()
    val queryWatcher = textWatcher { elmController.messages.offer(MainMessages.msgQuery(it to true)) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter: DelegateAdapter<MainElement> = DelegateAdapter(
            loaderDelegate<MainElement, MainElement.Loader>(),
            itemDelegate(imageLoader)
        )
        val renders: List<MainRender> = listOf(
            MainRenders.renderItems(adapter),
            MainRenders.renderLoading(adapter),
            MainRenders.renderRefreshing(view),
            MainRenders.renderQuery(view)
        )
        view.refresh.setOnRefreshListener { elmController.messages.offer(MainMessages.msgRefresh()) }
        view.search.setOnClickListener { elmController.messages.offer(MainMessages.msgSearch()) }
        view.query.addTextChangedListener(queryWatcher)
        view.recycler.adapter = adapter
        view.recycler.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        view.recycler.addOnScrollListener(loadMoreListener { elmController.messages.offer(MainMessages.msgLoadMore()) })
        scope.launch {
            while (!elmController.states.isEmpty) elmController.states.receive()
            launch {
                watchState(
                    elmController,
                    listOf(
                        debugWatcher { Log.d("Main", it) },
                        renderWatcher(renders),
                        { s ->
                            saveState = {
                                it.putParcelable("state", s)
                                Log.d("State", "Saved: $s")
                            }
                        }
                    )
                )
            }
            savedInstanceState
                ?.getParcelable<MainState>("state")
                ?.also { Log.d("State", "Restored: $it") }
                ?.let { s -> scope.sendMessage(elmController, msgState { s }) }
            scope.sendMessage(elmController, msgEffect(renderStateEffect(renders)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        supervisor.cancelChildren()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        elmController.start(msgEmpty())
    }

    override fun onDestroy() {
        super.onDestroy()
        elmController.stop()
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