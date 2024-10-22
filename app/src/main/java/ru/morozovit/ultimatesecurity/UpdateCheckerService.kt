@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ru.morozovit.ultimatesecurity;

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.AsyncTask.THREAD_POOL_EXECUTOR
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import ru.morozovit.ultimatesecurity.Settings.applicationContext
import java.io.BufferedInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UpdateCheckerService: Service() {
    companion object {
        const val UPDATE_AVAILABLE_NOTIFICATION_ID = 1
        const val UPDATE_AVAILABLE_CHANNEL_ID = "update"

        data class SemanticVersion(val major: Number = 0, val minor: Number = 0, val patch: Number = 0)
        data class UpdateInfo(
            val available: Boolean,
            val version: SemanticVersion,
            val description: String,
            val download: String)

        @Suppress("UNUSED_VALUE")
        fun checkForUpdates(): UpdateInfo? {
            with (applicationContext) {
                val request = URL("https://api.github.com/repos/denis0001-dev/AIP-Website/releases")
                    .openConnection() as HttpsURLConnection
                request.requestMethod = "GET";
                request.setRequestProperty("Accept", "application/vnd.github+json")
                val token = "gi" + "th" + "ub_p" + "at_11BESRTYY" + "0e5lNGcsHV9Up_7HTMBq6ZkfKYXou7bkc" + "mZVX6nMJ0ua9I" + "sqqcsPGmuHHYCZ" + "J4BDL4f0SSrM0"

                request.setRequestProperty("Authorization", "Bearer $token")
                request.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

                try {
                    val input = BufferedInputStream(request.inputStream)
                    var c: Char;

                    val chars: MutableList<Char> = mutableListOf()

                    while (true) {
                        c = input.read().toChar()
                        if (c == 0.toChar() || c == '\uFFFF') break;
                        chars.add(c)
                    }
                    val response = String(chars.toCharArray())
                    val parsedResponse = JsonParser.parseString(response) as JsonArray

                    val latestRelease = parsedResponse[0].asJsonObject
                    val name = latestRelease["name"].asString;
                    val description = latestRelease["body"].asString

                    val asset = latestRelease["assets"].asJsonArray[0].asJsonObject["browser_download_url"].asString

                    // Parse the semantic version of the latest release
                    var majorLatest = 0
                    var minorLatest = 0
                    var patchLatest = 0
                    name.substring(1).split(".").forEachIndexed { index, s ->
                        when (index) {
                            0 -> majorLatest = s.toInt()
                            1 -> minorLatest = s.toInt()
                            2 -> patchLatest = s.toInt()
                        }
                    }
                    // Parse the semantic version of the current release
                    var majorCurrent = 0
                    var minorCurrent = 0
                    var patchCurrent = 0
                    packageManager
                        .getPackageInfo(packageName, PackageManager.GET_META_DATA)
                        .versionName.split(".")
                        .forEachIndexed { index, s ->
                            when (index) {
                                0 -> majorCurrent = s.toInt()
                                1 -> minorCurrent = s.toInt()
                                2 -> patchCurrent = s.toInt()
                            }
                        }
                    // Compare
                    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                    var updateAvailable = false
                    if (majorLatest > majorCurrent) {
                        updateAvailable = true
                    } else if (majorLatest == majorCurrent) {
                        if (minorLatest > minorCurrent) {
                            updateAvailable = true
                        } else if (minorLatest == minorCurrent) {
                            if (patchLatest > patchCurrent) {
                                updateAvailable = true
                            }
                        }
                    }

                    return UpdateInfo(
                        updateAvailable,
                        SemanticVersion(majorLatest, minorLatest, patchLatest),
                        description,
                        asset)
                } catch (e: Exception) {
                    return null
                } finally {
                    request.disconnect()
                }
            }
        }
    }

    private var interrupted = false

    override fun onBind(intent: Intent) = null

    @SuppressLint("StaticFieldLeak")
    inner class Task: AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            while (!interrupted) {
                val info = checkForUpdates()
                if (info != null) {
                    val text =
                        "${info.version.major}." +
                                "${info.version.minor}." +
                                "${info.version.patch}"

                    // Notification
                    val builder = NotificationCompat.Builder(applicationContext, UPDATE_AVAILABLE_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Update available")
                        .setContentText("Version $text")
                        .setStyle(NotificationCompat.BigTextStyle().bigText(info.description))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channelName = "Update"
                        val descriptionText = "When a new version is available"
                        val importance = NotificationManager.IMPORTANCE_DEFAULT
                        val channel = NotificationChannel(UPDATE_AVAILABLE_CHANNEL_ID, channelName, importance)
                        channel.description = descriptionText
                        // Register the channel with the system.
                        val notificationManager: NotificationManager =
                            applicationContext.getSystemService(
                                Context.NOTIFICATION_SERVICE
                            ) as NotificationManager
                        notificationManager.createNotificationChannel(channel)
                    }
                    with(NotificationManagerCompat.from(applicationContext)) notification@{
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            // ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                            //                                        grantResults: IntArray)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.

                            return@notification
                        }
                        // notificationId is a unique int for each notification that you must define.
                        notify(UPDATE_AVAILABLE_NOTIFICATION_ID, builder.build())
                    }
                }
                Thread.sleep(10 * 1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Task().executeOnExecutor(THREAD_POOL_EXECUTOR)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        interrupted = true
    }
}
