package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.media.AudioManager


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        Settings.init(context)
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}