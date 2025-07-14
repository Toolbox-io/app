package io.toolbox.ui.protection.actions.intruderphoto

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import io.toolbox.App
import io.toolbox.App.Companion.IP_FG_SERVICE_CHANNEL_ID
import io.toolbox.App.Companion.IP_FG_SERVICE_NOTIFICATION_ID
import io.toolbox.App.Companion.IP_PHOTO_TAKEN_CHANNEL_ID
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.CameraController
import io.toolbox.services.DeviceAdmin.Companion.intruderPhotoNotifications
import ru.morozovit.android.utils.isScreenLocked
import ru.morozovit.android.utils.notifyIfAllowed
import ru.morozovit.android.utils.waitWhile
import java.io.File
import kotlin.concurrent.thread

class IntruderPhotoService: android.app.Service() {
    companion object {
        private var filename: String? = null
        private var running = false
        private var instance: IntruderPhotoService? = null
            set(value) {
                field = value
                if (value == null) {
                    nullCallback?.invoke()
                }
            }
        private var nullCallback: (() -> Unit)? = null

        private var completionCallback: (() -> Unit)? = null

        fun takePhoto(context: Context, name: String, onCompletion: (() -> Unit)? = null) {
            filename = name
            runCatching {
                context.stopService(Intent(context, IntruderPhotoService::class.java))
                instance!!.stopSelf()
            }

            fun callback() {
                assert(instance == null && !running)
                completionCallback = onCompletion
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, IntruderPhotoService::class.java)
                )
            }

            if (instance != null || running) {
                nullCallback = ::callback
            } else {
                callback()
            }
        }
    }

    private fun run() {
        val handler = Handler(mainLooper)
        Log.d("IntruderPhoto", "Service started")
        try {
            if (filename != null) {
                Log.d("IntruderPhoto", "Filename not null")
                CameraController(this).apply {
                    if (open()) {
                        Log.d("IntruderPhoto", "Camera opened successfully")
                        Thread.sleep(20)
                        takePicture(filename!!)
                        thread {
                            waitWhile(2000) { waitingForImage }
                            Log.d("IntruderPhoto", "Picture taken!")
                            handler.post {
                                try {
                                    Log.d("IntruderPhoto", "Trying to close the camera...")
                                    close()
                                    Log.d("IntruderPhoto", "Camera closed successfully")
                                } catch (_: Exception) {
                                } finally {
                                    if (Settings.UnlockProtection.IntruderPhoto.nopt) {
                                        val file = File(App.context.filesDir.absolutePath + "/front/$filename.jpg")
                                        val uri = FileProvider.getUriForFile(
                                            App.context,
                                            applicationContext.packageName + ".provider",
                                            file
                                        )

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "image/*")
                                            flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        val pendingIntent = PendingIntent.getActivity(
                                            App.context,
                                            0,
                                            intent,
                                            PendingIntent.FLAG_IMMUTABLE
                                        )

                                        val drawable = runCatching {
                                            val stream = contentResolver.openInputStream(uri)
                                            val drawable = Drawable.createFromStream(stream, null)!!
                                            stream!!.close()
                                            drawable
                                        }.getOrNull()

                                        val notification =
                                            NotificationCompat.Builder(
                                                this@IntruderPhotoService,
                                                IP_PHOTO_TAKEN_CHANNEL_ID
                                            ).apply {
                                                setSmallIcon(R.drawable.priority_high)
                                                setContentTitle(resources.getString(R.string.i_f))
                                                setContentText(resources.getString(R.string.i_f_d))
                                                setPriority(NotificationCompat.PRIORITY_MAX)
                                                setContentIntent(pendingIntent)
                                                setAutoCancel(true)
                                                if (drawable != null) {
                                                    setLargeIcon(drawable.toBitmap())
                                                }
                                            }.build()
                                        if (isScreenLocked) {
                                            intruderPhotoNotifications += notification
                                        } else {
                                            notifyIfAllowed(
                                                IP_FG_SERVICE_NOTIFICATION_ID,
                                                notification
                                            )
                                        }
                                    }

                                    Log.d("IntruderPhoto", "Stopping!")
                                    stopSelf()
                                }
                            }
                        }
                    } else {
                        Log.e("CameraController", "Error taking photo!")
                        runCatching {
                            close()
                        }
                        Log.d("IntruderPhoto", "Stopping!")
                        stopSelf()
                    }
                }
            } else {
                Log.e("IntruderPhoto", "Filename is null!")
                Log.d("IntruderPhoto", "Stopping!")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("IntruderPhotoService", e.message ?: "Unknown error")
            Log.d("IntruderPhoto", "Stopping")
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        instance = this
        try {
            startForeground(IP_FG_SERVICE_NOTIFICATION_ID, getNotification())
        } catch (e: Exception) {
            Log.e("IntruderPhotoService", e.message ?: "Unknown error")
            stopSelf()
        }
        run()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(App.context, IP_FG_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.primitive_icon)
            .setContentTitle("")
            .setPriority(PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        running = false
        instance = null
        completionCallback?.invoke()
        completionCallback = null
    }
}