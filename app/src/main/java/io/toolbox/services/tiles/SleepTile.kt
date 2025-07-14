package io.toolbox.services.tiles

import android.annotation.SuppressLint
import android.os.PowerManager
import android.os.PowerManager.SCREEN_DIM_WAKE_LOCK
import android.service.quicksettings.Tile
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.Tile.STATE_UNAVAILABLE
import android.service.quicksettings.TileService
import android.util.Log
import io.toolbox.App.Companion.context
import ru.morozovit.android.utils.configure
import ru.morozovit.android.utils.runOrLog

/**
 * This **Quick Settings** tile will keep the screen on if disabled.
 * If the tile is not enabled in the settings, it will be hidden if possible
 * and its state will be set to [STATE_UNAVAILABLE].
 *
 * @since 1.7
 * @author denis0001-dev
 * @see SleepTileKeeperService
 * @see TileService
 */
class SleepTile: TileService() {
    companion object {
        var isVisible = false
            private set

        var canUpdate = false
            private set

        @Suppress("DEPRECATION")
        val wakeLock by lazy {
            (context.getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(SCREEN_DIM_WAKE_LOCK, "toolbox.io:wakelock")!!
        }

        private var set = false

        var instance: SleepTile? = null
            private set

        private var scheduledConfig: (Tile.() -> Unit)? = null

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTileAdded() {
        super.onTileAdded()
        isVisible = true
        qsTile.configure {
            enabled = true
        }
        Log.d("SleepTile", "Releasing wake lock [onTileAdded]")
        releaseWakelock()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        isVisible = false
        qsTile.configure {
            enabled = true
        }
        Log.d("SleepTile", "Releasing wake lock [onTileRemoved]")
        releaseWakelock()
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!set) {
            qsTile.configure {
                enabled = true
            }
            Log.d("SleepTile", "Releasing wake lock [onStartListening]")
            releaseWakelock()
            set = true
        }
        canUpdate = true
        if (scheduledConfig != null) {
            Log.d("SleepTile", "Invoking scheduled config")
            scheduledConfig!!.invoke(qsTile)
            scheduledConfig = null
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        canUpdate = false
    }

    @SuppressLint("WakelockTimeout")
    fun acquireWakelock() {
        runOrLog("SleepTile") {
            if (!wakeLock.isHeld) wakeLock.acquire()
            SleepTileKeeperService.start(applicationContext)
        }
    }

    fun releaseWakelock() {
        runOrLog("SleepTile") {
            if (wakeLock.isHeld) wakeLock.release()
            SleepTileKeeperService.stop()
        }
    }

    var enabled
        inline get() = qsTile.state == STATE_ACTIVE || qsTile.state == STATE_INACTIVE
        inline set(value) {
            qsTile.configure {
                state = if (value) STATE_ACTIVE else STATE_INACTIVE
            }
        }

    override fun onClick() {
        qsTile.configure {
            state = if (state == STATE_ACTIVE) STATE_INACTIVE else STATE_ACTIVE
        }
        if (qsTile.state == STATE_ACTIVE) {
            Log.d("SleepTile", "Releasing wake lock [onClick]")
            releaseWakelock()
        } else {
            Log.d("SleepTile", "Acquiring wake lock [onClick]")
            acquireWakelock()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}