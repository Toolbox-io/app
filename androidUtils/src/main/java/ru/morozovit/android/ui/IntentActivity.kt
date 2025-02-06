package ru.morozovit.android.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.Toast
import ru.morozovit.android.R
import ru.morozovit.android.getParcelableExtraAs

class IntentActivity: Activity() {
    companion object {
        const val EXTRA_INTENT = "ru.morozovit.android.ui.IntentActivity.intent"
        const val EXTRA_PACKAGE_NAME = "ru.morozovit.android.ui.IntentActivity.package"
        const val EXTRA_CLASS_NAME = "ru.morozovit.android.ui.IntentActivity.class"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val intent = try {
                intent.getParcelableExtraAs<Intent>(EXTRA_INTENT)
            } catch (e: Exception) {
                Intent().apply {
                    component = ComponentName(
                        intent.getStringExtra(EXTRA_PACKAGE_NAME)!!,
                        intent.getStringExtra(EXTRA_CLASS_NAME)!!
                    )
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                resources.getString(R.string.smthwentwrong),
                Toast.LENGTH_SHORT
            ).show()
        }
        finish()
    }
}