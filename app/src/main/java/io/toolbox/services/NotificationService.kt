package io.toolbox.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.toolbox.Settings
import io.toolbox.mainActivity
import io.toolbox.ui.tools.notificationhistory.NotificationData
import io.toolbox.ui.tools.notificationhistory.NotificationDatabase

class NotificationService: NotificationListenerService() {
    companion object {
        var running = false
        var returnBack = false
        var lastNotification by mutableStateOf<NotificationData?>(null)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        running = true
        if (Accessibility.returnBack) mainActivity()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        running = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        Log.d("NotificationService", "Received notification: $sbn")
        if (sbn.packageName in Settings.NotificationHistory.apps) {
            with(sbn.notification) {
                val data = NotificationData(
                    title = extras.getString(Notification.EXTRA_TITLE) ?: return,
                    message = extras.getString(Notification.EXTRA_TEXT) ?: return,
                    sourcePackageName = sbn.packageName,
                    icon = smallIcon.loadDrawable(this@NotificationService)
                )
                NotificationDatabase += data
                lastNotification = data
            }
        }
    }
}