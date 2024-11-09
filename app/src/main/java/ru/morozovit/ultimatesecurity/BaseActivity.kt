package ru.morozovit.ultimatesecurity

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.morozovit.android.NoParallelExecutor
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import java.lang.Thread.sleep

@Suppress("unused")
abstract class BaseActivity(
    @Suppress("MemberVisibilityCanBePrivate")
    protected var authEnabled: Boolean = true,
    private val savedInstanceStateEnabled: Boolean = false
): AppCompatActivity() {
    private var paused = false
    private var interacted = false
    private var noPause = false

    protected var savedInstanceState: Bundle? = null

    @AnimRes
    private var transitionEnter = 0
    @AnimRes
    private var transitionExit = 0

    private var transitionCalled = false

    private val interactionDetectorExecutor = NoParallelExecutor()

    private fun interactionDetector() {
        if (authEnabled && globalPassword != "") {
            interactionDetectorExecutor.execute {
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
        if (savedInstanceStateEnabled) {
            super.onCreate(savedInstanceState)
        } else {
            savedInstanceState?.clear()
            super.onCreate(null)
        }
        if (authEnabled && globalPassword != "") {
            auth()
            interactionDetector()
        }
        this.savedInstanceState = savedInstanceState
    }

    protected fun auth() {
        if (authEnabled && globalPassword != "" && !authenticated && globalPasswordEnabled) {
            paused = true
            startActivity(Intent(this, AuthActivity::class.java))
        }
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

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }

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

    private fun preSplashScreen() {
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

    @Suppress("DEPRECATION")
    open fun vibrate(millis: Long) {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    millis, VibrationEffect
                        .DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(millis)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!savedInstanceStateEnabled) outState.clear()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (!savedInstanceStateEnabled) savedInstanceState.clear()
        super.onRestoreInstanceState(savedInstanceState)
    }
}