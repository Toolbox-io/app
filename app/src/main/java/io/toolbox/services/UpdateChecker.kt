@file:Suppress("DEPRECATION", "NOTHING_TO_INLINE")

package io.toolbox.services

import android.annotation.SuppressLint
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
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.toolbox.App.Companion.GITHUB_API_VERSION
import io.toolbox.App.Companion.GITHUB_TOKEN
import io.toolbox.App.Companion.UPDATE_CHANNEL_ID
import io.toolbox.App.Companion.UPDATE_NOTIFICATION_ID
import io.toolbox.App.Companion.context
import io.toolbox.App.Companion.githubRateLimitRemaining
import io.toolbox.R
import io.toolbox.api.DefaultHTTPClient
import io.toolbox.ui.MainActivity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.morozovit.android.utils.JobIdManager
import ru.morozovit.android.utils.SimpleAsyncTask
import ru.morozovit.android.utils.ThreadExecutor
import ru.morozovit.android.utils.change
import ru.morozovit.android.utils.fillTo
import ru.morozovit.android.utils.getSerializableExtraAs
import ru.morozovit.android.utils.notifyIfAllowed
import ru.morozovit.android.utils.pendingIntent
import ru.morozovit.android.utils.recreate
import ru.morozovit.android.utils.runOrLog
import ru.morozovit.android.utils.ui.DialogActivity
import ru.morozovit.utils.EParser
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

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

        private val TASK_EXECUTOR = ThreadExecutor()
        private val client by lazy { DefaultHTTPClient() }

        const val ACTION_START_UPDATE_CHECKER = "io.toolbox.UpdateChecker.START"
        const val DOWNLOAD_BROADCAST = "UpdateChecker.DOWNLOAD"

        data class SemanticVersion(
            val major: Int = 0,
            val minor: Int = 0,
            val patch: Int = 0
        ): java.io.Serializable {
            override fun equals(other: Any?) = other != null && (
                (
                    other is SemanticVersion &&
                    major == other.major &&
                    minor == other.minor &&
                    patch == other.patch
                ) || (
                    other is Number &&
                    other.toInt() == toString().toInt()
                ) || (
                    other.toString() == toString()
                )
            )

            operator fun compareTo(other: SemanticVersion): Int {
                val majorBigger = major.compareTo(other.major)
                val minorBigger = minor.compareTo(other.minor)
                val patchBigger = patch.compareTo(other.patch)

                return majorBigger.change { maj ->
                    (
                        minorBigger.change { min ->
                            patchBigger.takeIf { min == 0 }
                        }
                    ).takeIf { maj == 0 }
                }
            }

            override fun toString() = buildString {
                if (major > 0) append(major)
                if (minor > 0) append(".$minor")
                if (patch > 0) append(".$patch")
            }

            override fun hashCode(): Int {
                var result = major.hashCode()
                result = 31 * result + minor.hashCode()
                result = 31 * result + patch.hashCode()
                return result
            }
        }

        inline fun SemanticVersion(string: String?) = if (string.isNullOrBlank()) {
            SemanticVersion()
        } else {
            val (maj, min, pat) = string
                .split(".")
                .map { it.toInt() }
                .fillTo(3) { 0 }
            SemanticVersion(maj, min, pat)
        }

        data class UpdateInfo(
            val available: Boolean,
            val version: SemanticVersion,
            val description: String,
            val download: String
        ): java.io.Serializable

        @Serializable
        private data class GithubAsset(
            @SerialName("browser_download_url") val browserDownloadUrl: String
        )

        @Serializable
        private data class GithubRelease(
            val name: String,
            val body: String,
            val assets: List<GithubAsset>
        )

        suspend fun checkForUpdates(): UpdateInfo? {
            if (githubRateLimitRemaining < 10 && githubRateLimitRemaining != -1L) {
                Log.d("UpdateChecker", "Rate limit almost exceeded.")
                return null
            }
            try {
                Log.d("UpdateChecker", "Checking for updates (Ktor)")
                val response = client.get("https://api.github.com/repos/Toolbox-io/Toolbox-io/releases") {
                    header("Accept", "application/vnd.github+json")
                    header("X-Github-Api-Version", GITHUB_API_VERSION)
                    header("Authorization", "Bearer $GITHUB_TOKEN")
                }

                githubRateLimitRemaining = githubRateLimitRemaining.change { response.headers["x-ratelimit-remaining"]?.toLongOrNull() }
                if (response.status.value != 200) {
                    Log.d("UpdateChecker", "Error. HTTP response code: ${response.status.value}")
                    Log.d("UpdateChecker", "Error response: ${response.bodyAsText()}")
                    return null
                }
                val latestRelease = response.body<List<GithubRelease>>().firstOrNull() ?: return null

                val latestVersion = SemanticVersion(latestRelease.name.substring(1))
                val currentVersion = SemanticVersion(
                    context
                        .packageManager
                        .getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
                        .versionName
                )

                return UpdateInfo(
                    latestVersion > currentVersion,
                    latestVersion,
                    latestRelease.body,
                    latestRelease.assets.firstOrNull()?.browserDownloadUrl ?: return null
                )
            } catch (e: Exception) {
                Log.d("UpdateChecker", "Error. \n${EParser(e)}")
                return null
            }
        }

        class DownloadBroadcastReceiver: BroadcastReceiver() {
            companion object {
                private val TASK_EXECUTOR = ThreadExecutor()
            }

            @Suppress("OVERRIDE_DEPRECATION")
            @SuppressLint("StaticFieldLeak")
            inner class DownloadTask: AsyncTask<String, String, Unit>() {
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

                override fun doInBackground(vararg params: String) {
                    var count: Int
                    runOrLog("UpdateChecker") {
                        URL(params[0]).let {
                            val connection = it.openConnection().also {
                                it.connect()
                                mime = it.contentType
                            }

                            // this will be useful so that you can show a tipical 0-100%
                            // progress bar
                            val lengthOfFile = connection.contentLength

                            // download the file
                            it.openStream().buffered().use { input ->
                                // Output stream
                                file = File("${context.cacheDir.absolutePath}/update.apk").apply {
                                    recreate()
                                    outputStream().use { output ->
                                        val data = ByteArray(1024)
                                        var total = 0L

                                        while ((input.read(data).also { count = it }) != -1) {
                                            total += count.toLong()
                                            // publishing the progress....
                                            // After this onProgressUpdate will be called
                                            publishProgress("" + ((total * 100) / lengthOfFile).toInt())

                                            // writing data to file
                                            output.write(data, 0, count)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onProgressUpdate(vararg progress: String) {
                    // setting progress percentage
                    builder
                        .setProgress(100, progress[0].toInt(), false)
                        .setSilent(true)
                        .setOngoing(true)
                    context.notifyIfAllowed(UPDATE_NOTIFICATION_ID, builder.build())
                }

                @SuppressLint("MissingPermission")
                override fun onPostExecute(result: Unit) {
                    with (context) context@ {
                        val dialog = DialogActivity.getLaunchIntent(
                            context = context,
                            title = resources.getString(R.string.install_package),
                            body = resources.getString(R.string.install_package_d),
                            positiveButtonText = resources.getString(R.string.install),
                            negativeButtonText = resources.getString(R.string.cancel),
                            positiveButtonOnClick = {
                                startActivity(
                                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                        addFlags(
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        )
                                        data = FileProvider.getUriForFile(
                                            this@context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                    }
                                )
                            },
                            negativeButtonOnClick = {}
                        )

                        builder.setContentTitle("Update is ready")
                            .setContentText("Tap to install")
                            .setProgress(0, 0, false)
                            .setContentIntent(pendingIntent(dialog, activity = true))
                            .setAutoCancel(true)
                            .setSilent(false)
                            .setOngoing(false)

                        Handler(Looper.getMainLooper()).postDelayed(1000) {
                            notifyIfAllowed(UPDATE_NOTIFICATION_ID, builder.build())
                        }
                    }
                }
            }

            override fun onReceive(context: Context, intent: Intent) {
                DownloadTask().executeOnExecutor(
                    TASK_EXECUTOR,
                    intent.getSerializableExtraAs<UpdateInfo>("updateInfo")!!.download
                )
            }
        }

        class Receiver: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (
                    intent.action in arrayOf(
                        ACTION_BOOT_COMPLETED,
                        ACTION_START_UPDATE_CHECKER,
                        ACTION_PACKAGE_REPLACED,
                        ACTION_PACKAGE_ADDED
                    )
                ) schedule(context)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class Task: SimpleAsyncTask() {
        override fun run() {
            val info = runBlocking { checkForUpdates() }
            if (info != null && info.available && info != prevInfo) {
                // Notification
                notifyIfAllowed(
                    UPDATE_NOTIFICATION_ID,
                    NotificationCompat.Builder(
                        context,
                        UPDATE_CHANNEL_ID
                    )
                        .setSmallIcon(R.drawable.primitive_icon)
                        .setContentTitle("Update available")
                        .setContentText("Version ${info.version}")
                        .setStyle(NotificationCompat.BigTextStyle().bigText(info.description))
                        .setPriority(PRIORITY_DEFAULT)
                        .setContentIntent(
                            pendingIntent(
                                activity = true,
                                intent = Intent(context, MainActivity::class.java)
                            )
                        )
                        .addAction(
                            R.drawable.primitive_icon,
                            "Download",
                            pendingIntent(
                                Intent(context, DownloadBroadcastReceiver::class.java).apply {
                                    action = DOWNLOAD_BROADCAST
                                    putExtra("updateInfo", info)
                                }
                            )
                        )
                        .setAutoCancel(true)
                        .build()
                )
                prevInfo = info
            } else {
                Log.d(
                    "UpdateChecker",
                    "Conditions are not met. (info != null) = ${info != null}," +
                    " (info.available) = ${info?.available}, (info != prevInfo) = ${info != prevInfo} "
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