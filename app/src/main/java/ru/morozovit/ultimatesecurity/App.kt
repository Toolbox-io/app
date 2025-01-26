package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null
        val context get() = mContext ?: throw IllegalStateException("Context hasn't been initialized")

        var authenticated = false


        // Notification channels
        const val UPDATE_CHANNEL_ID = "update"
        const val UPDATE_NOTIFICATION_ID = 1

        const val IP_FG_SERVICE_CHANNEL_ID = "ip_fgservice"
        const val IP_FG_SERVICE_NOTIFICATION_ID = 2

        const val IP_PHOTO_TAKEN_CHANNEL_ID = "photo_taken"
        const val IP_PHOTO_TAKEN_NOTIFICATION_ID = 3

        const val ACCESSIBILITY_CHANNEL_ID = "accessibility"
        const val ACCESSIBILITY_NOTIFICATION_ID = 4
    }

    private lateinit var notificationManager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        name: String,
        description: String,
        id: String,
        importance: Int
    ) {
        val channel = NotificationChannel(
            id,
            name,
            importance
        )
        channel.description = description
        notificationManager.createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        // Variables
        mContext = applicationContext
        notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        Settings.init()

        // Material You support for views
        if (materialYouEnabled)
            DynamicColors.applyToActivitiesIfAvailable(
                this,
                DynamicColorsOptions
                    .Builder()
                    .setThemeOverlay(
                        com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight
                    )
                    .build()
            )

        // Migrate settings
        val applockerPassword = Settings.Applocker.password
        if (applockerPassword != "") {
            Settings.Keys.Applocker.set(applockerPassword)
            Settings.Applocker.password = ""
        }

        val appPassword = Settings.globalPassword
        if (appPassword != "") {
            Settings.Keys.App.set(appPassword)
            Settings.globalPassword = ""
        }

        // Create notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Intruder Photo foreground service
            createNotificationChannel(
                name = resources.getString(R.string.fgs_ip),
                description = resources.getString(R.string.fgs_ip_d),
                importance = NotificationManager.IMPORTANCE_LOW,
                id = IP_FG_SERVICE_CHANNEL_ID
            )

            // Update notification
            createNotificationChannel(
                name = resources.getString(R.string.update_n),
                description = resources.getString(R.string.update_n_d),
                importance = NotificationManager.IMPORTANCE_HIGH,
                id = UPDATE_CHANNEL_ID
            )

            // Intruder Photo: photo taken
            createNotificationChannel(
                name = resources.getString(R.string.ip_pt),
                description = resources.getString(R.string.ip_pt_d),
                importance = NotificationManager.IMPORTANCE_HIGH,
                id = IP_PHOTO_TAKEN_CHANNEL_ID
            )

            // Accessibility foreground service
            createNotificationChannel(
                name = resources.getString(R.string.afs),
                description = resources.getString(R.string.afs_d),
                importance = NotificationManager.IMPORTANCE_LOW,
                id = ACCESSIBILITY_CHANNEL_ID
            )
        }
    }
}