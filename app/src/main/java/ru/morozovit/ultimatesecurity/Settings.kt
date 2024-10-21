package ru.morozovit.ultimatesecurity

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.NOTHING_SELECTED
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE

object Settings {
    private lateinit var applicationContext: Context
    private lateinit var sharedPref: SharedPreferences
    private var init = false

    fun init(context: Context) {
        if (!init) {
            applicationContext = context
            sharedPref = applicationContext.getSharedPreferences("main", Context.MODE_PRIVATE)
            // Init sub-objects
            Applocker.init()
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

    object Applocker {
        private lateinit var sharedPref: SharedPreferences
        private var init = false

        fun init() {
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
            get() = sharedPref.getInt("unlockMode", LONG_PRESS_APP_INFO).let {
                if (it == NOTHING_SELECTED) return LONG_PRESS_APP_INFO
                return it
            }
            set(value) {
                if (value in 0..PRESS_TITLE) with(sharedPref.edit()) {
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
            else -> ""
        }
    }
}