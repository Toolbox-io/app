package io.toolbox.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.postDelayed
import io.toolbox.App.Companion.authenticated
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.ui.Auth.Companion.MODE_ENTER_OLD_PW
import io.toolbox.ui.Auth.Companion.MODE_SET
import io.toolbox.ui.Auth.Companion.started
import ru.morozovit.android.ComposeView
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.addOneTimeOnPreDrawListener

class AuthActivity: BaseActivity(false) {
    private lateinit var auth: Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Auth(this, intent)
        with (auth) {
            enableEdgeToEdge()
            isSecure = true
            launchingIntent = intent
            activityLauncher = activityResultLauncher

            if ((!key.isSet || authenticated) && !isSetOrConfirm) {
                finish()
                return
            }

            if (!isSetOrConfirm && !isSplashScreenVisible)
                overridePendingTransition(R.anim.alpha_up, R.anim.scale_up)

            if (isSplashScreenVisible) overridePendingTransition()

            val view = ComposeView {
                AuthScreen(mode)
            }
            if (isSetOrConfirm && mode == MODE_SET && key.isSet && !oldPwConfirmed) {
                intent.putExtra("mode", MODE_ENTER_OLD_PW)
                assert(mode == MODE_ENTER_OLD_PW)
            } else {
                startEnterAnimation(view)
            }
            setContentView(view)
            view.postDelayed(250) {
                if (setStarted) started = true
            }
            if (!isSetOrConfirm && Settings.allowBiometric) {
                window.decorView.addOneTimeOnPreDrawListener {
                    view.postDelayed(1000) {
                        requestAuth()
                    }
                    true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isSecure = false
    }
}