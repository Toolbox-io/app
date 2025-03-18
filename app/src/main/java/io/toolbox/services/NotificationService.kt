package io.toolbox.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.toolbox.ui.tools.notificationhistory.NotificationData
import io.toolbox.ui.tools.notificationhistory.NotificationDatabase

class NotificationService: NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        Log.d("NotificationService", "Received notification: $sbn")
        with (sbn.notification) {
            NotificationDatabase += NotificationData(
                title = extras.getString(Notification.EXTRA_TITLE) ?: return,
                message = extras.getString(Notification.EXTRA_TEXT) ?: return,
                sourcePackageName = sbn.packageName,
                icon = smallIcon.loadDrawable(this@NotificationService)
            )
        }
    }
}