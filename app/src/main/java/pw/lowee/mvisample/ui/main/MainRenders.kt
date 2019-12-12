package pw.lowee.mvisample.ui.main

import android.view.View
import kotlinx.android.synthetic.main.fragment_main.view.*
import pw.lowee.mvikungfu.renderT
import pw.lowee.mvisample.nonUser
import pw.lowee.mvisample.ui.common.DelegateAdapter

object MainRenders {

    fun renderItems(adapter: DelegateAdapter<MainElement>): MainRender = renderT(
        { it.results },
        { items ->
            adapter.items = items.map { MainElement.Item(it) }
            adapter.notifyDataSetChanged()
        }
    )

    fun renderRefreshing(view: View): MainRender = renderT(
        { it.loading && it.results.isEmpty() },
        { s -> view.refresh.isRefreshing = s }
    )

    fun renderLoading(adapter: DelegateAdapter<MainElement>): MainRender = renderT(
        { it.loading && it.results.isNotEmpty() },
        { s ->
            if (s) {
                adapter.items = adapter.items + MainElement.Loader
                adapter.notifyItemInserted(adapter.itemCount - 1)
            } else {
                adapter.items = adapter.items.filter { it !is MainElement.Loader }
                adapter.notifyDataSetChanged()
            }
        }
    )

    fun renderQuery(view: View): MainRender = renderT(
        { it.query.nonUser() },
        { it?.let { query -> view.query.setText(query) } }
    )
}