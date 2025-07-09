@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import io.toolbox.Settings.materialYouEnabled
import ru.morozovit.android.NotificationIdManager
import java.lang.ref.WeakReference


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var mContext: WeakReference<Context>
        val context get() = mContext.get()!!

        var authenticated = false

        private val NotificationIdManager = NotificationIdManager(1, 2, 4..6)

        // Notification channels
        const val UPDATE_CHANNEL_ID = "update"
        const val UPDATE_NOTIFICATION_ID = 1

        const val IP_FG_SERVICE_CHANNEL_ID = "ip_fgservice"
        const val IP_FG_SERVICE_NOTIFICATION_ID = 2

        const val IP_PHOTO_TAKEN_CHANNEL_ID = "photo_taken"
        val IP_PHOTO_TAKEN_NOTIFICATION_ID get() = NotificationIdManager.getAndReserve()

        const val ACCESSIBILITY_CHANNEL_ID = "accessibility"
        const val ACCESSIBILITY_NOTIFICATION_ID = 4

        const val SLEEP_TILE_CHANNEL_ID = "sleep_tile"
        const val SLEEP_TILE_NOTIFICATION_ID = 5

        const val DONT_TOUCH_MY_PHONE_CHANNEL_ID = "dont_touch_my_phone"
        const val DONT_TOUCH_MY_PHONE_NOTIFICATION_ID = 6

        // Other constants
        var githubRateLimitRemaining: Long = -1
        const val GITHUB_TOKEN = "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"
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

    private inline fun materialYouForViews() {
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
    }

    private inline fun createNotificationChannels() {
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

            // Sleep tile foreground service
            createNotificationChannel(
                name = resources.getString(R.string.sleep_tile_service),
                description = resources.getString(R.string.sleep_tile_service_d),
                importance = NotificationManager.IMPORTANCE_LOW,
                id = SLEEP_TILE_CHANNEL_ID
            )

            // Don't touch my phone service
            createNotificationChannel(
                name = resources.getString(R.string.dtmp_service),
                description = resources.getString(R.string.dtmp_service_d),
                importance = NotificationManager.IMPORTANCE_LOW,
                id = DONT_TOUCH_MY_PHONE_CHANNEL_ID
            )
        }
    }

    private fun cleanCache() {
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
    }

    override fun onCreate() {
        super.onCreate()

        // Variables
        mContext = WeakReference(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Initialize
        Settings.init(this)
        materialYouForViews()
        createNotificationChannels()
        cleanCache()
        IssueReporter.init()
    }
}