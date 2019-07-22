package pw.lowee.mvisample.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import pw.lowee.mvikungfu.Effect
import pw.lowee.mvikungfu.ElmController
import pw.lowee.mvikungfu.Msg
import pw.lowee.mvikungfu.msgEffects
import pw.lowee.mvisample.R
import pw.lowee.mvisample.ui.Screens
import ru.terrakok.cicerone.Router

data class SplashState(
    val ready: Boolean = false
)

interface SplashCtx {
    suspend fun route(block: Router.() -> Unit)
}

private typealias SplashMsg = Msg<SplashCtx, SplashState>
private typealias SplashEffect = Effect<SplashCtx, SplashState>

fun SplashController(context: SplashCtx, state: SplashState) = object : ElmController<SplashCtx, SplashState>(state) {
    override fun effectContext(): SplashCtx = context
    override fun init(): SplashMsg = msgInit()
}

fun msgInit(): SplashMsg = msgEffects(
    { it },
    { listOf(effectInit()) }
)

fun effectInit(): SplashEffect = { ctx, state ->
    delay(1000)
    ctx.value.route { newRootScreen(Screens.Main) }
}

class SplashFragment : Fragment() {

    private val router: Router by inject()
    private val effectContext = object : SplashCtx {
        override suspend fun route(block: Router.() -> Unit) {
            withContext(Dispatchers.Main) { block(router) }
        }
    }
    private val controller = SplashController(effectContext, SplashState())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }
}