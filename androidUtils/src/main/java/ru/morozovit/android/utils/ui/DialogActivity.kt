package ru.morozovit.android.utils.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.postDelayed

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
        @PublishedApi internal var onCreate: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var onPause: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var onResume: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var onDestroy: (DialogActivity.() -> Unit)? = null
        @PublishedApi internal var onDismiss: (DialogActivity.() -> Boolean)? = null

        inline fun getLaunchIntent(
            context: Context,
            title: String,
            body: String,
            positiveButtonText: String? = null,
            negativeButtonText: String? = null,
            neutralButtonText: String? = null,
            noinline positiveButtonOnClick: (DialogActivity.() -> Unit)? = null,
            noinline negativeButtonOnClick: (DialogActivity.() -> Unit)? = null,
            noinline neutralButtonOnClick: (DialogActivity.() -> Unit)? = null,
            noinline onCreate: (DialogActivity.() -> Unit)? = null,
            noinline onPause: (DialogActivity.() -> Unit)? = null,
            noinline onResume: (DialogActivity.() -> Unit)? = null,
            noinline onDestroy: (DialogActivity.() -> Unit)? = null,
            noinline onDismiss: (DialogActivity.() -> Boolean)? = null,
            intentModifier: (Intent.() -> Intent) = {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                this
            }
        ): Intent {
            if (positiveButtonText != null) {
                this.positiveButtonOnClick = positiveButtonOnClick
            }
            if (negativeButtonText != null) {
                this.negativeButtonOnClick = negativeButtonOnClick
            }
            if (neutralButtonText != null) {
                this.neutralButtonOnClick = neutralButtonOnClick
            }
            if (onCreate != null) {
                this.onCreate = onCreate
            }
            if (onPause != null) {
                this.onPause = onPause
            }
            if (onResume != null) {
                this.onResume = onResume
            }
            if (onDestroy != null) {
                this.onDestroy = onDestroy
            }
            if (onDismiss != null) {
                this.onDismiss = onDismiss
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
            noinline onCreate: (DialogActivity.() -> Unit)? = null,
            noinline onPause: (DialogActivity.() -> Unit)? = null,
            noinline onResume: (DialogActivity.() -> Unit)? = null,
            noinline onDestroy: (DialogActivity.() -> Unit)? = null,
            noinline onDismiss: (DialogActivity.() -> Boolean)? = null,
            intentModifier: (Intent.() -> Intent) = {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                this
            }
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
                    onCreate,
                    onPause,
                    onResume,
                    onDestroy,
                    onDismiss,
                    intentModifier
                )
            )
        }
    }

    var open by mutableStateOf(true)

    lateinit var dialog: AlertDialog
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val title = intent.extras!!.getString(EXTRA_TITLE)!!
            val body = intent.extras!!.getString(EXTRA_BODY)!!
            val positiveButtonText = intent.extras!!.getString(EXTRA_POSITIVE_BUTTON_TEXT)
            val negativeButtonText = intent.extras!!.getString(EXTRA_NEGATIVE_BUTTON_TEXT)

            enableEdgeToEdge()
            setContent {
                MaterialTheme {
                    if (open) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                open = false

                                handler.postDelayed(500) {
                                    if (onDismiss?.invoke(this) != false) {
                                        super.finish()
                                    }
                                }
                            },
                            title = {
                                Text(title)
                            },
                            text = {
                                Text(body)
                            },
                            confirmButton = {
                                if (positiveButtonText != null) {
                                    TextButton(onClick = { positiveButtonOnClick!!() }) {
                                        Text(positiveButtonText)
                                    }
                                }
                            },
                            dismissButton = {
                                if (negativeButtonText != null) {
                                    TextButton(onClick = { positiveButtonOnClick!!() }) {
                                        Text(negativeButtonText)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            onCreate?.invoke(this)
        } catch (e: Exception) {
            e.printStackTrace()
            setResult(RESULT_ERROR)
            finish()
        }
    }

    override fun finish() {
        runCatching {
            open = false
        }
        handler.postDelayed(500) {
            super.finish()
        }
    }

    override fun onPause() {
        super.onPause()
        onPause?.invoke(this)
    }

    override fun onResume() {
        super.onResume()
        onResume?.invoke(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroy?.invoke(this)
    }
}