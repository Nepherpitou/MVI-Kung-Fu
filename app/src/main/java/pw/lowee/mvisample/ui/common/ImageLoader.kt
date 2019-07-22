package pw.lowee.mvisample.ui.common

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import pw.lowee.mvisample.GlideApp
import pw.lowee.mvisample.GlideRequest
import pw.lowee.mvisample.GlideRequests

interface ImageLoader {
    fun loadImage(view: ImageView, src: String)
}

class ImageLoaderImpl : ImageLoader {
    override fun loadImage(view: ImageView, src: String) {
        GlideApp.with(view).asGif().load(src).transform(CenterCrop()).into(view)
    }
}