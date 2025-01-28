package ru.morozovit.ultimatesecurity.services

import android.Manifest
import android.app.Notification
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.MediaPlayer
import android.net.Uri
import android.os.UserHandle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.App.Companion.IP_PHOTO_TAKEN_NOTIFICATION_ID
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.intruderphoto.IntruderPhotoService.Companion.takePhoto
import java.io.IOException


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

        val intruderPhotoNotifications = mutableListOf<Notification>()
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
            if (attemptsCounter >= Settings.UnlockProtection.unlockAttempts) {
                // Take the required actions
                if (Settings.UnlockProtection.Alarm.enabled) {
                    mediaPlayer.apply {
                        if (mediaPlayer.isPlaying) stop()
                        reset()
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                        )
                        if (Settings.UnlockProtection.Alarm.current == "") {
                            val afd: AssetFileDescriptor =
                                App.context.assets.openFd("alarm.mp3")
                            setDataSource(
                                afd.fileDescriptor,
                                afd.startOffset,
                                afd.length
                            )
                        } else {
                            try {
                                setDataSource(App.context, Uri.parse(Settings.UnlockProtection.Alarm.current))
                            } catch (e: IOException) {
                                Log.w("DeviceAdmin", "Invalid custom alarm URI, falling back to default")
                                Settings.UnlockProtection.Alarm.current = ""
                                val afd: AssetFileDescriptor =
                                    App.context.assets.openFd("alarm.mp3")
                                setDataSource(
                                    afd.fileDescriptor,
                                    afd.startOffset,
                                    afd.length
                                )
                            }
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
                if (Settings.UnlockProtection.IntruderPhoto.enabled) {
                    takePhoto(context, "${System.currentTimeMillis()}")
                }
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        if (intruderPhotoNotifications.isNotEmpty() && Settings.UnlockProtection.IntruderPhoto.nopt) {
            with(NotificationManagerCompat.from(App.context)) {
                intruderPhotoNotifications.forEach {
                    if (ActivityCompat.checkSelfPermission(
                            App.context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@with
                    }
                    // notificationId is a unique int for each notification that you must define.
                    notify(IP_PHOTO_TAKEN_NOTIFICATION_ID, it)
                }
                intruderPhotoNotifications.clear()
            }
        }
        attemptsCounter = 0
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        running = false
        attemptsCounter = 0
    }
}