@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.services.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import io.toolbox.App.Companion.SLEEP_TILE_CHANNEL_ID
import io.toolbox.App.Companion.SLEEP_TILE_NOTIFICATION_ID
import io.toolbox.R
import ru.morozovit.android.configure

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
        const val ACTION_ENABLE_TILE = "io.toolbox.services.tiles.SleepTileKeeperService.ENABLE_TILE"

        inline fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SleepTileKeeperService::class.java))
        }

        inline fun stop() {
            instance?.stopSelf()
        }
    }

    private val enableTileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ENABLE_TILE) {
                // Try to enable the tile if possible
                SleepTile.instance?.let { tile ->
                    tile.qsTile.configure {
                        state = android.service.quicksettings.Tile.STATE_ACTIVE
                    }
                    tile.releaseWakelock()
                    tile.stopSelf()
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(enableTileReceiver, android.content.IntentFilter(ACTION_ENABLE_TILE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(enableTileReceiver, android.content.IntentFilter(ACTION_ENABLE_TILE))
        }
    }

    override fun onDestroy() {
        unregisterReceiver(enableTileReceiver)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        instance = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val enableIntent = Intent(ACTION_ENABLE_TILE).apply {
            setPackage(packageName)
        }
        val enablePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            enableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
                    enablePendingIntent
                )
                .setPriority(PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()
        )
        instance = this
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?) = null
}