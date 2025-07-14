@file:Suppress("NOTHING_TO_INLINE", "unused")

package ru.morozovit.android.utils.ui

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.webkit.WebView
import androidx.annotation.AttrRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ru.morozovit.android.utils.ComposeLifecycleOwner

/**
 * Evaluates JavaScript in a [WebView] without a callback.
 * @param script The JavaScript code to evaluate.
 */
inline fun WebView.evaluateJavascript(script: String) = evaluateJavascript(script, null)

/**
 * Creates a [ComposeView] and sets its content.
 * @param lifecycleOwner Optional lifecycle owner to attach.
 * @param content Composable content to set.
 * @return The created [ComposeView].
 */
@JvmName("createComposeView")
inline fun Context.ComposeView(
    lifecycleOwner: ComposeLifecycleOwner? = null,
    crossinline content: @Composable () -> Unit
) = ComposeView(this).apply {
    setContent {
        content()
    }
    if (lifecycleOwner != null) {
        lifecycleOwner.onStart()
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }
}

/**
 * Creates a [ComposeView] and sets its content.
 * @param context The context to use.
 * @param content Composable content to set.
 * @return the created [ComposeView].
 */
inline fun ComposeView(
    context: Context,
    lifecycleOwner: ComposeLifecycleOwner? = null,
    crossinline content: @Composable () -> Unit
) = context.ComposeView(lifecycleOwner, content)

/**
 * Resolves an attribute resource ID from the current theme.
 * @param id The attribute resource ID.
 * @return The resolved resource ID, or `null` if not found.
 */
fun Activity.resolveAttr(@AttrRes id: Int): Int? {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(id, typedValue, true)) {
        typedValue.resourceId.takeIf { it != 0 }
    } else {
        null
    }
}

/**
 * Adds a one-time [OnPreDrawListener] to a [View].
 * As soon as the underlying listener is called, it's removed
 * and the lambda is called.
 *
 * @param listener Lambda returning true to proceed with drawing, false to cancel.
 */
inline fun View.addOneTimeOnPreDrawListener(crossinline listener: () -> Boolean) {
    viewTreeObserver.addOnPreDrawListener(object: OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            return listener()
        }
    })
}