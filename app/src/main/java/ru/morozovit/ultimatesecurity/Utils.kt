@file:Suppress("unused")

package ru.morozovit.ultimatesecurity

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment


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

fun Context.fileExists(contentUri: String): Boolean = fileExists(Uri.parse(contentUri))

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

val View.relativeX: Int get() {
    return (parent as ViewGroup).screenX - screenX
}

val View.relativeY: Int get() {
    return (parent as ViewGroup).screenY - screenY
}