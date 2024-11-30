package ru.morozovit.ultimatesecurity

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.content.res.Resources
import android.os.Build
import android.service.quicksettings.Tile.STATE_UNAVAILABLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.NOTHING_SELECTED
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE
import ru.morozovit.ultimatesecurity.services.Accessibility
import ru.morozovit.ultimatesecurity.services.tiles.SleepTile
import ru.morozovit.ultimatesecurity.ui.customization.shortcuts.FilesActivity

object Settings {
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

    var installPackage_dsa
        get() = sharedPref.getBoolean("installPackage_dsa", false)
        set(value) {
            if (value) {
                with(sharedPref.edit()) {
                    putBoolean("installPackage_dsa", true)
                    apply()
                }
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

    var deleteGlobalPasswordDsa
        get() = sharedPref.getBoolean("deleteGlobalPasswordDsa", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("deleteGlobalPasswordDsa", value)
                apply()
            }
        }

    var exitDsa
        get() = sharedPref.getBoolean("exitDsa", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("exitDsa", value)
                apply()
            }
        }

    val accessibility get() = Accessibility.instance != null

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

    var allowBiometric
        get() = sharedPref.getBoolean("allowBiometric", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("allowBiometric", value)
                apply()
            }
        }

    var dontShowInRecents
        get() = sharedPref.getBoolean("dontShowInRecents", false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("dontShowInRecents", value)
                apply()
            }
        }

    object Applocker: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref =
                    App.context.getSharedPreferences("applocker", Context.MODE_PRIVATE)
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

        var password
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

            var intruderPhotoFromBackCam
                get() = sharedPref.getBoolean("intruderPhotoFromBackCam", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("intruderPhotoFromBackCam", value)
                        commit()
                    }
                }

            var intruderPhotoFromFrontCam
                get() = sharedPref.getBoolean("intruderPhotoFromFrontCam", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("intruderPhotoFromFrontCam", value)
                        commit()
                    }
                }

            var intruderPhotoDirEnabled
                get() = sharedPref.getBoolean("intruderPhotoDirEnabled", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("intruderPhotoDirEnabled", value)
                        commit()
                    }
                }

            var intruderPhotoDir
                get() = sharedPref.getString("intruderPhotoDir", "")!!
                set(value) {
                    with(sharedPref.edit()) {
                        putString("intruderPhotoDir", value)
                        commit()
                    }
                }

            var intruderPhotoWarning_dsa
                get() = sharedPref.getBoolean("intruderPhotoWarning_dsa", false)
                set(value) {
                    with(sharedPref.edit()) {
                        putBoolean("intruderPhotoWarning_dsa", value)
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

    object Shortcuts: SettingsObj {
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref = App.context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)
                init = true
            }
        }

        var files
            get() = sharedPref.getBoolean("files", false)
            set(value) {
                with(sharedPref.edit()) {
                    putBoolean("files", value)
                    apply()
                }
                with (App.context) {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this, FilesActivity::class.java),
                        if (value)
                            COMPONENT_ENABLED_STATE_ENABLED
                        else
                            COMPONENT_ENABLED_STATE_DISABLED,
                        DONT_KILL_APP
                    )
                    try {
                        SleepTile.scheduleConfig {
                            state = STATE_UNAVAILABLE
                        }
                    } catch (_: Exception) {}
                }
            }

        var files_choice
            get() = sharedPref.getInt("files_choice", -1)
            set(value) {
                with(sharedPref.edit()) {
                    putInt("files_choice", value)
                    apply()
                }
            }
    }

}