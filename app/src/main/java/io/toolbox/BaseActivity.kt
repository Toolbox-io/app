@file:Suppress("DEPRECATION")

package io.toolbox

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.AnimRes
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.toolbox.App.Companion.authenticated
import io.toolbox.ui.Auth.Companion.started
import io.toolbox.ui.AuthActivity
import io.toolbox.ui.MainActivity
import ru.morozovit.android.NoParallelExecutor
import ru.morozovit.android.resolveAttr
import ru.morozovit.android.ui.ThemeSetting
import ru.morozovit.utils.ExceptionParser.Companion.eToString
import java.lang.Thread.sleep

@Suppress("unused")
abstract class BaseActivity(
    protected var authEnabled: Boolean = true,
    private val savedInstanceStateEnabled: Boolean = true,
    private val configTheme: Boolean = true
): AppCompatActivity() {
    companion object {
        private var interacted = false
        private val interactionDetectorExecutor = NoParallelExecutor()
        private var currentActivity: BaseActivity? = null
        protected var authScheduled2 = false

        var splashScreenDisplayed = false

        private var splashScreenCounter = 0
        var isSplashScreenVisible = true
            set(value) {
                if (authScheduled2 && !value) {
                    authScheduled2 = false
                    startAuth {
                        putExtra("setAuthscheduled", true)
                    }
                }
                field = value
            }

        private const val TAG = "BaseActivity"

        private inline fun startAuth(apply: Intent.() -> Unit = {}) {
            currentActivity!!.startActivity(
                Intent(currentActivity!!, AuthActivity::class.java)
                    .apply {
                        putExtra("setStarted", true)
                        putExtra("noAnim", true)
                        apply()
                    }
            )
        }

        fun scheduleAuth(noAnim: Boolean = true, setPendingAuth: Boolean = false) {
            with(currentActivity!!) {
                Log.d("Auth", "Scheduling auth")
                if (setPendingAuth) currentActivity!!.pendingAuth = true
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

    private var finishAfterTransitionCalled = false
    protected var savedInstanceState: Bundle? = null
    protected var pendingAuth = false

    @AnimRes
    private var transitionEnter = 0
    @AnimRes
    private var transitionExit = 0
    private var transitionCalled = false

    private var isActive = false

    private var onPause = mutableListOf<() -> Unit>()
    private var onResume = mutableListOf<() -> Unit>()
    private var onDestroy = mutableListOf<() -> Unit>()

    private var permissionCallbacks =
        mutableMapOf<Array<out String>, (Map<String, Boolean>) -> Unit>()

    protected fun interactionDetector() {
        if (Settings.Keys.App.isSet) {
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

    fun addResumeCallback(callback: () -> Unit) {
        onResume.add(callback)
    }

    fun removeResumeCallback(callback: () -> Unit) {
        onResume.remove(callback)
    }

    fun addPauseCallback(callback: () -> Unit) {
        onPause.add(callback)
    }

    fun removePauseCallback(callback: () -> Unit) {
        onPause.remove(callback)
    }

    fun addDestroyCallback(callback: () -> Unit) {
        onDestroy.add(callback)
    }

    fun removeDestroyCallback(callback: () -> Unit) {
        onDestroy.remove(callback)
    }

    fun requestPermissions(vararg permissions: String, callback: (Map<String, Boolean>) -> Unit) {
        requestPermissions(permissions, 0)
        permissionCallbacks[permissions] = callback
    }

    fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
        requestPermissions(arrayOf(permission), 1)
        permissionCallbacks[arrayOf(permission)] = {
            callback.invoke(it[permission]!!)
        }
    }

    fun requestPermissions(vararg permissions: String) {
        requestPermissions(permissions, 2)
    }

    fun requestPermission(permission: String) {
        requestPermissions(arrayOf(permission), 3)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 || requestCode == 1) {
            for ((perms, callback) in permissionCallbacks) {
                if (perms.contentEquals(permissions)) {
                    val map = mutableMapOf<String, Boolean>()
                    for (i in permissions.indices) {
                        map[permissions[i]] = grantResults[i] == PackageManager.PERMISSION_GRANTED
                    }
                    callback.invoke(map)
                    break
                }
            }
            permissionCallbacks.clear()
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        currentActivity = this
        preSplashScreen(!(!(authEnabled && Settings.Keys.App.isSet) || this !is MainActivity))
        if (savedInstanceStateEnabled) {
            super.onCreate(savedInstanceState)
        } else {
            savedInstanceState?.clear()
            super.onCreate(null)
        }
        if (authEnabled && Settings.Keys.App.isSet) {
            auth(noAnim = false, setPendingAuth = true)
        }
        this.savedInstanceState = savedInstanceState
        if (configTheme) configureTheme()
    }

    @Suppress("SameParameterValue")
    protected fun auth(noAnim: Boolean = true, setPendingAuth: Boolean = false) {
        if (currentActivity?.authEnabled == true && Settings.Keys.App.isSet && !authenticated) {
            authenticated = false
            scheduleAuth(noAnim, setPendingAuth)
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        currentActivity = this
        interactionDetector()
        isActive = true
        onResume.forEach { it() }
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        interactionDetector()
        isActive = false
        onPause.forEach { it() }
    }

    @CallSuper
    override fun onUserInteraction() {
        super.onUserInteraction()
        interacted = true
    }

    @SuppressLint("InlinedApi")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun overridePendingTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
        overridePendingTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun overridePendingTransition(overrideType: Int, @AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                overrideType,
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

    @SuppressLint("InlinedApi")
    open fun finishAfterTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
        finishAfterTransitionCalled = true
        if (transitionCalled) {
            transitionCalled = false
            finish()
            overridePendingTransition(OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            super.finishAfterTransition()
        }
    }

    open fun finishAfterSceneTransition() = super.finishAfterTransition()
    open fun finishAfterTransitionReverse() = finishAfterTransition(transitionEnter, transitionExit)
    open fun overridePendingTransition() = overridePendingTransition(0, 0)

    private fun preSplashScreen(setTheme: Boolean = false) {
        if (!splashScreenDisplayed && !setTheme) {
            Log.d(TAG, "Trying to initialize")
            try {
                val splashScreen = installSplashScreen()

                Log.d(TAG, "Splash screen not intitialized.")
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    splashScreenCounter++
                    splashScreenDisplayed = true
                    splashScreenView.view.animate()
                        .scaleX(3f)
                        .scaleY(3f)
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction {
                            splashScreenView.remove()
                            isSplashScreenVisible = false
                        }
                        .start()

                    if (configTheme) configureTheme()
                }
                splashScreen.setKeepOnScreenCondition {
                    if (authEnabled && Settings.Keys.App.isSet) {
                        started
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to init splash screen.\n${eToString(e)}")
            }
        } else {
            Log.d(
                TAG,
                "Splash screen is already initialized, setting theme to postSplashScreenTheme"
            )
            resolveAttr(androidx.core.splashscreen.R.attr.postSplashScreenTheme)?.let {
                setTheme(it)
                Log.d(TAG, "Theme set successfully")
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

    private val views: MutableList<Pair<View, Int>> = mutableListOf()

    @CallSuper
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (authScheduled2 && hasFocus) {
            Log.d("Auth", "Running scheduled auth")
            authScheduled2 = false
            startAuth()
        }
        Log.d(TAG, "Focus changed, hasFocus = $hasFocus")
        if (Settings.dontShowInRecents) {
            isSecure = !hasFocus
        }
    }

    open var isSecure
        get() = window.attributes.flags and FLAG_SECURE != 0
        set(value) {
            if (value) window.addFlags(FLAG_SECURE)
            else window.clearFlags(FLAG_SECURE)
        }

    protected open fun startEnterAnimation(root: View) {
        if (!intent.getBooleanExtra("noAnim", false) && !splashScreenDisplayed) {
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

    val isDarkTheme get() =
        resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    fun configureTheme() {
        if (Settings.materialYouEnabled) {
            when (Settings.appTheme) {
                ThemeSetting.AsSystem -> setTheme(R.style.Theme_Toolbox_io_NoActionBar)
                ThemeSetting.Light -> setTheme(R.style.Theme_Toolbox_io_Light_NoActionBar)
                ThemeSetting.Dark -> setTheme(R.style.Theme_Toolbox_io_Night_NoActionBar)
            }
        } else {
            when (Settings.appTheme) {
                ThemeSetting.AsSystem -> setTheme(R.style.Theme_Toolbox_io_NoActionBar_NoDynamicColor)
                ThemeSetting.Light -> setTheme(R.style.Theme_Toolbox_io_Light_NoActionBar_NoDynamicColor)
                ThemeSetting.Dark -> setTheme(R.style.Theme_Toolbox_io_Night_NoActionBar_NoDynamicColor)
            }
        }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        onDestroy.forEach { it() }
    }
}