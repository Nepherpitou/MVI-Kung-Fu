package pw.lowee.mvisample.ui.common

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DelegateAdapter<T>(vararg delegate: AdapterDelegate<T>) : RecyclerView.Adapter<BaseHolder>() {

    val items: MutableList<T> = mutableListOf()
    private val manager = AdapterDelegateManager<T>()

    init {
        manager.delegates.addAll(delegate)
    }

    override fun onViewAttachedToWindow(holder: BaseHolder) {
        super.onViewAttachedToWindow(holder)
        manager.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: BaseHolder) {
        super.onViewDetachedFromWindow(holder)
        manager.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        manager.bindViewHolder(items, position, holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        return manager.createViewHolder(parent, viewType)
    }

    override fun getItemViewType(position: Int): Int = manager.getItemViewType(items, position)
}