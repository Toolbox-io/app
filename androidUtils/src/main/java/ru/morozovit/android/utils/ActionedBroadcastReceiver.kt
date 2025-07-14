@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * A [android.content.BroadcastReceiver] that registers for a specific action.
 * @param actions The actions to listen for.
 */
abstract class ActionedBroadcastReceiver(vararg val actions: String): BroadcastReceiver() {
    inline fun register(context: Context, exported: Boolean = false) {
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter().apply {
                actions.forEach { addAction(it) }
            },
            if (exported)
                ContextCompat.RECEIVER_EXPORTED
            else ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    inline fun unregister(context: Context) = context.unregisterReceiver(this)
}

/**
 * Creates a [BroadcastReceiver] for a specific action.
 * @param actions The actions to listen for.
 * @param receiver The lambda to invoke on receive.
 * @return The created [BroadcastReceiver].
 */
inline fun broadcastReceiver(
    vararg actions: String,
    crossinline receiver: Context.(Intent) -> Unit
) = object: ActionedBroadcastReceiver(*actions) {
    override fun onReceive(context: Context, intent: Intent) {
        if (actions.isEmpty() || intent.action in actions) {
            receiver(context, intent)
        }
    }
}