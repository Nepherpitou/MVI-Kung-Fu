package pw.lowee.mvisample.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.koin.android.ext.android.inject
import pw.lowee.mvisample.R
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.android.support.SupportAppNavigator

class HostActivity : FragmentActivity() {

    private val holder: NavigatorHolder by inject()
    private val navigator by lazy { SupportAppNavigator(this, R.id.host) }
    private val router: Router by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
        if (savedInstanceState == null) router.newRootScreen(Screens.Splash)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        holder.setNavigator(navigator)
    }

    override fun onPause() {
        super.onPause()
        holder.removeNavigator()
    }
}