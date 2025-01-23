package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null
        val context get() = mContext ?: throw IllegalStateException("Context hasn't been initialized")

        var authenticated = false
    }

    @Suppress("DEPRECATION")
    private fun migrateSettings() {
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
    }

    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        Settings.init()
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

        migrateSettings()
    }
}