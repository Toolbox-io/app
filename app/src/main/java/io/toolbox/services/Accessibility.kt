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
import io.toolbox.mainActivity
import io.toolbox.ui.protection.applocker.FakeCrashActivity
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
                        val apps = Settings.Applocker.apps
                        if (apps.contains(newPackageName)) {
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