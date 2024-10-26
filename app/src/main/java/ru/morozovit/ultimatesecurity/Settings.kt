package ru.morozovit.ultimatesecurity

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.NOTHING_SELECTED
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE

object Settings {
    lateinit var applicationContext: Context
    private lateinit var sharedPref: SharedPreferences
    private var init = false

    private interface SettingsObj {
        fun init()
    }

    fun init(context: Context) {
        if (!init) {
            applicationContext = context
            sharedPref = applicationContext.getSharedPreferences("main", Context.MODE_PRIVATE)
            // Init sub-objects
            Applocker.init()
            UnlockProtection.init()
            init = true
        }
    }

    var installPackage_dsa: Boolean
        get() = sharedPref.getBoolean("installPackage_dsa", false)
        set(value) {
            if (value) {
                with(sharedPref.edit()) {
                    putBoolean("installPackage_dsa", true)
                    apply()
                }
            }
        }

    var update_dsa: Boolean
        get() = sharedPref.getBoolean("update_dsa", false)
        set(value) {
            if (value) {
                with(sharedPref.edit()) {
                    putBoolean("update_dsa", true)
                    apply()
                }
            }
        }

    val accessibility: Boolean get()  =
        try {
            Service.instance != null
        } catch (e: Settings.SettingNotFoundException) {
            false
        }

    object Applocker: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref =
                    applicationContext.getSharedPreferences("applocker", Context.MODE_PRIVATE)
                init = true
            }
        }

        object UnlockMode {
            const val NOTHING_SELECTED = -1
            const val LONG_PRESS_APP_INFO = 0
            const val LONG_PRESS_CLOSE = 1
            const val LONG_PRESS_TITLE = 2
            const val PRESS_TITLE = 3
            const val LONG_PRESS_OPEN_APP_AGAIN = 4
        }

        var apps: Set<String>
            get() = sharedPref.getStringSet("selectedApps", setOf())!!
            set(value) {
                if (value.isNotEmpty()) with(sharedPref.edit()) {
                    putStringSet("selectedApps", value)
                    apply()
                }
            }

        var password: String
            get() = sharedPref.getString("password", "")!!
            set(value) {
                if (value != "") with(sharedPref.edit()) {
                    putString("password", value)
                    apply()
                }
            }

        var unlockMode: Int
            get() = sharedPref.getInt("unlockMode", if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) LONG_PRESS_APP_INFO else LONG_PRESS_OPEN_APP_AGAIN).let {
                if (it == NOTHING_SELECTED) return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) LONG_PRESS_APP_INFO else LONG_PRESS_OPEN_APP_AGAIN
                return it
            }
            set(value) {
                if (value in 0..LONG_PRESS_OPEN_APP_AGAIN) with(sharedPref.edit()) {
                    putInt("unlockMode", value)
                    apply()
                } else {
                    throw IllegalArgumentException("The argument must be from 0 to PRESS_TITLE.")
                }
            }

        fun getUnlockModeDescription(value: Int, resources: Resources) = when (value) {
            LONG_PRESS_APP_INFO -> resources.getString(R.string.lp_ai)
            LONG_PRESS_CLOSE -> resources.getString(R.string.lp_c)
            LONG_PRESS_TITLE -> resources.getString(R.string.lp_t)
            PRESS_TITLE -> resources.getString(R.string.p_t)
            LONG_PRESS_OPEN_APP_AGAIN -> resources.getString(R.string.lp_oaa)
            else -> ""
        }
    }

    object UnlockProtection: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref =
                    applicationContext.getSharedPreferences("unlockProtection", Context.MODE_PRIVATE)
                Actions.init()
                init = true
            }
        }

        var enabled: Boolean
            get() = sharedPref.getBoolean("enabled", false)
            set(value) {
                with(sharedPref.edit()) {
                    putBoolean("enabled", value)
                    apply()
                }
            }

        var unlockAttempts: Int
            get() = sharedPref.getInt("unlockAttempts", 2)
            set(value) = with(sharedPref.edit()) {
                putInt("unlockAttempts", value)
                apply()
            }

        object Actions: SettingsObj {
            private lateinit var sharedPref: SharedPreferences
            private var init = false

            override fun init() {
                if (!init) {
                    sharedPref =
                        applicationContext.getSharedPreferences("unlockProtection.actions", Context.MODE_PRIVATE)
                    init = true
                }
            }

            var alarm: Boolean
                get() = sharedPref.getBoolean("alarm", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("alarm", value)
                        commit()
                    }
                }

            var customAlarms: Set<String>
                get() = sharedPref.getStringSet("customAlarms", setOf())!!
                set(value) {
                    with(sharedPref.edit()) {
                        putStringSet("customAlarms", value)
                        commit()
                    }
                }

            var currentCustomAlarm: String
                get() = sharedPref.getString("currentCustomAlarm", "")!!
                set(value) {
                    with(sharedPref.edit()) {
                        putString("currentCustomAlarm", value)
                        commit()
                    }
                }
        }
    }
}