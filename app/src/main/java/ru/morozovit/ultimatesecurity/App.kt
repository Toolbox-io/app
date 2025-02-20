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
import ru.morozovit.android.NotificationIdManager
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled
import java.io.File
import java.lang.ref.WeakReference


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var mContext: WeakReference<Context>
        val context get() = mContext.get()!!

        var authenticated = false

        private val NotificationIdManager = NotificationIdManager(1, 2, 4)

        // Notification channels
        const val UPDATE_CHANNEL_ID = "update"
        const val UPDATE_NOTIFICATION_ID = 1

        const val IP_FG_SERVICE_CHANNEL_ID = "ip_fgservice"
        const val IP_FG_SERVICE_NOTIFICATION_ID = 2

        const val IP_PHOTO_TAKEN_CHANNEL_ID = "photo_taken"
        val IP_PHOTO_TAKEN_NOTIFICATION_ID = NotificationIdManager.getAndReserve()

        const val ACCESSIBILITY_CHANNEL_ID = "accessibility"
        const val ACCESSIBILITY_NOTIFICATION_ID = 4

        // Other constants
        var githubRateLimitRemaining: Long = -1
        const val GITHUB_TOKEN =
            /*"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"*/
            "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"
        const val GITHUB_API_VERSION = "2022-11-28"
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

    override fun onCreate() {
        super.onCreate()

        // Variables
        mContext = WeakReference(this)
        notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        Settings.init(this)

        // Migrate settings
        if (!Settings.migratedFromOldSettings) {
            SettingsV1.init()
            // Passwords
            if (SettingsV1.globalPasswordEnabled)
                Settings.Keys.App.set(SettingsV1.globalPassword)
            Settings.Keys.Applocker.set(SettingsV1.Applocker.password)
            // Global
            Settings.update_dsa = SettingsV1.update_dsa
            // App Locker
            Settings.Applocker.apps = SettingsV1.Applocker.apps
            Settings.Applocker.unlockMode = SettingsV1.Applocker.unlockMode
            // Unlock Protection
            Settings.UnlockProtection.enabled = SettingsV1.UnlockProtection.enabled
            Settings.UnlockProtection.unlockAttempts = SettingsV1.UnlockProtection.unlockAttempts
            // Actions
            Settings.Actions.Alarm.enabled = SettingsV1.UnlockProtection.Actions.alarm
            Settings.Actions.Alarm.customAlarms = SettingsV1.UnlockProtection.Actions.customAlarms
            Settings.Actions.Alarm.current = SettingsV1.UnlockProtection.Actions.currentCustomAlarm
            Settings.Actions.IntruderPhoto.enabled = SettingsV1.UnlockProtection.Actions.intruderPhoto
            // Tiles
            Settings.Tiles.sleep = SettingsV1.Tiles.sleep

            // Flag that migrated
            Settings.migratedFromOldSettings = true

            // Delete old settings
            arrayOf(
                File("$dataDir/shared_prefs/main.xml"),
                File("$dataDir/shared_prefs/applocker.xml"),
                File("$dataDir/shared_prefs/unlockProtection.xml"),
                File("$dataDir/shared_prefs/unlockProtection.actions.xml"),
                File("$dataDir/shared_prefs/tiles.xml")
            ).forEach {
                runCatching {
                    it.delete()
                }
            }
        }

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

        // Clean up the cache
        runCatching {
            for (file in cacheDir.listFiles()!!) {
                runCatching {
                    // older than 1 day (in millis)
                    if (System.currentTimeMillis() - file.lastModified() > 86400000L) {
                        file.delete()
                    }
                }
            }
        }

        // Register exception handler
        Thread.setDefaultUncaughtExceptionHandler(IssueReporter.DEFAULT_HANDLER)
    }
}