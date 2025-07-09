@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.services.tiles

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import io.toolbox.App.Companion.SLEEP_TILE_CHANNEL_ID
import io.toolbox.App.Companion.SLEEP_TILE_NOTIFICATION_ID
import io.toolbox.R
import ru.morozovit.android.broadcastReceiver
import ru.morozovit.android.configure
import ru.morozovit.android.notificationButtonPendingIntent

/**
 * This **foreground service** will help [SleepTile] stay disabled and prevent
 * the device from going to sleep.
 *
 * The following notification is displayed:
 *
 * > Sleep is disabled
 * >
 * > The sleep tile is disabled, so the screen won't turn off until
 * > the tile is enabled again.
 * >
 * > **Enable**
 *
 * @since 2.0
 * @author denis0001-dev
 * @see SleepTile
 * @see Service
 */
class SleepTileKeeperService: Service() {
    companion object {
        var instance: SleepTileKeeperService? = null
        val ACTION_ENABLE_TILE = "${SleepTileKeeperService::class.qualifiedName}.ENABLE_TILE"

        inline fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SleepTileKeeperService::class.java))
        }

        inline fun stop() {
            instance?.stopSelf()
        }
    }

    private val enableTileReceiver = broadcastReceiver(ACTION_ENABLE_TILE) {
        SleepTile.instance?.let { tile ->
            tile.qsTile.configure {
                state = android.service.quicksettings.Tile.STATE_ACTIVE
            }
            tile.releaseWakelock()
            tile.stopSelf()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        enableTileReceiver.register(this)
    }

    override fun onDestroy() {
        enableTileReceiver.unregister(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        instance = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            SLEEP_TILE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, SLEEP_TILE_CHANNEL_ID)
                .setSmallIcon(R.drawable.sleep)
                .setContentTitle("Sleep is disabled")
                .setContentText(
                    """
                    The sleep tile is disabled, so the screen won't turn off until
                    the tile is enabled again.
                    """.trimIndent().replace("\n", " ")
                )
                .addAction(
                    R.drawable.sleep, // Use the same icon for the action
                    "Enable",
                    notificationButtonPendingIntent(ACTION_ENABLE_TILE)
                )
                .setPriority(PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()
        )
        instance = this
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent) = null
}