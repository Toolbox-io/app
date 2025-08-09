@file:Suppress( "NOTHING_TO_INLINE", "SameParameterValue", "unused")

package ru.morozovit.android.utils

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_LAUNCHER
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.UsbInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.service.quicksettings.Tile
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import java.io.InputStream
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * Returns the width of the device screen in pixels.
 */
inline val screenWidth get() = Resources.getSystem().displayMetrics.widthPixels
/**
 * Returns the height of the device screen in pixels.
 */
inline val screenHeight get() = Resources.getSystem().displayMetrics.heightPixels

/**
 * Configures a [Tile] and updates it.
 * @param config Lambda to apply configuration to the tile.
 */
inline fun Tile.configure(config: Tile.() -> Unit) {
    config()
    updateTile()
}

/**
 * Returns a list of all endpoints for a [UsbInterface].
 */
inline val UsbInterface.endpointList get() = List(endpointCount) { getEndpoint(it) }

/**
 * Creates an [OnBackPressedCallback].
 * @param enabled Whether the callback is enabled.
 * @param callback The handler to invoke on back pressed.
 * @return The created [OnBackPressedCallback].
 */
inline fun backCallback(
    enabled: Boolean = true,
    crossinline callback: OnBackPressedCallback.() -> Unit
) = object: OnBackPressedCallback(enabled) {
    override fun handleOnBackPressed() {
        callback(this)
    }
}

/**
 * Creates a [SensorEventListener] for orientation sensors.
 * @param callback Called with azimuth, pitch, and roll values.
 * @return The [SensorEventListener].
 */
inline fun orientationSensorEventListener(
    crossinline callback: (Float, Float, Float) -> Unit
) = SensorEventListener { event ->
    val accelerometerData = event.values.takeIf { event.sensor.type == Sensor.TYPE_ACCELEROMETER }
    val geomagneticData = event.values.takeIf { event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD }

    if (accelerometerData != null && geomagneticData != null) {
        val R = FloatArray(9)
        if (
            SensorManager.getRotationMatrix(
                R,
                FloatArray(9),
                accelerometerData,
                geomagneticData
            )
        ) {
            val (azimuth, pitch, roll) = FloatArray(3).also { SensorManager.getOrientation(R, it) }
            callback(azimuth, pitch, roll)
        }
    }
}

/**
 * Creates a [SensorEventListener] for generic sensor events.
 * @param callback Called with the [SensorEvent].
 * @return The [SensorEventListener].
 */
inline fun SensorEventListener(crossinline callback: (SensorEvent) -> Unit) = object: SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        callback(event)
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

/**
 * Tests if an [InputStream] can be read and closed without exception.
 * @return True if successful, false otherwise.
 */
inline fun InputStream.test() = try {
    read()
    close()
    true
} catch (_: Exception) {
    false
}

/**
 * Tests if a [ContentResolver] can open and read a [Uri].
 * @param item The [Uri] to test.
 * @return True if successful, false otherwise.
 */
inline fun ContentResolver.test(item: Uri) = try {
    openInputStream(item)!!.apply {
        read()
        close()
    }
    true
} catch (_: Exception) {
    false
}

/**
 * Compares two [ActivityInfo] objects for content equality.
 * @param other The other [ActivityInfo] to compare.
 * @return True if the contents are equal, false otherwise.
 */
fun ActivityInfo.contentEquals(other: ActivityInfo): Boolean {
    return (
        Build.VERSION.SDK_INT < 26 || colorMode == other.colorMode &&
        Build.VERSION.SDK_INT < 34 || requiredDisplayCategory == other.requiredDisplayCategory &&
        theme == other.theme &&
        launchMode == other.launchMode &&
        documentLaunchMode == other.documentLaunchMode &&
        permission == other.permission &&
        taskAffinity == other.taskAffinity &&
        targetActivity == other.targetActivity &&
        flags == other.flags &&
        screenOrientation == other.screenOrientation &&
        configChanges == other.configChanges &&
        softInputMode == other.softInputMode &&
        uiOptions == other.uiOptions &&
        parentActivityName == other.parentActivityName &&
        maxRecents == other.maxRecents &&
        windowLayout == other.windowLayout
    )
}

