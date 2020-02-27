package pw.lowee.mvisample.ui.main

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koin.core.KoinComponent
import org.koin.core.get
import pw.lowee.mvikungfu.ElmEffect
import pw.lowee.mvikungfu.ElmMessage
import pw.lowee.mvikungfu.ElmRender
import pw.lowee.mvisample.UserInput
import pw.lowee.mvisample.models.GifObject

@Parcelize
data class MainState(
    val query: UserInput<String> = "" to false,
    val results: List<GifObject> = emptyList(),
    val resultsQuery: String = "",
    val loading: Boolean = false
) : Parcelable

class MainContext : KoinComponent {
    val repository: MainRepository = MainRepository(get())
    var errorToast: (Exception) -> Unit = {}
}

typealias MainMsg = ElmMessage<MainContext, MainState>
typealias MainEffect = ElmEffect<MainContext, MainState>
typealias MainRender = ElmRender<MainState>
