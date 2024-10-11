@file:Suppress("unused")

package ru.morozovit.ultimatesecurity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.provider.Settings
import androidx.fragment.app.Fragment


val Activity.isAccessibilityPermissionAvailable: Boolean get()  =
    try {
        Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) != 0
    } catch (e: Settings.SettingNotFoundException) {
        false
    }

val Fragment.isAccessibilityPermissionAvailable: Boolean get() = requireActivity().isAccessibilityPermissionAvailable

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

fun Fragment.homeScreen() = requireActivity().homeScreen()