/**
 * Gets a launch [Intent] for this [ActivityInfo].
 */
inline val ActivityInfo.launchIntent get() = Intent().apply {
    component = ComponentName(packageName, name)
    flags = FLAG_ACTIVITY_NEW_TASK
}

/**
 * Returns true if this [ActivityInfo] is a launcher activity.
 * @param context The context to use for lookup.
 * @return True if this is a launcher activity, false otherwise.
 */
fun ActivityInfo.isLauncher(context: Context): Boolean {
    with(context) {
        for (
            info in packageManager.queryIntentActivities(
                launchIntent.apply {
                    action = ACTION_MAIN
                    addCategory(CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, name)
                    flags = FLAG_ACTIVITY_NEW_TASK
                },
                PackageManager.GET_RESOLVED_FILTER
            )
        ) {
            val filter = info.filter
            if (
                info.activityInfo.contentEquals(this@isLauncher) &&
                filter != null &&
                filter.hasAction(ACTION_MAIN) &&
                filter.hasCategory(CATEGORY_LAUNCHER)
            ) return true
        }
        return false
    }
}

/**
 * Encodes a string for safe use in JSON.
 *
 * Do NOT use this if you use a JSON encoder that will automatically
 * encode everything for you, or else you will get visible escaped
 * characters.
 *
 * @return The JSON-encoded string.
 */
fun String.encodeJSON() = buildString {
    for (c in this) {
        when (c) {
            '"', '\\', '/' -> append("\\$c")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(
                if (c.code <= 0x1F)
                    String.format("\\u%04x", c.code)
                else c
            )
        }
    }
}

/**
 * Allows [WeakReference] to be used as a delegated property.
 */
@Suppress("unused", "RedundantSuppression")
inline operator fun <T> WeakReference<T>.getValue(
    thisRef: Any,
    property: KProperty<*>
) = get()

/**
 * Converts a [BaseBundle] to a [Map].
 * @return The map of keys to values.
 */
@Suppress("DEPRECATION")
fun BaseBundle.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keySet()) {
        map[key] = get(key)!!
    }
    return map
}

/**
 * Converts a [Bundle] to a [PersistableBundle].
 * All non-persistable data will be lost in the created bundle.
 * 
 * @return The created [PersistableBundle].
 */
fun Bundle.toPersistableBundle(): PersistableBundle {
    val map = toMap()
    val bundle = PersistableBundle()
    for ((key, value) in map) {
        when (value) {
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is String -> bundle.putString(key, value)
            is IntArray -> bundle.putIntArray(key, value)
            is LongArray -> bundle.putLongArray(key, value)
            is DoubleArray -> bundle.putDoubleArray(key, value)
            is PersistableBundle -> bundle.putPersistableBundle(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is BooleanArray -> bundle.putBooleanArray(key, value)
            is Array<*> -> {
                if (value.isArrayOf<String>()) {
                    @Suppress("UNCHECKED_CAST")
                    bundle.putStringArray(key, value as Array<String>)
                }
            }
        }
    }
    return bundle
}

/**
 * Decodes a string from Windows-1251 encoding to UTF-8.
 * @return The decoded string.
 */
inline fun String.decodeWindows1251() = String(
    toByteArray(Charsets.ISO_8859_1),
    Charsets.UTF_8
)

/**
 * Returns true if the [MediaPlayer] is playing, or false if not or if an exception occurs.
 */
inline val MediaPlayer.isPlayingSafe get() = runCatching { isPlaying }.getOrNull() == true

inline fun WebView.settings(block: WebSettings.() -> Unit) = settings.block()