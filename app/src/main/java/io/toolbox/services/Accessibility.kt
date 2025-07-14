package io.toolbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.util.Log
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
                Log.d("applocker", "Window state changed: $event")
                if (Settings.Applocker.enabled) {
                    Log.d("applocker", "applocker enabled")
                    val newPackageName = event.packageName.toString() // App package name
                    Log.d("applocker", "package: $newPackageName")
                    Log.d("applocker", "app context package: ${applicationContext.packageName}")
                    Log.d("applocker", "prevApp: $prevApp")
                    Log.d("applocker", "lock: $lock")
                    if (newPackageName != applicationContext.packageName && newPackageName != prevApp && !lock) {
                        prevApp = newPackageName
                        Log.i("applocker", "conditions met, locking")
                        val apps = Settings.Applocker.apps
                        Log.d("applocker", "apps list: $apps")
                        if (newPackageName in apps) {
                            Log.i("applocker", "app in list, locking...")
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
                                    Log.d("applocker", "enabling fullscreen auth")
                                    if (!ApplockerAuthOverlay.shown)
                                        ApplockerAuthOverlay(this).show()
                                }
                            }
                        } else {
                            Log.w("applocker", "app not in list")
                        }
                    } else {
                        Log.w("applocker", "conditions not met")
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
    }

    override fun onDestroy() {
        super.onDestroy()
        onInterrupt()
    }
}