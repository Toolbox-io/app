package io.toolbox

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import io.toolbox.SettingsV1.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import io.toolbox.SettingsV1.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import io.toolbox.SettingsV1.Applocker.UnlockMode.NOTHING_SELECTED

object SettingsV1 {
    private lateinit var sharedPref: SharedPreferences
    private var init = false

    private interface SettingsObj {
        fun init()
    }

    fun init() {
        if (!init) {
            sharedPref = App.context.getSharedPreferences("main", Context.MODE_PRIVATE)
            // Init sub-objects
            Applocker.init()
            UnlockProtection.init()
            Tiles.init()
            init = true
        }
    }

    var update_dsa
        get() = sharedPref.getBoolean("update_dsa", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("update_dsa", value)
                apply()
            }
        }

    var globalPassword
        get() = sharedPref.getString("globalPassword", "")!!
        set(value) {
            with(sharedPref.edit()) {
                putString("globalPassword", value)
                apply()
            }
        }

    var globalPasswordEnabled
        get() = sharedPref.getBoolean("globalPasswordEnabled", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("globalPasswordEnabled", value)
                apply()
            }
        }

    object Applocker: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref = App.context.getSharedPreferences("applocker", Context.MODE_PRIVATE)
                init = true
            }
        }

        object UnlockMode {
            const val NOTHING_SELECTED = -1
            const val LONG_PRESS_APP_INFO = 0
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

        var password
            get() = sharedPref.getString("password", "")!!
            set(value) {
                if (value != "") with(sharedPref.edit()) {
                    putString("password", value)
                    apply()
                }
            }

        var unlockMode: Int
            get() = sharedPref.getInt("unlockMode", if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) UnlockMode.LONG_PRESS_APP_INFO else UnlockMode.LONG_PRESS_OPEN_APP_AGAIN).let {
                if (it == UnlockMode.NOTHING_SELECTED) return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) UnlockMode.LONG_PRESS_APP_INFO else UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
                return it
            }
            set(value) {
                if (value in 0..UnlockMode.LONG_PRESS_OPEN_APP_AGAIN) with(sharedPref.edit()) {
                    putInt("unlockMode", value)
                    apply()
                } else {
                    throw IllegalArgumentException("The argument must be from 0 to PRESS_TITLE.")
                }
            }

    }

    object UnlockProtection: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref =
                    App.context.getSharedPreferences("unlockProtection", Context.MODE_PRIVATE)
                Actions.init()
                init = true
            }
        }

        var enabled
            get() = sharedPref.getBoolean("enabled", false)
            set(value) {
                with(sharedPref.edit()) {
                    putBoolean("enabled", value)
                    apply()
                }
            }

        var unlockAttempts
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
                        App.context.getSharedPreferences("unlockProtection.actions", Context.MODE_PRIVATE)
                    init = true
                }
            }

            var alarm
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

            var currentCustomAlarm
                get() = sharedPref.getString("currentCustomAlarm", "")!!
                set(value) {
                    with(sharedPref.edit()) {
                        putString("currentCustomAlarm", value)
                        commit()
                    }
                }

            var intruderPhoto
                get() = sharedPref.getBoolean("intruderPhoto", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("intruderPhoto", value)
                        commit()
                    }
                }
        }
    }

    object Tiles: SettingsObj {
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref = App.context.getSharedPreferences("tiles", Context.MODE_PRIVATE)
                init = true
            }
        }

        var sleep
            get() = sharedPref.getBoolean("sleep", false)
            set(value) {
                with(sharedPref.edit()) {
                    putBoolean("sleep", value)
                    apply()
                }
            }
    }
}