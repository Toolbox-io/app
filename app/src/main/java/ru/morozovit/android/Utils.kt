@file:Suppress("unused", "NOTHING_TO_INLINE")
package ru.morozovit.android

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.service.quicksettings.Tile
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.annotation.AnimRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern
import kotlin.reflect.KClass


val screenWidth: Int inline get() = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int inline get() = Resources.getSystem().displayMetrics.heightPixels

fun appName(context: Context, packageName: String): String? {
    try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(
            packageName, PackageManager.GET_META_DATA
        )
        val appName = packageManager.getApplicationLabel(applicationInfo) as String
        return appName
    } catch (e: PackageManager.NameNotFoundException) {
        return null
    }
}

fun Context.homeScreen() {
    val intentHome = Intent(ACTION_MAIN)
    intentHome.addCategory(Intent.CATEGORY_HOME)
    intentHome.flags = FLAG_ACTIVITY_NEW_TASK
    startActivity(intentHome)
}

fun Context.fileExists(contentUri: Uri): Boolean {
    try {
        val file = DocumentFile.fromTreeUri(this, contentUri)!!
        return file.exists()
    } catch (_: Exception) {
        return false
    }
}

inline fun Context.fileExists(contentUri: String) = fileExists(Uri.parse(contentUri))

inline fun Fragment.homeScreen() = requireActivity().homeScreen()

inline fun Editable?.toInt() = toString().toInt()
inline fun EditText.toInt() = text.toInt()

val View.screenX: Int get() {
    val arr = intArrayOf(0, 0)
    getLocationOnScreen(arr)
    return arr[0]
}

val View.screenY: Int get() {
    val arr = intArrayOf(0, 0)
    getLocationOnScreen(arr)
    return arr[1]
}

val View.relativeX: Int
    inline get() = (parent as ViewGroup).screenX - screenX


val View.relativeY: Int
    inline get() = (parent as ViewGroup).screenY - screenY

fun Activity.setWindowFlag(bits: Int, on: Boolean) {
    val win = window
    val winParams = win.attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    win.attributes = winParams
}

fun async(exec: () -> Unit) = Thread(exec).apply {start()}

inline fun MaterialAlertDialogBuilder.setNeutralButton(text: CharSequence)
    = setNeutralButton(text, null)
inline fun MaterialAlertDialogBuilder.setNeutralButton(@StringRes textRes: Int)
    = setNeutralButton(textRes, null)

inline fun MaterialAlertDialogBuilder.setNegativeButton(text: CharSequence)
        = setNegativeButton(text, null)
inline fun MaterialAlertDialogBuilder.setNegativeButton(@StringRes textRes: Int)
        = setNegativeButton(textRes, null)

inline fun MaterialAlertDialogBuilder.setPositiveButton(text: CharSequence)
        = setPositiveButton(text, null)
inline fun MaterialAlertDialogBuilder.setPositiveButton(@StringRes textRes: Int)
        = setPositiveButton(textRes, null)

inline fun View.startAnimation(@AnimRes animRes: Int) = startAnimation(loadAnimation(context, animRes))

inline fun Tile.configure(apply: Tile.() -> Unit) {
    apply.invoke(this)
    updateTile()
}

fun View.createBlankClone(
    includeMargins: Boolean = true,
    fixedWidth: Boolean = false
): View {
    val v = View(context)


    var width = width
    var height = height

    fun setParams() {
        if (layoutParams is MarginLayoutParams && includeMargins) {
            val p = layoutParams as MarginLayoutParams
            width += p.leftMargin + p.rightMargin
            height += p.topMargin + p.bottomMargin
        }
        if (!fixedWidth) {
            if (layoutParams.width == MATCH_PARENT) {
                width = MATCH_PARENT
            }
            if (layoutParams.height == MATCH_PARENT) {
                height = MATCH_PARENT
            }
        }
    }

    if (layoutParams == null)
        addOneTimeOnGlobalLayoutListener(::setParams)
    else
        setParams()

    v.addOneTimeOnGlobalLayoutListener {
        updateLayoutParams {
            this.width = width
            this.height = height
        }
    }
    return v
}

inline fun View.addOneTimeOnGlobalLayoutListener(crossinline listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            listener()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

inline fun View.addOneTimeOnPreDrawListener(crossinline listener: () -> Boolean) {
    viewTreeObserver.addOnPreDrawListener(object: OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            return listener()
        }
    })
}

inline val View.parentViewGroup get() = parent as ViewGroup

inline fun View.removeSelf() = parentViewGroup.removeView(this)
inline val View.pos get() = parentViewGroup.indexOfChild(this)

fun Context.launchFiles(): Boolean {
    val primaryStorageUri = Uri.parse(
        "content://com.android.externalstorage.documents/root/primary"
    )
    fun launchIntent(intent: Intent): Boolean {
        try {
            startActivity(intent)
            return true
        } catch (th: Throwable) {
            return false
        }
    }
    fun launchIntentWithComponent(action: String, componentName: ComponentName? = null):
            Boolean {
        val intent = Intent(action, primaryStorageUri)
        if (componentName != null) {
            intent.setComponent(componentName)
        }
        return launchIntent(intent)
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
inline fun Fragment.launchFiles() = requireActivity().launchFiles()

inline fun Fragment.getSystemService(name: String): Any? = requireActivity().getSystemService(name)
inline fun <T> Fragment.getSystemService(cls: Class<T>): T? = requireActivity().getSystemService(cls)

inline fun <T: Any> Context.getSystemService(cls: KClass<T>): T? = getSystemService(cls.java)
inline fun <T: Any> Fragment.getSystemService(cls: KClass<T>): T? = requireActivity().getSystemService(cls)

inline val Fragment.supportFragmentManager get() = requireActivity().supportFragmentManager

typealias ActivityLauncher = BetterActivityResult<Intent, ActivityResult>
typealias ActivityResultLauncher = ActivityLauncher

val AppCompatActivity.activityResultLauncher get() = BetterActivityResult.registerActivityForResult(this)



fun getOpenDocumentIntent(vararg fileTypes: String) =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        if (fileTypes.size == 1) {
            type = fileTypes[0]
        } else {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, fileTypes)
        }
    }

fun Context.getFileName(uri: Uri): String {
    if (uri.scheme == "content") {
        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )!!.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    } else {
        throw UnsupportedOperationException("Cannot process non-\"content://\" URIs")
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
}

inline fun Fragment.getFileName(uri: Uri) = requireActivity().getFileName(uri)

val UsbInterface.endpointList: List<UsbEndpoint> get() {
    val list = mutableListOf<UsbEndpoint>()
    for (i in 0 until endpointCount) {
        list.add(getEndpoint(i))
    }
    return list.toList()
}

fun Int.toDp(resources: Resources) = this * resources.displayMetrics.density