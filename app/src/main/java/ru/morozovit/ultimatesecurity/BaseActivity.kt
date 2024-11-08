package ru.morozovit.ultimatesecurity

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.BaseActivity.InteractionDetector.execute
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import java.lang.Thread.sleep
import java.util.concurrent.Executor

abstract class BaseActivity(protected val authEnabled: Boolean = true): AppCompatActivity() {
    private var paused = false
    private var interacted = false
    private var noPause = false

    @AnimRes
    private var transitionEnter = 0
    @AnimRes
    private var transitionExit = 0

    private var transitionCalled = false

    private object InteractionDetector: Executor {
        private var executing = false
        private var thread: Thread? = null

        override fun execute(command: Runnable) {
            if (!executing) {
                executing = true
                thread = async {
                    command.run()
                    executing = false
                }
            }
        }
    }

    private fun interactionDetector() {
        if (authEnabled && globalPassword != "") {
            execute {
                try {
                    var timer = 0
                    while (true) {
                        var br = false
                        for (i in 0..1000) {
                            sleep(1)
                            if (paused) {
                                br = true
                                paused = false
                                Log.d("Auth", "Paused. [${this::class.simpleName}]")
                                break
                            }
                        }
                        if (br) break
                        if (interacted) {
                            interacted = false
                            timer = 0
                            Log.i(
                                "Auth",
                                "Interaction detected, resetting. [${this::class.simpleName}]"
                            )
                        } else {
                            timer++
                            Log.i(
                                "Auth",
                                "No interaction, timer = $timer. [${this::class.simpleName}] "
                            )
                        }

                        if (timer >= 60) {
                            authenticated = false
                            auth()
                            break
                        }
                    }
                } catch (_: InterruptedException) {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preSplashScreen()
        super.onCreate(savedInstanceState)
        if (authEnabled && globalPassword != "") {
            auth()
            interactionDetector()
        }
    }

    protected fun auth(): Boolean {
        if (authEnabled && globalPassword != "" && !authenticated) {
            paused = true
            startActivity(Intent(this, AuthActivity::class.java))
        }
        return authEnabled
    }

    override fun onResume() {
        super.onResume()
        paused = false
        interactionDetector()
    }

    override fun onPause() {
        super.onPause()
        if (!noPause) {
            paused = true
        } else {
            noPause = false
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        interacted = true
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                enterAnim,
                exitAnim,
                0
            )
        } else {
            super.overridePendingTransition(
                enterAnim,
                exitAnim
            )
        }
        transitionEnter = enterAnim
        transitionExit = exitAnim
        transitionCalled = true
    }

    override fun finishAfterTransition() = finishAfterTransition(transitionExit, transitionEnter)

    open fun finishAfterTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
        if (transitionCalled) {
            transitionCalled = false
            finish()
            @Suppress("DEPRECATION")
            super.overridePendingTransition(enterAnim, exitAnim)
        } else {
            super.finishAfterTransition()
        }
    }

    open fun finishAfterTransitionReverse() = finishAfterTransition(transitionEnter, transitionExit)

    open fun overridePendingTransition() = overridePendingTransition(0, 0)

    fun preSplashScreen() {
        try {
            val splashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                // Create your custom animation.
                splashScreenView.view.animate()
                    .scaleX(3f)
                    .scaleY(3f)
                    .alpha(0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            splashScreenView.remove()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            splashScreenView.remove()
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        } catch (_: Exception) {}
    }
}