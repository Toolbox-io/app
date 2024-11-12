package ru.morozovit.ultimatesecurity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.AnimRes
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.morozovit.android.NoParallelExecutor
import ru.morozovit.android.homeScreen
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.AuthActivity.Companion.started
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import java.lang.Thread.sleep

@Suppress("unused")
abstract class BaseActivity(
    @Suppress("MemberVisibilityCanBePrivate")
    protected var authEnabled: Boolean = true,
    private val savedInstanceStateEnabled: Boolean = false,
    private var backButtonBehavior: BackButtonBehavior = BackButtonBehavior.FINISH,
): AppCompatActivity() {
    companion object {
        enum class BackButtonBehavior {
            DEFAULT,
            FINISH,
            HOME_SCREEN,
            NONE
        }

        private var interacted = false
        private val interactionDetectorExecutor = NoParallelExecutor()
        private var currentActivity: BaseActivity? = null
        private var authScheduled = false
        protected var authScheduled2 = false
        private var splashScreenDisplayed = false
        @JvmStatic
        protected var isSplashScreenVisible = true
            set(value) {
                if (authScheduled2 && !value) {
                    authScheduled2 = false
                    startAuth {
                        putExtra("setAuthscheduled", true)
                    }
                }
                field = value
            }

        private inline fun startAuth(apply: Intent.() -> Unit) {
            currentActivity!!.startActivity(
                Intent(currentActivity!!, AuthActivity::class.java)
                    .apply {
                        putExtra("setStarted", true)
                        putExtra("noAnim", true)
                        apply()
                    }
            )
        }

        private fun startAuth() = startAuth {}

        fun scheduleAuth(noAnim: Boolean = true) {
            with(currentActivity!!) {
                Log.d("Auth", "Scheduling auth")
                if (hasWindowFocus() || isSplashScreenVisible) {
                    Log.d("Auth", "Authenticating now.")
                    if (!noAnim) {
                        startAuth {
                            putExtra("noAnim", false)
                        }
                    } else {
                        startAuth()
                    }
                } else {
                    Log.d("Auth", "Scheduled auth.")
                    authScheduled2 = true
                }
            }
        }
    }

    protected var savedInstanceState: Bundle? = null

    @AnimRes
    private var transitionEnter = 0
    @AnimRes
    private var transitionExit = 0
    private var transitionCalled = false

    protected fun interactionDetector() {
        if (globalPassword != "" && globalPasswordEnabled) {
            interactionDetectorExecutor.execute {
                try {
                    var timer = 0
                    while (true) {
                        sleep(1000)
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
                            if (currentActivity?.authEnabled == true) {
                                authenticated = false
                                auth()
                            } else {
                                Log.d("Auth", "Auth activity, not starting again")
                            }
                            break
                        }
                    }
                } catch (_: InterruptedException) {}
            }
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        currentActivity = this
        preSplashScreen()
        if (savedInstanceStateEnabled) {
            super.onCreate(savedInstanceState)
        } else {
            savedInstanceState?.clear()
            super.onCreate(null)
        }
        if (authEnabled && globalPassword != "" && globalPasswordEnabled) {
            window.decorView.viewTreeObserver.addOnPreDrawListener(
                object: OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (started) {
                            window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                            return true
                        }
                        return false
                    }
                }
            )
            auth(false)
        }
        this.savedInstanceState = savedInstanceState
    }

    protected fun auth(noAnim: Boolean = true) {
        if (currentActivity?.authEnabled == true && globalPassword != "" && !authenticated &&
            globalPasswordEnabled) {
            authenticated = false
            scheduleAuth(noAnim)
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        currentActivity = this
        interactionDetector()
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        interactionDetector()
    }

    @CallSuper
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

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() {
        when (backButtonBehavior) {
            BackButtonBehavior.DEFAULT -> super.onBackPressed()
            BackButtonBehavior.FINISH -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            BackButtonBehavior.HOME_SCREEN -> homeScreen()
            BackButtonBehavior.NONE -> {}
        }
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
        if (!splashScreenDisplayed) {
            try {
                val splashScreen = installSplashScreen()
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    isSplashScreenVisible = false
                    splashScreenDisplayed = true
                    splashScreenView.view.animate()
                        .scaleX(3f)
                        .scaleY(3f)
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(splashScreenView::remove)
                }
            } catch (_: Exception) {
            }
        } else {
            val typedValue = TypedValue()
            if (theme.resolveAttribute(androidx.core.splashscreen.R.attr.postSplashScreenTheme,
                typedValue, true)) {
                val themeId = typedValue.resourceId
                if (themeId != 0) {
                    setTheme(themeId)
                }
            }
        }
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

    @CallSuper
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (authScheduled && hasFocus) {
            Log.d("Auth", "Running scheduled auth")
            authScheduled = false
            startAuth()
        }
        Log.d("BaseActivity", "Focus changed, hasFocus = $hasFocus")
    }

    protected open fun startEnterAnimation(root: View) {
        if (!intent.getBooleanExtra("noAnim", false) && Build.VERSION.SDK_INT >= 31) {
            root.viewTreeObserver.addOnPreDrawListener(object: OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    root.apply {
                        scaleY = 1.4f
                        scaleX = 1.4f
                        pivotX = width / 2f
                        pivotY = height / 2f

                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .duration = 250
                    }
                    root.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }
    }
}