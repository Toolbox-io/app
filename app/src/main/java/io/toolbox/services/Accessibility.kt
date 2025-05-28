package io.toolbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.toolbox.App.Companion.context
import io.toolbox.Settings
import io.toolbox.Settings.Applocker.showMode
import io.toolbox.mainActivity
import io.toolbox.ui.protection.applocker.FakeCrashActivity
import io.toolbox.ui.protection.applocker.PasswordInputActivity
import ru.morozovit.android.ComposeLifecycleOwner
import ru.morozovit.android.ComposeView
import ru.morozovit.android.homeScreen
import ru.morozovit.android.runOrLog
import java.lang.Thread.sleep


class Accessibility: AccessibilityService() {
    var lock = false
    private var prevApp: String? = null

    @SuppressLint("StaticFieldLeak")
    companion object {
        var instance: Accessibility? = null
        inline val running get() = instance != null
        var returnBack = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (returnBack) mainActivity()
        if (Settings.UnlockProtection.fgServiceEnabled)
            AccessibilityKeeperService.start(this)
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            // App opened - App Locker
            TYPE_WINDOW_STATE_CHANGED -> {
                if (Settings.Applocker.enabled) {
                    val newPackageName = event.packageName.toString() // App package name
                    if (newPackageName != applicationContext.packageName && newPackageName != prevApp && !lock) {
                        val apps = Settings.Applocker.apps
                        if (apps.contains(newPackageName)) {
                            when (showMode) {
                                Settings.Applocker.ShowMode.FAKE_CRASH -> {
                                    // App lock mechanism
                                    homeScreen()

                                    sleep(500)

                                    // Fake crash
                                    val intent = Intent(this, FakeCrashActivity::class.java)

                                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(FLAG_ACTIVITY_NEW_DOCUMENT)
                                    intent.addFlags(FLAG_ACTIVITY_MULTIPLE_TASK)
                                    val args = Bundle()

                                    args.putString("appPackage", newPackageName)
                                    intent.putExtras(args)
                                    startActivity(intent)
                                }
                                Settings.Applocker.ShowMode.PASSWORD_POPUP -> PasswordInputActivity.start(this, newPackageName)
                                Settings.Applocker.ShowMode.FULLSCREEN_POPUP -> {
                                    // TODO implement

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        // set the layout parameters of the window
                                        val params = WindowManager.LayoutParams( // Shrink the window to wrap the content rather
                                            // than filling the screen
                                            WindowManager.LayoutParams.WRAP_CONTENT,
                                            WindowManager.LayoutParams.WRAP_CONTENT,  // Display it on top of other application windows
                                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Don't let it grab the input focus
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  // Make the underlying application window visible
                                            // through any transparent parts
                                            PixelFormat.TRANSLUCENT
                                        )

                                        val view = ComposeView {
                                            Text("gherdiguvofwfehgoesg")
                                        }.apply {
                                            // Trick The ComposeView into thinking we are tracking lifecycle
                                            val lifecycleOwner = ComposeLifecycleOwner()
                                            lifecycleOwner.performRestore(null)
                                            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                                            setViewTreeLifecycleOwner(lifecycleOwner)
                                            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                                        }

                                        // Define the position of the
                                        // window within the screen
                                        params.gravity = Gravity.CENTER
                                        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
                                        runOrLog("Applocker") {
                                            // check if the view is already
                                            // inflated or present in the window
                                            if (view.windowToken == null && view.parent == null) {
                                                windowManager.addView(view, params);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    prevApp = newPackageName
                }
            }
        }
    }

    fun disable() {
        disableSelf()
        instance = null
    }

    override fun onInterrupt() {
        instance = null
        if (Settings.UnlockProtection.fgServiceEnabled)
            AccessibilityKeeperService.instance?.stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        onInterrupt()
    }
}