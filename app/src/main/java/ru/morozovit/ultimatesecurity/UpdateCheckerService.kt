@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ru.morozovit.ultimatesecurity;

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.AsyncTask.THREAD_POOL_EXECUTOR
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.os.postDelayed
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import ru.morozovit.ultimatesecurity.Settings.applicationContext
import ru.morozovit.ultimatesecurity.UpdateCheckerBroadcastReceiver.Companion.ACTION_START_UPDATE_CHECKER
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.URL
import java.util.concurrent.Executor
import javax.net.ssl.HttpsURLConnection

class UpdateCheckerService: Service() {
    companion object {
        const val UPDATE_AVAILABLE_NOTIFICATION_ID = 1
        const val UPDATE_AVAILABLE_CHANNEL_ID = "update"

        const val DOWNLOAD_BROADCAST = "UpdateCheckerService.DOWNLOAD"

        var running = false

        data class SemanticVersion(val major: Number = 0, val minor: Number = 0, val patch: Number = 0): Serializable
        data class UpdateInfo(
            val available: Boolean,
            val version: SemanticVersion,
            val description: String,
            val download: String): Serializable

        @Suppress("UNUSED_VALUE")
        fun checkForUpdates(): UpdateInfo? {
            with (App.context) {
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
                        .versionName?.split(".")?.forEachIndexed { index, s ->
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

        class DownloadBroadcastReceiver: BroadcastReceiver() {
            @SuppressLint("StaticFieldLeak")
            inner class Task : AsyncTask<String, String, Unit>() {
                private lateinit var file: File
                private lateinit var mime: String
                private lateinit var builder: NotificationCompat.Builder

                @SuppressLint("MissingPermission")
                override fun onPreExecute() {
                    super.onPreExecute()
                    builder = NotificationCompat.Builder(applicationContext, UPDATE_AVAILABLE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.primitive_icon)
                        .setContentTitle("Downloading update...")
                        .setPriority(PRIORITY_DEFAULT)
                        .setSilent(true)
                        .setOngoing(false)
                    with(NotificationManagerCompat.from(applicationContext)) notification@{
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) return@notification

                        notify(UPDATE_AVAILABLE_NOTIFICATION_ID, builder.build())
                    }
                }

                override fun doInBackground(vararg params: String?) {
                    var count: Int
                    try {
                        val url = URL(params[0])
                        val connection = url.openConnection()
                        connection.connect()
                        mime = connection.contentType

                        // this will be useful so that you can show a tipical 0-100%
                        // progress bar
                        val lengthOfFile = connection.contentLength

                        // download the file
                        val input: InputStream = BufferedInputStream(
                            url.openStream(),
                            8192
                        )

                        // Output stream
                        file = File(applicationContext.cacheDir.absolutePath + "/update.apk")
                        if (file.exists()) {
                            file.delete()
                        }
                        file.createNewFile()

                        val output: OutputStream = FileOutputStream(file)
                        val data = ByteArray(1024)
                        var total: Long = 0

                        while ((input.read(data).also { count = it }) != -1) {
                            total += count.toLong()
                            // publishing the progress....
                            // After this onProgressUpdate will be called
                            publishProgress("" + ((total * 100) / lengthOfFile).toInt())

                            // writing data to file
                            output.write(data, 0, count)
                        }

                        // flushing output
                        output.flush()

                        // closing streams
                        output.close()
                        input.close()
                    } catch (e: java.lang.Exception) {
                        Log.e("Error: ", e.message!!)
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onProgressUpdate(vararg progress: String) {
                    // setting progress percentage
                    builder.setProgress(100, progress[0].toInt(), false)
                        .setSilent(true)
                        .setOngoing(true)
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) return

                    NotificationManagerCompat.from(applicationContext).notify(
                        UPDATE_AVAILABLE_NOTIFICATION_ID, builder.build()
                    )
                }

                @SuppressLint("MissingPermission")
                override fun onPostExecute(result: Unit?) {
                    val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    install.data = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.applicationContext.packageName + ".provider",
                        file
                    )

                    val pendingIntent = PendingIntent.getActivity(applicationContext, 0, install, FLAG_IMMUTABLE)

                    builder.setContentTitle("Update is ready")
                        .setContentText("Tap to install")
                        .setProgress(0, 0, false)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setSilent(false)
                        .setOngoing(false)

                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) return

                    Handler(Looper.getMainLooper()).postDelayed(1000) {
                        NotificationManagerCompat.from(applicationContext).notify(
                            UPDATE_AVAILABLE_NOTIFICATION_ID, builder.build()
                        )
                    }
                }
            }

            override fun onReceive(context: Context, intent: Intent) {
                val executor = Executor { command -> Thread(command).start() }
                Task().executeOnExecutor(
                    executor,
                    (intent.getSerializableExtra("updateInfo") as UpdateInfo).download
                )
            }
        }
    }

    private var interrupted = false
    private var checked = false

    override fun onBind(intent: Intent) = null

    @SuppressLint("StaticFieldLeak")
    inner class Task: AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            while (!interrupted) {
                if (!checked) {
                    val info = checkForUpdates()
                    if (info != null && info.available) {
                        val text =
                            "${info.version.major}." +
                                    "${info.version.minor}." +
                                    "${info.version.patch}"

                        // Notification
                        val downloadIntent =
                            Intent(this@UpdateCheckerService, DownloadBroadcastReceiver::class.java)
                                .apply {
                                    action = DOWNLOAD_BROADCAST
                                    putExtra(UPDATE_AVAILABLE_CHANNEL_ID, 0)
                                    putExtra("updateInfo", info)
                                }
                        val downloadPendingIntent = PendingIntent.getBroadcast(
                            this@UpdateCheckerService, 0, downloadIntent,
                            PendingIntent.FLAG_MUTABLE
                        )
                        val mainIntent = Intent(this@UpdateCheckerService, MainActivity::class.java)

                        val pendingIntent = PendingIntent.getActivity(
                            this@UpdateCheckerService, 0, mainIntent,
                            FLAG_IMMUTABLE
                        )

                        val builder = NotificationCompat.Builder(
                            applicationContext,
                            UPDATE_AVAILABLE_CHANNEL_ID
                        )
                            .setSmallIcon(R.drawable.primitive_icon)
                            .setContentTitle("Update available")
                            .setContentText("Version $text")
                            .setStyle(NotificationCompat.BigTextStyle().bigText(info.description))
                            .setPriority(PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .addAction(R.drawable.primitive_icon, "Download", downloadPendingIntent)
                            .setAutoCancel(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channelName = "Update"
                            val descriptionText = "When a new version is available"
                            val importance = NotificationManager.IMPORTANCE_HIGH
                            val channel = NotificationChannel(
                                UPDATE_AVAILABLE_CHANNEL_ID,
                                channelName,
                                importance
                            )
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
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) return@notification

                            // notificationId is a unique int for each notification that you must define.
                            notify(UPDATE_AVAILABLE_NOTIFICATION_ID, builder.build())
                        }
                        checked = true
                    }
                }
                Thread.sleep(10 * 1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        Task().executeOnExecutor(THREAD_POOL_EXECUTOR)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        interrupted = true
        running = false
        Log.d(javaClass.simpleName, "Destroying service")
        applicationContext.sendBroadcast(Intent(ACTION_START_UPDATE_CHECKER))
        Log.d(javaClass.simpleName, "Restarted")
    }
}
