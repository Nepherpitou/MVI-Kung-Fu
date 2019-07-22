package pw.lowee.mvisample.ui.common

import android.view.ViewGroup

interface AdapterDelegate<T> {
    fun isForViewType(items: List<T>, position: Int): Boolean
    fun createViewHolder(parent: ViewGroup): BaseHolder
    fun bindViewHolder(items: List<T>, position: Int, holder: BaseHolder)

    fun onViewAttachedToWindow(holder: BaseHolder) {}
    fun onViewDetachedFromWindow(holder: BaseHolder) {}
}