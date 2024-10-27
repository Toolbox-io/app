package ru.morozovit.ultimatesecurity

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.os.postDelayed
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.alarm
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.currentCustomAlarm
import ru.morozovit.ultimatesecurity.Settings.applicationContext


class DeviceAdmin: DeviceAdminReceiver() {
    companion object {
        var running = false
        private var attemptsCounter = 0
            get() = if (field < 0) 0 else field
            set(value) {
                if (value >= 0) field = value
            }
        private val mediaPlayer = MediaPlayer()
        lateinit var audioManager: AudioManager
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        running = true
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Settings.UnlockProtection.enabled) {
            attemptsCounter++
            Handler(Looper.getMainLooper()).postDelayed(60000) {
                attemptsCounter--
            }
            if (attemptsCounter >= Settings.UnlockProtection.unlockAttempts) {
                // Take the required actions
                if (alarm) {
                    mediaPlayer.apply {
                        if (mediaPlayer.isPlaying) stop()
                        reset()
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                        )
                        if (currentCustomAlarm == "") {
                            val afd: AssetFileDescriptor =
                                applicationContext.assets.openFd("alarm.mp3")
                            setDataSource(
                                afd.fileDescriptor,
                                afd.startOffset,
                                afd.length
                            )
                        } else {
                            setDataSource(applicationContext, Uri.parse(currentCustomAlarm))
                        }
                        prepare()
                        start()

                        Thread {
                            while (mediaPlayer.isPlaying) {
                                audioManager.setStreamVolume(STREAM_ALARM, audioManager.getStreamMaxVolume(STREAM_ALARM), 0);
                                Thread.sleep(100)
                            }
                        }.start()
                    }
                }
                attemptsCounter = 0
            }
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        running = false
        attemptsCounter = 0
    }
}