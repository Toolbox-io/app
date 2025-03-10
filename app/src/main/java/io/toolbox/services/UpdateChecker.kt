@file:Suppress("DEPRECATION")

package io.toolbox.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REPLACED
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.content.FileProvider
import androidx.core.os.postDelayed
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import io.toolbox.App
import io.toolbox.App.Companion.GITHUB_API_VERSION
import io.toolbox.App.Companion.GITHUB_TOKEN
import io.toolbox.App.Companion.UPDATE_CHANNEL_ID
import io.toolbox.App.Companion.UPDATE_NOTIFICATION_ID
import io.toolbox.App.Companion.context
import io.toolbox.App.Companion.githubRateLimitRemaining
import io.toolbox.R
import io.toolbox.ui.MainActivity
import ru.morozovit.android.JobIdManager
import ru.morozovit.android.NoParallelExecutor
import ru.morozovit.android.SimpleAsyncTask
import ru.morozovit.android.notifyIfAllowed
import ru.morozovit.android.ui.DialogActivity
import ru.morozovit.utils.EParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class UpdateChecker: JobService() {
    companion object {
        private var prevInfo: UpdateInfo? = null
        private var scheduled = false

        fun schedule(context: Context) {
            Log.d("UpdateChecker", "schedule: adding job to scheduler")
            (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler)
                .schedule(
                    JobInfo.Builder(
                        JobIdManager.getJobId(
                            JobIdManager.JOB_TYPE_CHANNEL_PROGRAMS,
                            2
                        ),
                        ComponentName(context, UpdateChecker::class.java)
                    ).apply {
                        if (scheduled) setMinimumLatency(60 * 1000)
                        setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                        setRequiresDeviceIdle(false)
                        setRequiresCharging(false)
                        setBackoffCriteria(
                            TimeUnit.SECONDS.toMillis(60),
                            JobInfo.BACKOFF_POLICY_LINEAR
                        )
                    }.build()
                )
            Log.d("UpdateChecker", "schedule: job scheduled")
            scheduled = true
        }

        private val TASK_EXECUTOR = NoParallelExecutor()

        const val ACTION_START_UPDATE_CHECKER = "io.toolbox.UpdateChecker.START"

        const val DOWNLOAD_BROADCAST = "UpdateChecker.DOWNLOAD"

        data class SemanticVersion(
            val major: Int = 0,
            val minor: Int = 0,
            val patch: Int = 0
        ) : Serializable {
            override fun equals(other: Any?): Boolean {
                return (other is SemanticVersion &&
                        major == other.major &&
                        minor == other.minor &&
                        patch == other.patch) ||
                        (other is Number && other.toInt() == toString().toInt())
            }
            override fun toString(): String {
                return StringBuilder().apply {
                    if (major > 0) append(major)
                    if (minor > 0) append(".$minor")
                    if (patch > 0) append(".$patch")
                }.toString()
            }
            override fun hashCode(): Int {
                var result = major.hashCode()
                result = 31 * result + minor.hashCode()
                result = 31 * result + patch.hashCode()
                return result
            }
        }
        data class UpdateInfo(
            val available: Boolean,
            val version: SemanticVersion,
            val description: String,
            val download: String
        ) : Serializable {
            override fun toString(): String {
                return "UpdateInfo {available = $available, version = $version, description = " +
                        "\"$description\", download = $download}"
            }
            override fun equals(other: Any?): Boolean {
                return other is UpdateInfo &&
                        available == other.available &&
                        version == other.version &&
                        description == other.description &&
                        download == other.download
            }
            override fun hashCode(): Int {
                var result = available.hashCode()
                result = 31 * result + version.hashCode()
                result = 31 * result + description.hashCode()
                result = 31 * result + download.hashCode()
                return result
            }
        }

        fun checkForUpdates(): UpdateInfo? {
            if (githubRateLimitRemaining < 10 && githubRateLimitRemaining != -1L) {
                Log.d("UpdateChecker", "Rate limit almost exceeded.")
                return null
            }
            with (context) {
                Log.d("UpdateChecker", "Checking for updates")
                val request = URL("https://api.github.com/repos/Toolbox-io/Toolbox-io/releases")
                    .openConnection() as HttpsURLConnection
                request.requestMethod = "GET";
                request.setRequestProperty("Accept", "application/vnd.github+json")
                request.setRequestProperty("X-Github-Api-Version", GITHUB_API_VERSION)
                request.setRequestProperty("Authorization", "Bearer ${GITHUB_TOKEN}")

                try {
                    request.connect()
                    if (request.responseCode == HttpsURLConnection.HTTP_OK) {
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
                        @Suppress("UNUSED_VALUE")
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
                            asset
                        )
                    } else {
                        Log.d("UpdateChecker", "Error. HTTP response code: ${request.responseCode}")
                        val errorInput = request.errorStream!!
                        var c: Char;

                        val chars: MutableList<Char> = mutableListOf()

                        while (true) {
                            c = errorInput.read().toChar()
                            if (c == 0.toChar() || c == '\uFFFF') break;
                            chars.add(c)
                        }
                        val response = String(chars.toCharArray())
                        Log.d("UpdateChecker", "Error response: $response")
                        return null
                    }
                } catch (e: Exception) {
                    Log.d("UpdateChecker", "Error. \n${EParser(e)}")
                    return null
                } finally {
                    runCatching {
                        githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
                    }
                    request.disconnect()
                }
            }
        }

        class DownloadBroadcastReceiver: BroadcastReceiver() {
            companion object {
                private val TASK_EXECUTOR = NoParallelExecutor()
            }

            @Suppress("OVERRIDE_DEPRECATION")
            @SuppressLint("StaticFieldLeak")
            inner class Task: AsyncTask<String, String, Unit>() {
                private lateinit var file: File
                private lateinit var mime: String
                private lateinit var builder: NotificationCompat.Builder

                @SuppressLint("MissingPermission")
                override fun onPreExecute() {
                    super.onPreExecute()
                    builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.primitive_icon)
                        .setContentTitle("Downloading update...")
                        .setPriority(PRIORITY_DEFAULT)
                        .setSilent(true)
                        .setOngoing(false)
                    context.notifyIfAllowed(
                        UPDATE_NOTIFICATION_ID,
                        builder.build()
                    )
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
                        file = File(context.cacheDir.absolutePath + "/update.apk")
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
                    context.notifyIfAllowed(
                        UPDATE_NOTIFICATION_ID, builder.build()
                    )
                }

                @SuppressLint("MissingPermission")
                override fun onPostExecute(result: Unit?) {
                    with (context) {
                        val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        install.data = FileProvider.getUriForFile(
                            this,
                            context.packageName + ".provider",
                            file
                        )

                        val dialog = DialogActivity.getLaunchIntent(
                            context = context,
                            title = resources.getString(R.string.install_package),
                            body = resources.getString(R.string.install_package_d),
                            positiveButtonText = resources.getString(R.string.install),
                            negativeButtonText = resources.getString(R.string.cancel),
                            positiveButtonOnClick = {
                                startActivity(install)
                            },
                            negativeButtonOnClick = {}
                        )

                        val pendingIntent = PendingIntent.getActivity(this, 0, dialog, FLAG_IMMUTABLE)

                        builder.setContentTitle("Update is ready")
                            .setContentText("Tap to install")
                            .setProgress(0, 0, false)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setSilent(false)
                            .setOngoing(false)

                        Handler(Looper.getMainLooper()).postDelayed(1000) {
                            notifyIfAllowed(
                                UPDATE_NOTIFICATION_ID, builder.build()
                            )
                        }
                    }
                }
            }

            override fun onReceive(context: Context, intent: Intent) {
                Task().executeOnExecutor(
                    TASK_EXECUTOR,
                    (intent.getSerializableExtra("updateInfo") as UpdateInfo).download
                )
            }
        }

        class Receiver: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (
                    intent.action == ACTION_BOOT_COMPLETED ||
                    intent.action == ACTION_START_UPDATE_CHECKER ||
                    intent.action == ACTION_PACKAGE_REPLACED ||
                    intent.action == ACTION_PACKAGE_ADDED
                ) {
                    schedule(context)
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class Task: SimpleAsyncTask() {
        override fun run() {
            val info = checkForUpdates()
            if (info != null && info.available && info != prevInfo) {
                val text =
                    "${info.version.major}." +
                    "${info.version.minor}." +
                    "${info.version.patch}"

                // Notification
                val downloadIntent =
                    Intent(context, DownloadBroadcastReceiver::class.java)
                        .apply {
                            action = DOWNLOAD_BROADCAST
                            putExtra("updateInfo", info)
                        }
                val downloadPendingIntent = PendingIntent.getBroadcast(
                    context, 0, downloadIntent,
                    PendingIntent.FLAG_MUTABLE
                )
                val mainIntent = Intent(context, MainActivity::class.java)

                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent,
                    FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(
                    context,
                    UPDATE_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.primitive_icon)
                    .setContentTitle("Update available")
                    .setContentText("Version $text")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(info.description))
                    .setPriority(PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .addAction(R.drawable.primitive_icon, "Download", downloadPendingIntent)
                    .setAutoCancel(true)


                // notificationId is a unique int for each notification that you must define.
                notifyIfAllowed(UPDATE_NOTIFICATION_ID, builder.build())
                prevInfo = info
            } else {
                Log.d(
                    "UpdateChecker",
                    "Conditions are not met. (info != null) = ${info != null}," +
                        " (info.available) = ${info?.available}, (info != prevInfo) = ${info !=
                                prevInfo
                        } "
                )
            }
        }

        override fun postRun() {
            schedule(context)
            stopSelf()
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        @Suppress("DEPRECATION")
        Task().executeOnExecutor(TASK_EXECUTOR)
        return true
    }

    override fun onStopJob(params: JobParameters?) = true
}