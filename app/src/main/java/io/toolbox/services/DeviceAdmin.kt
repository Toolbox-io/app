package io.toolbox.services

import android.app.Notification
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.os.postDelayed
import io.toolbox.App.Companion.IP_PHOTO_TAKEN_NOTIFICATION_ID
import io.toolbox.App.Companion.context
import io.toolbox.Settings
import ru.morozovit.android.utils.notifyIfAllowed


class DeviceAdmin: DeviceAdminReceiver() {
    companion object {
        var running = false
        private var attemptsCounter = 0
            get() = if (field < 0) 0 else field
            set(value) {
                if (value >= 0) field = value
            }

        val intruderPhotoNotifications = mutableListOf<Notification>()
    }

    private val mediaPlayer = MediaPlayer()
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        running = true
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        if (Settings.UnlockProtection.enabled) {
            attemptsCounter++
            if (attemptsCounter >= Settings.UnlockProtection.unlockAttempts) {
                Settings.Actions.run(context, mediaPlayer, audioManager)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        if (intruderPhotoNotifications.isNotEmpty() && Settings.UnlockProtection.IntruderPhoto.nopt) {
            Handler(Looper.getMainLooper()).postDelayed(3000) {
                intruderPhotoNotifications.forEach {
                    context.notifyIfAllowed(IP_PHOTO_TAKEN_NOTIFICATION_ID, it)
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