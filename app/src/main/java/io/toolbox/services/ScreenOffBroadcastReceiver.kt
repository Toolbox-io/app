package io.toolbox.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.toolbox.Settings

class ScreenOffBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF && Settings.Applocker.unlockDuration >= 10) {
            Log.d("Applocker", "Screen locked, locking all apps")
            Accessibility.instance?.lock = false
        }
    }
}