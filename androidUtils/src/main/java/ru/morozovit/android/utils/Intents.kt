@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Parcelable
import androidx.core.net.toUri
import java.io.Serializable

/**
 * Gets a [Parcelable] extra from an [Intent], handling API differences.
 * @param key The extra key.
 * @return The [Parcelable] extra, or null if not found.
 */
inline fun <reified T: Parcelable> Intent.getParcelableExtraAs(
    key: String
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(key, T::class.java)
} else {
    @Suppress("DEPRECATION")
    getParcelableExtra(key)
}

/**
 * Gets a [Serializable] extra from an [Intent], handling API differences.
 * @param key The extra key.
 * @return The [Serializable] extra, or null if not found.
 */
inline fun <reified T: Serializable> Intent.getSerializableExtraAs(
    key: String
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getSerializableExtra(key, T::class.java)
} else {
    @Suppress("DEPRECATION")
    getSerializableExtra(key) as T
}

/**
 * Creates an intent to open a document, optionally filtering by file types.
 * @param fileTypes The MIME types to filter for.
 * @return The intent for opening a document.
 */
@Suppress("unused")
inline fun openDocumentIntent(vararg fileTypes: String) =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        if (fileTypes.size == 1) {
            type = fileTypes[0]
        } else {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, fileTypes)
        }
    }

/**
 * Launches the home screen activity.
 */
inline fun Context.homeScreen() = startActivity(
    Intent(ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = FLAG_ACTIVITY_NEW_TASK
    }
)

private val PRIMARY_STORAGE_URI = "content://com.android.externalstorage.documents/root/primary".toUri()

/**
 * Attempts to launch a file manager for the primary storage. Tries several known intents.
 * @return True if a file manager was successfully launched, false otherwise.
 */
fun Context.launchFiles(): Boolean {
    fun launchIntentWithComponent(
        action: String,
        componentName: ComponentName? = null
    ) = try {
        startActivity(
            Intent(action, PRIMARY_STORAGE_URI).apply {
                if (componentName != null) setComponent(componentName)
            }
        )
        true
    } catch (_: Throwable) {
        false
    }

    fun intent1() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.google.android.documentsui",
            "com.android.documentsui.files.FilesActivity"
        )
    )
    fun intent2() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.android.documentsui",
            "com.android.documentsui.files.FilesActivity"
        )
    )
    fun intent3() = launchIntentWithComponent(
        Intent.ACTION_VIEW,
        ComponentName(
            "com.android.documentsui",
            "com.android.documentsui.FilesActivity"
        )
    )
    fun intent4() = launchIntentWithComponent(Intent.ACTION_VIEW)
    fun intent5() = launchIntentWithComponent("android.provider.action.BROWSE")
    fun intent6() = launchIntentWithComponent("android.provider.action.BROWSE_DOCUMENT_ROOT")

    return intent1() || intent2() || intent3() || intent4() || intent5() || intent6()
}

/**
 * Creates a [PendingIntent] for a notification button broadcast.
 * @param action The action string for the intent.
 * @return The [PendingIntent].
 */
inline fun Context.notificationButtonPendingIntent(
    action: String
) = PendingIntent.getBroadcast(
    this,
    0,
    Intent(action).setPackage(packageName),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)!!