package pw.lowee.mvisample.ui

import androidx.fragment.app.Fragment
import pw.lowee.mvisample.ui.main.MainFragment
import pw.lowee.mvisample.ui.splash.SplashFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen

sealed class Screens : SupportAppScreen() {

    object Splash : Screens() {
        override fun getFragment(): Fragment {
            return SplashFragment()
        }
    }

    object Main : Screens() {
        override fun getFragment(): Fragment {
            return MainFragment()
        }
    }

}