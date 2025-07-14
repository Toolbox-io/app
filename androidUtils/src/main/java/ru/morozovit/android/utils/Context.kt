@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android.utils

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import java.util.regex.Pattern
import kotlin.reflect.KClass

/**
 * Gets a system service by name from a [Fragment].
 *
 * @param name The service name
 */
inline fun Fragment.getSystemService(name: String): Any? = requireActivity().getSystemService(name)
/**
 * Gets a system service by Java class from a [Fragment].
 *
 * @param cls The service Java class.
 */
inline fun <T> Fragment.getSystemService(cls: Class<T>): T? = requireActivity().getSystemService(cls)
/**
 * Gets a system service by [KClass] from a [Context].
 *
 * @param cls The service Kotlin class.
 */
inline fun <T: Any> Context.getSystemService(cls: KClass<T>): T? = getSystemService(cls.java)
/**
 * Gets a system service by [KClass] from a [Fragment].
 *
 * @param cls The service Kotlin class.
 */
inline fun <T: Any> Fragment.getSystemService(cls: KClass<T>): T? = requireActivity().getSystemService(cls)

/**
 * Type alias for a [BetterActivityResult] launcher.
 */
typealias ActivityLauncher = BetterActivityResult<Intent, ActivityResult>

/**
 * Gets a launcher for activity results from an [ActivityResultCaller].
 *
 * MUST be called before any UI is initialized.
 */
inline val ActivityResultCaller.activityResultLauncher get() =
    BetterActivityResult.registerActivityForResult(this)

/**
 * Gets the display name of a file from a content URI.
 * @param uri The content URI.
 * @return The file name.
 * @throws UnsupportedOperationException if the URI is not a content URI.
 */
fun Context.getFileName(uri: Uri): String {
    if (uri.scheme == "content") {
        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )!!.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return uri.lastPathSegment.let { name ->
            name!!
            val matcher = Pattern.compile("\\w+:(.*/)*(.*)").matcher(name)
            if (matcher.find()) {
                matcher.group(2)
            } else {
                name
            }
        }
    } else {
        throw UnsupportedOperationException("Cannot process non-\"content://\" URIs")
    }
}

/**
 * Gets the display name of a file from a content URI in a [Fragment].
 *
 * @param uri The `content://` URI.
 */
inline fun Fragment.getFileName(uri: Uri) = requireActivity().getFileName(uri)

/**
 * Opens a URL in a browser from a [Context].
 * @param url The [Uri] to open.
 */
inline fun Context.openUrl(url: Uri) = startActivity(Intent(Intent.ACTION_VIEW, url))
/**
 * Opens a URL in a browser from a [Context].
 * @param url The string URL to open. Adds `https://` if the scheme is missing.
 */
inline fun Context.openUrl(url: String) = openUrl(
    url.let {
        if (!it.matches("^[\\w-_]://".toRegex())) {
            "https://$it"
        } else it
    }.toUri()
)

/**
 * Returns true if the device screen is currently locked.
 */
inline val Context.isScreenLocked get() = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

/**
 * Posts a notification if the
 * [POST_NOTIFICATIONS][android.Manifest.permission.POST_NOTIFICATIONS]
 * permission is granted.
 * @param id The notification ID.
 * @param notification The notification to post.
 */
inline fun Context.notifyIfAllowed(
    id: Int,
    notification: Notification
) {
    with (NotificationManagerCompat.from(this)) {
        if (
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@with
        }
        notify(id, notification)
    }
}

/**
 * Gets the application label (name) for the given package name.
 *
 * @param context The context to use for package manager lookup.
 * @param packageName The package name to look up.
 * @return The application label, or `null` if not found.
 */
fun Context.appName(packageName: String) = try {
    packageManager.let {
        it.getApplicationLabel(
            it.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
        ).toString()
    }
} catch (_: PackageManager.NameNotFoundException) {
    null
}