package io.toolbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import io.toolbox.Settings
import io.toolbox.ui.MainActivity
import io.toolbox.ui.protection.applocker.FakeCrashActivity
import ru.morozovit.android.homeScreen
import java.lang.Thread.sleep

class Accessibility: AccessibilityService() {
    private var interrupted = false
    var lock = false
    private var prevApp: String? = null

    @SuppressLint("StaticFieldLeak")
    companion object {
        var instance: Accessibility? = null
        var waitingForAccessibility = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (waitingForAccessibility) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            startActivity(intent)
        }
        if (Settings.UnlockProtection.fgServiceEnabled)
            AccessibilityKeeperService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // App opened listener
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
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
                    // always launch in new window
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

    fun disable() {
        disableSelf()
        instance = null
    }

    override fun onInterrupt() {
        interrupted = true
        instance = null
        if (Settings.UnlockProtection.fgServiceEnabled)
            AccessibilityKeeperService.instance?.stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        onInterrupt()
    }
}