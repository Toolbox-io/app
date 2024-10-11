package ru.morozovit.ultimatesecurity

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import ru.morozovit.ultimatesecurity.applocker.FakeCrashActivity
import java.lang.Thread.sleep

class Service: AccessibilityService() {
    private var interrupted = false
    var lock = false

    private var prevApp: String? = null

    companion object {
        var instance: Service? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Settings.init(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // App opened listener
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val newPackageName = event.packageName.toString() // App package name
            if (newPackageName != applicationContext.packageName && newPackageName != prevApp && !lock) {
                val apps = Settings.Applocker.apps
                // TODO handle multiple packages
                if (/* newPackageName == "com.android.vending" */ apps.contains(newPackageName)) {
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
    }

    override fun onInterrupt() {
        interrupted = true
        instance = null
    }
}