package ru.morozovit.ultimatesecurity

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.os.postDelayed
import ru.morozovit.ultimatesecurity.Settings.applicationContext


class DeviceAdmin: DeviceAdminReceiver() {
    companion object {
        var running = false
        private var attemptsCounter = 0
            get() = if (field < 0) 0 else field
            set(value) {
                if (value >= 0) field = value
            }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        running = true
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        if (Settings.UnlockProtection.enabled) {
            attemptsCounter++
            Handler(Looper.getMainLooper()).postDelayed(60000) {
                attemptsCounter--
            }
            if (attemptsCounter >= 2) {
                val afd: AssetFileDescriptor = applicationContext.assets.openFd("alarm.mp3")
                val player = MediaPlayer()
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                player.prepare()
                player.start()
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