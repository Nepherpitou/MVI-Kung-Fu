package pw.lowee.mvisample.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import pw.lowee.mvikungfu.ElmEffect
import pw.lowee.mvikungfu.ElmMsg
import pw.lowee.mvikungfu.defaultController
import pw.lowee.mvikungfu.msgEffects
import pw.lowee.mvisample.R
import pw.lowee.mvisample.ui.Screens
import ru.terrakok.cicerone.Router

data class SplashState(
    val ready: Boolean = false
)

class SplashCtx : KoinComponent {
    val router: Router by inject()
    suspend fun route(block: Router.() -> Unit) {
        withContext(Dispatchers.Main) { block(router) }
    }
}

typealias SplashMsg = ElmMsg<SplashCtx, SplashState>
typealias SplashEffect = ElmEffect<SplashCtx, SplashState>

fun msgInit(): SplashMsg = msgEffects(
    { it },
    { listOf(effectInit()) }
)

fun effectInit(): SplashEffect = { ctx, _ ->
    delay(1000)
    ctx.route { newRootScreen(Screens.Main) }
}

class SplashFragment : Fragment() {

    private val effectContext = SplashCtx()
    private val controller = defaultController(SplashState()) { effectContext }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start(msgInit())
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }
}