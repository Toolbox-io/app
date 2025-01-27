package ru.morozovit.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.android.alertDialog

class DialogActivity: AppCompatActivity() {
    companion object {
        const val RESULT_ERROR = RESULT_FIRST_USER

        const val EXTRA_TITLE = "ru.morozovit.android.DialogActivity.TITLE"
        const val EXTRA_BODY = "ru.morozovit.android.DialogActivity.MESSAGE"
        const val EXTRA_POSITIVE_BUTTON_TEXT = "ru.morozovit.android.DialogActivity.POSITIVE_BUTTON_TEXT"
        const val EXTRA_NEGATIVE_BUTTON_TEXT = "ru.morozovit.android.DialogActivity.NEGATIVE_BUTTON_TEXT"
        const val EXTRA_NEUTRAL_BUTTON_TEXT = "ru.morozovit.android.DialogActivity.NEUTRAL_BUTTON_TEXT"

        @PublishedApi internal var positiveButtonOnClick: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var negativeButtonOnClick: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var neutralButtonOnClick: (DialogActivity.() -> Unit)? = null

        inline fun getLaunchIntent(
            context: Context,
            title: String,
            body: String,
            positiveButtonText: String? = null,
            negativeButtonText: String? = null,
            neutralButtonText: String? = null,
            noinline positiveButtonOnClick: (DialogActivity.() -> Unit)? = {},
            noinline negativeButtonOnClick: (DialogActivity.() -> Unit)? = {},
            noinline neutralButtonOnClick: (DialogActivity.() -> Unit)? = {},
            intentModifier: (Intent.() -> Intent) = {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                this
            }
        ): Intent {
            if (positiveButtonText != null) {
                this.positiveButtonOnClick = positiveButtonOnClick!!
            }
            if (negativeButtonText != null) {
                this.negativeButtonOnClick = negativeButtonOnClick!!
            }
            if (neutralButtonText != null) {
                this.neutralButtonOnClick = neutralButtonOnClick!!
            }

            return Intent(context, DialogActivity::class.java).let {
                it.putExtra(EXTRA_TITLE, title)
                it.putExtra(EXTRA_BODY, body)
                it.putExtra(EXTRA_POSITIVE_BUTTON_TEXT, positiveButtonText)
                it.putExtra(EXTRA_NEGATIVE_BUTTON_TEXT, negativeButtonText)
                it.putExtra(EXTRA_NEUTRAL_BUTTON_TEXT, neutralButtonText)
                intentModifier(it)
            }
        }

        inline fun show(
            context: Context,
            title: String,
            body: String,
            positiveButtonText: String? = null,
            negativeButtonText: String? = null,
            neutralButtonText: String? = null,
            noinline positiveButtonOnClick: (DialogActivity.() -> Unit)? = null,
            noinline negativeButtonOnClick: (DialogActivity.() -> Unit)? = null,
            noinline neutralButtonOnClick: (DialogActivity.() -> Unit)? = null,
            intentModifier: (Intent.() -> Intent) = { this }
        ) {
            context.startActivity(
                getLaunchIntent(
                    context,
                    title,
                    body,
                    positiveButtonText,
                    negativeButtonText,
                    neutralButtonText,
                    positiveButtonOnClick,
                    negativeButtonOnClick,
                    neutralButtonOnClick,
                    intentModifier
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(View(this))

            val title = intent.extras!!.getString(EXTRA_TITLE)!!
            val body = intent.extras!!.getString(EXTRA_BODY)!!
            val positiveButtonText = intent.extras!!.getString(EXTRA_POSITIVE_BUTTON_TEXT)
            val negativeButtonText = intent.extras!!.getString(EXTRA_NEGATIVE_BUTTON_TEXT)
            val neutralButtonText = intent.extras!!.getString(EXTRA_NEUTRAL_BUTTON_TEXT)

            alertDialog {
                title(title)
                message(body)
                if (positiveButtonText != null) {
                    positiveButton(positiveButtonText) {
                        positiveButtonOnClick!!(this@DialogActivity)
                    }
                }
                if (negativeButtonText != null) {
                    negativeButton(negativeButtonText) {
                        negativeButtonOnClick!!(this@DialogActivity)
                    }
                }
                if (neutralButtonText != null) {
                    neutralButton(neutralButtonText) {
                        neutralButtonOnClick!!(this@DialogActivity)
                    }
                }
            }
        } catch (e: Exception) {
            setResult(RESULT_ERROR)
            finish()
        }
    }
}