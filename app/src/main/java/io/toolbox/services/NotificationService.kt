package io.toolbox.services

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import io.toolbox.ui.tools.notificationhistory.ActionData
import io.toolbox.ui.tools.notificationhistory.ActionType
import io.toolbox.ui.tools.notificationhistory.NotificationData
import ru.morozovit.android.intent
import ru.morozovit.android.toSerializableIntent
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

class NotificationService : NotificationListenerService() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        Log.d("NotificationService", "Received notification: $sbn")
        if (sbn != null) {
            val notification = sbn.notification
            val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
            val message = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
            val sourcePackageName = sbn.packageName

            val icon = notification.smallIcon.loadDrawable(this)

            val clickIntent = sbn.notification.contentIntent?.intent?.toSerializableIntent()
            val actions = mutableListOf<ActionData>()
            if (sbn.notification.actions != null) {
                for (action in sbn.notification.actions) {
                    runCatching {
                        actions += ActionData(
                            label = action.title.toString(),
                            intent = action.actionIntent?.intent?.toSerializableIntent(),
                            type = when {
                                Build.VERSION.SDK_INT < 31 -> ActionType.ACTIVITY
                                action.actionIntent?.isActivity == true -> ActionType.ACTIVITY
                                action.actionIntent?.isBroadcast == true -> ActionType.BROADCAST
                                action.actionIntent?.isService == true -> ActionType.SERVICE
                                action.actionIntent?.isForegroundService == true -> ActionType.FOREGROUND_SERVICE
                                else -> ActionType.UNKNOWN
                            }
                        )
                    }
                }
            }

            val notificationData = NotificationData(
                title = title,
                message = message,
                sourcePackageName = sourcePackageName,
                bottomContent = null, // TODO: Add bottom content if needed
                icon = icon,
                onClickIntent = clickIntent,
                actions = actions
            )


            // Serialize the notificationData object
            val fileName = "${System.currentTimeMillis()}.notification"
            val notificationHistoryDir = File(filesDir, "notification_history")
            notificationHistoryDir.mkdir()
            val file = File(notificationHistoryDir, fileName)

            try {
                val outputStream = FileOutputStream(file)
                val objectOutputStream = ObjectOutputStream(outputStream)
                objectOutputStream.writeObject(notificationData)
                objectOutputStream.close()
                outputStream.close()
                println("Notification data saved to $fileName")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}