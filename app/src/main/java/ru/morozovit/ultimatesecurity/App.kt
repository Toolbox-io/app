package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null
        val context get() = mContext ?: throw IllegalStateException("Context hasn't been initialized")

        var authenticated = false
    }

    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        Settings.init()
    }
}