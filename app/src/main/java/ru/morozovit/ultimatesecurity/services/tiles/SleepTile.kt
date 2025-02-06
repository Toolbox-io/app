package ru.morozovit.ultimatesecurity.services.tiles

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.SCREEN_DIM_WAKE_LOCK
import android.service.quicksettings.Tile
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.Tile.STATE_UNAVAILABLE
import android.service.quicksettings.TileService
import android.util.Log
import ru.morozovit.android.configure
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.Settings.Tiles.sleep

class SleepTile: TileService() {
    companion object {
        var isVisible = false
            private set

        var canUpdate = false
            private set

        @Suppress("DEPRECATION")
        val wakeLock by lazy {
            (App.context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(SCREEN_DIM_WAKE_LOCK, "aip:wakelock")!!
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
        if (sleep) {
            isVisible = true
            qsTile.configure {
                enabled = true
            }
            Log.d("SleepTile", "Releasing wake lock [onTileAdded]")
            try {
                wakeLock.release()
            } catch (_: Exception) {
            }
        } else {
            qsTile.configure {
                state = STATE_UNAVAILABLE
            }
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        isVisible = false
        qsTile.configure {
            enabled = true
        }
        Log.d("SleepTile", "Releasing wake lock [onTileRemoved]")
        try {
            wakeLock.release()
        } catch (_: Exception) {}
    }

    override fun onStartListening() {
        super.onStartListening()
        if (sleep) {
            if (!set) {
                qsTile.configure {
                    enabled = true
                }
                Log.d("SleepTile", "Releasing wake lock [onStartListening]")
                try {
                    wakeLock.release()
                } catch (_: Exception) {}
                set = true
            }
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

    var enabled
        inline get() = qsTile.state == STATE_ACTIVE || qsTile.state == STATE_INACTIVE
        inline set(value) {
            qsTile.configure {
                state = if (value) STATE_ACTIVE else STATE_INACTIVE
            }
        }

    @SuppressLint("Wakelock", "WakelockTimeout")
    override fun onClick() {
        if (sleep) {
            qsTile.configure {
                state = if (state == STATE_ACTIVE) STATE_INACTIVE else STATE_ACTIVE
            }
            if (qsTile.state == STATE_ACTIVE) {
                Log.d("SleepTile", "Releasing wake lock [onClick]")
                try {
                    wakeLock.release()
                } catch (_: Exception) {
                }
            } else {
                Log.d("SleepTile", "Acquiring wake lock [onClick]")
                wakeLock.acquire()
            }
        } else {
            qsTile.configure {
                state = STATE_UNAVAILABLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}