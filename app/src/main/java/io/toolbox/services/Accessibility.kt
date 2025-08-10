package io.toolbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import io.toolbox.Settings
import io.toolbox.Settings.Applocker.showMode
import io.toolbox.mainActivity
import io.toolbox.ui.protection.applocker.ApplockerAuthOverlay
import io.toolbox.ui.protection.applocker.FakeCrashActivity
import io.toolbox.ui.protection.applocker.PasswordInputActivity
import ru.morozovit.android.utils.homeScreen
import java.lang.Thread.sleep


class Accessibility: AccessibilityService() {
    var lock = false
    private var prevApp: String? = null
    private val receiver = ScreenOffBroadcastReceiver()

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

        registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF)
        )
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            // App opened - App Locker
            TYPE_WINDOW_STATE_CHANGED -> {
                if (Settings.Applocker.enabled) {
                    val newPackageName = event.packageName.toString()
                    if (
                        newPackageName != applicationContext.packageName &&
                        newPackageName != prevApp &&
                        !lock
                    ) {
                        prevApp = newPackageName
                        if (newPackageName in Settings.Applocker.apps) {
                            when (showMode) {
                                Settings.Applocker.ShowMode.FAKE_CRASH -> {
                                    homeScreen()
                                    sleep(1000)
                                    startActivity(
                                        Intent(this, FakeCrashActivity::class.java).apply {
                                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                                            addFlags(FLAG_ACTIVITY_NEW_DOCUMENT)
                                            addFlags(FLAG_ACTIVITY_MULTIPLE_TASK)
                                            putExtra("appPackage", newPackageName)
                                        }
                                    )
                                }
                                Settings.Applocker.ShowMode.PASSWORD_POPUP -> PasswordInputActivity.start(this, newPackageName)
                                Settings.Applocker.ShowMode.FULLSCREEN_POPUP -> {
                                    if (!ApplockerAuthOverlay.shown)
                                        ApplockerAuthOverlay(this).show()
                                }
                            }
                        }
                    }

                    if (newPackageName == "com.android.systemui") {
                        ApplockerAuthOverlay.instance?.hide()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        instance = null
        if (Settings.UnlockProtection.fgServiceEnabled)
            AccessibilityKeeperService.instance?.stopSelf()
        unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        onInterrupt()
    }
}