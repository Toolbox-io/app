package io.toolbox

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import io.toolbox.ui.MainActivity

fun Context.mainActivity() {
    startActivity(
        Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    )
}