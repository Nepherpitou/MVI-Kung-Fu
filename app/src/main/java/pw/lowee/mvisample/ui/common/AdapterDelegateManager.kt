package pw.lowee.mvisample.ui.common

import android.view.View
import android.view.ViewGroup

class AdapterDelegateManager<T> {

    val delegates: MutableList<AdapterDelegate<T>> = mutableListOf()

    fun getItemViewType(items: List<T>, position: Int): Int {
        return delegates.indexOfFirst { it.isForViewType(items, position) }
    }

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        return when (viewType) {
            in 0..delegates.size -> delegates[viewType].createViewHolder(parent)
            else -> BaseHolder(View(parent.context))
        }
    }

    fun bindViewHolder(items: List<T>, position: Int, holder: BaseHolder) {
        val viewType = getItemViewType(items, position)
        if (viewType >= 0 && viewType < delegates.size) {
            delegates[viewType].bindViewHolder(items, position, holder)
        }
    }

    fun onViewDetachedFromWindow(holder: BaseHolder) {
        delegates.getOrNull(holder.itemViewType)?.onViewDetachedFromWindow(holder)
    }

    fun onViewAttachedToWindow(holder: BaseHolder) {
        delegates.getOrNull(holder.itemViewType)?.onViewAttachedToWindow(holder)
    }
}