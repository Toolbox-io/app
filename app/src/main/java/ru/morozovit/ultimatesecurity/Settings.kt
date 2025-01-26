package ru.morozovit.ultimatesecurity

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import ru.morozovit.android.decrypt
import ru.morozovit.android.encrypt
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.NOTHING_SELECTED
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE
import ru.morozovit.ultimatesecurity.services.Accessibility
import ru.morozovit.ultimatesecurity.ui.Theme
import kotlin.random.Random

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
            Keys.init()
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

    val accessibility get() = Accessibility.instance != null

    @Deprecated("")
    var globalPassword
        get() = sharedPref.getString("globalPassword", "")!!
        set(value) {
            with(sharedPref.edit()) {
                putString("globalPassword", value)
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

    var materialYouEnabled
        get() = sharedPref.getBoolean("materialYouEnabled", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean("materialYouEnabled", value)
                apply()
            }
        }

    var appTheme: Theme
        get() = Theme.entries[sharedPref.getInt("appTheme", Theme.AsSystem.ordinal)]
        set(value) {
            with(sharedPref.edit()) {
                putInt("appTheme", value.ordinal)
                apply()
            }
        }

    object Keys: SettingsObj {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        override fun init() {
            if (!init) {
                sharedPref = ru.morozovit.ultimatesecurity.App.context.getSharedPreferences("keys", Context.MODE_PRIVATE)
                init = true
            }
        }

        interface Key {
            fun set(password: String)
            fun check(password: String): Boolean
            val isSet: Boolean
        }

        private fun generateKey() =
            Random.nextBytes(
                Random.nextInt(1, 16)
            )
                .map {
                    it.toInt().toChar()
                }
                .joinToString("")

        object Applocker: Key {
            private var randomKey: String
                get() {
                    var result = Settings.sharedPref.getString("applockerRandomKey", null)
                    if (result == null) {
                        result = generateKey()
                        randomKey = result
                    }
                    return result
                }
                set(value) {
                    with(Settings.sharedPref.edit()) {
                        putString("applockerRandomKey", value)
                        apply()
                    }
                }

            private var encryptedPassword
                get() = Settings.sharedPref.getString("applockerEncryptedPassoword", "")!!
                set(value) {
                    with(Settings.sharedPref.edit()) {
                        putString("applockerEncryptedPassoword", value)
                        apply()
                    }
                }

            override fun set(password: String) {
                randomKey = generateKey()
                encryptedPassword =
                    if (password.isEmpty()) ""
                    else randomKey.encrypt(password)
            }

            override fun check(password: String): Boolean {
                if (encryptedPassword.isEmpty() && password.isEmpty()) {
                    return true
                }
                val decryptedPassword = try {
                    encryptedPassword.decrypt(password)
                } catch (e: Exception) {
                    null
                }
                return decryptedPassword != null && decryptedPassword == randomKey
            }

            override val isSet get() = encryptedPassword.isNotEmpty()
        }

        object App: Key {
            private var randomKey: String
                get() {
                    var result = Settings.sharedPref.getString("authRandomKey", null)
                    if (result == null) {
                        result = generateKey()
                        randomKey = result
                    }
                    return result
                }
                set(value) {
                    with(Settings.sharedPref.edit()) {
                        putString("authRandomKey", value)
                        apply()
                    }
                }

            private var encryptedPassword
                get() = Settings.sharedPref.getString("authEncryptedPassoword", "")!!
                set(value) {
                    with(Settings.sharedPref.edit()) {
                        putString("authEncryptedPassoword", value)
                        apply()
                    }
                }

            override fun set(password: String) {
                if (password.isNotEmpty()) randomKey = generateKey()
                encryptedPassword =
                    if (password.isEmpty()) ""
                    else randomKey.encrypt(password)
            }

            override fun check(password: String): Boolean {
                if (encryptedPassword.isEmpty() && password.isEmpty()) {
                    return true
                }
                val decryptedPassword = try {
                    encryptedPassword.decrypt(password)
                } catch (e: Exception) {
                    null
                }
                return decryptedPassword != null && decryptedPassword == randomKey
            }

            override val isSet get() = encryptedPassword.isNotEmpty()
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
                with(sharedPref.edit()) {
                    putStringSet("selectedApps", value)
                    apply()
                }
            }

        @Deprecated("Use Settings.Keys.Applocker instead.")
        var password
            get() = sharedPref.getString("password", "")!!
            set(value) {
                with(sharedPref.edit()) {
                    putString("password", value)
                    apply()
                }
            }

        private val DEFAULT_UNLOCK_MODE =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                LONG_PRESS_APP_INFO
            else
                LONG_PRESS_OPEN_APP_AGAIN

        var unlockMode: Int
            get() = sharedPref.getInt("unlockMode", DEFAULT_UNLOCK_MODE).let {
                return if (it == NOTHING_SELECTED) DEFAULT_UNLOCK_MODE else it
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

        var fgServiceEnabled
            get() = sharedPref.getBoolean("fgServiceEnabled", true)
            set(value) {
                with(sharedPref.edit()) {
                    putBoolean("fgServiceEnabled", value)
                    apply()
                }
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

            object Alarm {
                var enabled
                    get() = sharedPref.getBoolean("alarm", false)
                    set(value) {
                        with(sharedPref.edit()) {
                            putBoolean("alarm", value)
                            commit()
                        }
                    }

                var current
                    get() = sharedPref.getString("currentCustomAlarm", "")!!
                    set(value) {
                        with(sharedPref.edit()) {
                            putString("currentCustomAlarm", value)
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
            }

            object IntruderPhoto {
                var enabled
                    get() = sharedPref.getBoolean("intruderPhoto", false)
                    set(value) {
                        with(sharedPref.edit()) {
                            putBoolean("intruderPhoto", value)
                            commit()
                        }
                    }

                var nopt
                    get() = sharedPref.getBoolean("intruderPhoto.nopt", false)
                    set(value) {
                        with(sharedPref.edit()) {
                            putBoolean("intruderPhoto.nopt", value)
                            commit()
                        }
                    }
            }
        }

        val Alarm = Actions.Alarm
        val IntruderPhoto = Actions.IntruderPhoto
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