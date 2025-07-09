@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import io.toolbox.App.Companion.DONT_TOUCH_MY_PHONE_CHANNEL_ID
import io.toolbox.App.Companion.DONT_TOUCH_MY_PHONE_NOTIFICATION_ID
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.tiles.SleepTileKeeperService
import ru.morozovit.android.SensorEventListener
import ru.morozovit.android.broadcastReceiver
import ru.morozovit.android.notificationButtonPendingIntent
import ru.morozovit.android.orientationSensorEventListener
import ru.morozovit.android.runOrLog
import kotlin.math.abs
import kotlin.math.roundToInt

class DontTouchMyPhoneService: Service() {
    companion object {
        var instance: DontTouchMyPhoneService? = null
        var working by mutableStateOf(false)

        val ACTION_DISABLE = "${SleepTileKeeperService::class.qualifiedName}.DISABLE"

        inline fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DontTouchMyPhoneService::class.java))
        }

        inline fun stop() = instance?.stopSelf()
    }

    private val disableReceiver = broadcastReceiver(ACTION_DISABLE) { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        disableReceiver.register(this)
    }

    val sensorManager = getSystemService(SensorManager::class.java)!!
    val mediaPlayer by lazy { MediaPlayer() }
    val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    inline fun trigger() = Settings.Actions.run(this, mediaPlayer, audioManager)

    var accelerometerListener = SensorEventListener { event ->
        val (x, y, z) = event.values

        // Simple threshold for touch detection
        if ((x > 5 || y > 5 || z > 20)) trigger()
    }
    var orientationListener = orientationSensorEventListener { _, pitch, roll ->
        currentPitch = (pitch * 100f).roundToInt() / 100.0f
        currentRoll = (roll * 100f).roundToInt() / 100.0f

        val prevPitch = lastData.first
        val prevRoll = lastData.second

        val pitchTest = abs(abs(prevPitch) - abs(currentPitch))
        val rollTest = abs(abs(prevRoll) - abs(currentRoll))

        if (pitchTest >= 0.01 || rollTest >= 0.01) trigger()

        lastData = Pair(currentPitch, currentRoll)
    }

    var currentRoll = 0f
    var currentPitch = 0f
    var lastData = Pair(0f, 0f)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            DONT_TOUCH_MY_PHONE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, DONT_TOUCH_MY_PHONE_CHANNEL_ID)
                .setSmallIcon(R.drawable.do_not_touch)
                .setContentTitle("\"Don't touch my phone\" active")
                .setContentText(
                    """
                        When someone touches your phone, security actions will be taken
                    """.trimIndent().replace("\n", " ")
                )
                .addAction(
                    R.drawable.do_not_touch,
                    "Disable",
                    notificationButtonPendingIntent(ACTION_DISABLE)
                )
                .setPriority(PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()
        )
        instance = this

        // Initialize sensors
        fun tryRegister(sensor: Sensor?, listener: SensorEventListener) {
            runOrLog("DontTouchMyPhone") {
                sensorManager.registerListener(
                    listener,
                    sensor!!,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }

        tryRegister(
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            accelerometerListener
        )

        tryRegister(
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            orientationListener
        )
        tryRegister(
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            orientationListener
        )

        working = true

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        instance = null
        working = false

        // Clean up
        disableReceiver.unregister(this)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(orientationListener)
        runCatching {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    override fun onBind(intent: Intent) = null
}