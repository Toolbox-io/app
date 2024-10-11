package ru.morozovit.ultimatesecurity

import android.content.Context
import android.content.SharedPreferences

object Settings {
    private lateinit var applicationContext: Context
    private var init = false

    fun init(context: Context) {
        if (!init) {
            applicationContext = context
            // Init sub-objects
            Applocker.init()
            init = true
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
    }
}