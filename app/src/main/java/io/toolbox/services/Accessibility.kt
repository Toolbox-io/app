package io.toolbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import io.toolbox.Settings
import io.toolbox.Settings.Applocker.showMode
import io.toolbox.mainActivity
import io.toolbox.ui.AuthActivity
import io.toolbox.ui.protection.applocker.FakeCrashActivity
import io.toolbox.ui.protection.applocker.PasswordInputActivity
import ru.morozovit.android.homeScreen
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
                        prevApp = newPackageName
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
                                    startActivity(
                                        Intent(this, AuthActivity::class.java).apply {
                                            putExtra("setStarted", true)
                                            putExtra("applocker", true)
                                            putExtra("noAnim", true)
                                            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_MULTIPLE_TASK
                                        }
                                    )
                                }
                            }
                        }
                    }
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