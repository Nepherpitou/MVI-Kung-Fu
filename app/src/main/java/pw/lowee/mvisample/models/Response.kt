package pw.lowee.mvisample.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GifObject(
    val id: String,
    val url: String,
    val images: Images
): Parcelable

@Parcelize
data class Images(
    @SerializedName("fixed_height") val fixedHeight: FixedHeightImage
): Parcelable

@Parcelize
data class FixedHeightImage(
    val url: String,
    val width: Int,
    val height: Int
): Parcelable

data class GiphyResponse<T>(
    val data: T
)