@file:Suppress("unused")
package ru.morozovit.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.service.quicksettings.Tile
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.EditText
import androidx.annotation.AnimRes
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val screenWidth: Int get() = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int get() = Resources.getSystem().displayMetrics.heightPixels

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

fun Context.fileExists(contentUri: String) = fileExists(Uri.parse(contentUri))

fun Fragment.homeScreen() = requireActivity().homeScreen()

fun Editable?.toInt() = toString().toInt()
fun EditText.toInt() = text.toInt()

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

fun async(exec: () -> Unit) = Thread(exec).apply {
    start()
}

fun MaterialAlertDialogBuilder.setNeutralButton(text: CharSequence)
    = setNeutralButton(text, null)
fun MaterialAlertDialogBuilder.setNeutralButton(@StringRes textRes: Int)
    = setNeutralButton(textRes, null)

fun MaterialAlertDialogBuilder.setNegativeButton(text: CharSequence)
        = setNegativeButton(text, null)
fun MaterialAlertDialogBuilder.setNegativeButton(@StringRes textRes: Int)
        = setNegativeButton(textRes, null)

fun MaterialAlertDialogBuilder.setPositiveButton(text: CharSequence)
        = setPositiveButton(text, null)
fun MaterialAlertDialogBuilder.setPositiveButton(@StringRes textRes: Int)
        = setPositiveButton(textRes, null)

fun View.startAnimation(@AnimRes animRes: Int) = startAnimation(loadAnimation(context, animRes))

inline fun Tile.configure(apply: Tile.() -> Unit) {
    apply.invoke(this)
    updateTile()
}