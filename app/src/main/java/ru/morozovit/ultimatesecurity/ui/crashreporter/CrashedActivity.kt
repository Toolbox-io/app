package ru.morozovit.ultimatesecurity.ui.crashreporter

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import ru.morozovit.android.ui.DialogActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.MainActivity

fun startCrashedActivity(exception: Throwable, context: Context) {
    val handler = Handler(Looper.getMainLooper())
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )
    handler.postDelayed(250) {
        DialogActivity.show(
            context = context,
            title = context.resources.getString(R.string.im_sorry),
            body = context.resources.getString(R.string.crash_d),
            positiveButtonText = context.resources.getString(R.string.continue1),
            positiveButtonOnClick = {
                finish()
            },
            negativeButtonText = context.resources.getString(R.string.details),
            negativeButtonOnClick = {
                context.startActivity(
                    Intent(context, ExceptionDetailsActivity::class.java).apply {
                        putExtra("exception", exception)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
    }
}