package pw.lowee.mvisample.ui.main

import android.graphics.Outline
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import pw.lowee.mvisample.R
import pw.lowee.mvisample.models.GifObject
import pw.lowee.mvisample.ui.common.AdapterDelegate
import pw.lowee.mvisample.ui.common.BaseHolder
import pw.lowee.mvisample.ui.common.ImageLoader

sealed class MainElement {
    object Loader : MainElement()
    data class Item(val v: GifObject) : MainElement()
}

inline fun <E, reified L : E> loaderDelegate(): AdapterDelegate<E> = object : AdapterDelegate<E> {

    override fun isForViewType(items: List<E>, position: Int): Boolean {
        return items[position] is L
    }

    override fun createViewHolder(parent: ViewGroup): BaseHolder = loaderHolder(parent)

    override fun bindViewHolder(items: List<E>, position: Int, holder: BaseHolder) {
    }

}

fun loaderHolder(parent: ViewGroup): BaseHolder = object : BaseHolder(parent, R.layout.item_loader) {}

inline fun <E, reified I : E> simpleDelegate(
    crossinline holder: (ViewGroup) -> BaseHolder,
    crossinline binder: (BaseHolder, I) -> Unit
) = object : AdapterDelegate<E> {
    override fun isForViewType(items: List<E>, position: Int): Boolean {
        return items[position] is I
    }

    override fun createViewHolder(parent: ViewGroup): BaseHolder = holder(parent)

    override fun bindViewHolder(items: List<E>, position: Int, holder: BaseHolder) =
        binder(holder, items[position] as I)

}

fun itemDelegate(imageLoader: ImageLoader) = simpleDelegate<MainElement, MainElement.Item>(
    { itemHolder(it) },
    { h, i -> bindItem(h, i, imageLoader) }
)

private fun itemHolder(parent: ViewGroup): BaseHolder = BaseHolder(parent, R.layout.item_gif).also {
    it.itemView.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(
                0,
                0,
                view.width,
                view.height,
                view.resources.getDimension(R.dimen.dp16)
            )
        }
    }
    it.itemView.clipToOutline = true
}

private fun bindItem(holder: BaseHolder, item: MainElement.Item, loader: ImageLoader) {
    loader.loadImage(holder.itemView as ImageView, item.v.images.fixedHeight.url)
    Log.d("ItemHolder", "Displaying gif ${item.v.images.fixedHeight.url}")
}