package ru.morozovit.ultimatesecurity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REPLACED

class UpdateCheckerBroadcastReceiver: BroadcastReceiver() {
    companion object {
        const val ACTION_START_UPDATE_CHECKER = "ru.morozovit.ultimatesecurity." +
                "UpdateCheckerBroadcastReceiver.START_UPDATE_CHECKER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null &&
            context != null && (
                    intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == ACTION_START_UPDATE_CHECKER ||
                    intent.action == ACTION_PACKAGE_REPLACED ||
                    intent.action == ACTION_PACKAGE_ADDED
                    )
            ) {
            if (!UpdateCheckerService.running) context.startService(
                Intent(context, UpdateCheckerService::class.java)
            )
        }
    }
}