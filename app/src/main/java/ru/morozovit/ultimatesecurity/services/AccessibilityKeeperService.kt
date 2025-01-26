package ru.morozovit.ultimatesecurity.services

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import ru.morozovit.ultimatesecurity.App.Companion.ACCESSIBILITY_CHANNEL_ID
import ru.morozovit.ultimatesecurity.App.Companion.ACCESSIBILITY_NOTIFICATION_ID
import ru.morozovit.ultimatesecurity.R

class AccessibilityKeeperService: Service() {
    companion object {
        var instance: AccessibilityKeeperService? = null
        private val callbacks = mutableListOf<AccessibilityKeeperService.() -> Unit>()

        fun start(context: Context, callback: AccessibilityKeeperService.() -> Unit) {
            ContextCompat.startForegroundService(context, Intent(context, AccessibilityKeeperService::class.java))
            callbacks.add(callback)
        }
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AccessibilityKeeperService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ACCESSIBILITY_NOTIFICATION_ID,
            NotificationCompat.Builder(this, ACCESSIBILITY_CHANNEL_ID)
                .setSmallIcon(R.drawable.primitive_icon)
                .setContentTitle("")
                .setPriority(PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()
        )
        instance = this
        callbacks.forEach { it(this) }
        callbacks.clear()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        instance = null
    }

    override fun onBind(intent: Intent?) = null
}