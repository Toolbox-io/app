package ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.intruderphoto

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import ru.morozovit.android.async
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.App.Companion.IP_FG_SERVICE_CHANNEL_ID
import ru.morozovit.ultimatesecurity.App.Companion.IP_FG_SERVICE_NOTIFICATION_ID
import ru.morozovit.ultimatesecurity.App.Companion.IP_PHOTO_TAKEN_CHANNEL_ID
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.services.DeviceAdmin.Companion.intruderPhotoNotification
import java.io.File

@MainThread
class IntruderPhotoService: Service() {
    companion object {
        private var filename: String? = null
        private var running = false
        private var instance: IntruderPhotoService? = null

        fun takePhoto(context: Context, name: String) {
            filename = name
            context.applicationContext.stopService(Intent(context, IntruderPhotoService::class.java))
            assert(instance == null && !running)
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context, IntruderPhotoService::class.java)
            )
        }
    }

    private fun run() {
        val handler = Handler(mainLooper)
        Log.d("IntruderPhoto", "Service started")
        try {
            if (filename != null) {
                Log.d("IntruderPhoto", "Filename not null")
                CameraController.getInstance(this).apply {
                    if (open()) {
                        Log.d("IntruderPhoto", "Camera opened successfully")
                        Thread.sleep(20)
                        takePicture(filename!!)
                        async {
                            @Suppress("ControlFlowWithEmptyBody")
                            while (getWaitingForImage()) {}
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

                                        val drawable = try {
                                            val stream = contentResolver.openInputStream(uri)
                                            val drawable = Drawable.createFromStream(stream, null)!!
                                            stream!!.close()
                                            drawable
                                        } catch (e: Exception) {
                                            null
                                        }
                                        intruderPhotoNotification =
                                            NotificationCompat.Builder(
                                                this@IntruderPhotoService,
                                                IP_PHOTO_TAKEN_CHANNEL_ID
                                            ).apply {
                                                setSmallIcon(R.drawable.priority_high)
                                                setContentTitle(resources.getString(R.string.i_f))
                                                setContentText(resources.getString(R.string.i_f_d))
                                                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                setContentIntent(pendingIntent)
                                                setAutoCancel(true)
                                                if (drawable != null) {
                                                    setLargeIcon(drawable.toBitmap())
                                                }
                                            }.build()
                                    }

                                    Log.d("IntruderPhoto", "Stopping!")
                                    stop()
                                }
                            }
                        }
                    } else {
                        Log.e("CameraController", "Error taking photo!")
                        runCatching {
                            close()
                        }
                        Log.d("IntruderPhoto", "Stopping!")
                        stop()
                    }
                }
            } else {
                Log.e("IntruderPhoto", "Filename is null!")
                Log.d("IntruderPhoto", "Stopping!")
                stop()
            }
        } catch (e: Exception) {
            Log.e("IntruderPhotoService", e.message ?: "Unknown error")
            Log.d("IntruderPhoto", "Stopping")
            stop()
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

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        instance = null
    }
}