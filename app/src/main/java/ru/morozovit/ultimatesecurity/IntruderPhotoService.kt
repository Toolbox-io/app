package ru.morozovit.ultimatesecurity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import org.jetbrains.annotations.Range
import ru.morozovit.ultimatesecurity.unlockprotection.intruderphoto.CameraController

class IntruderPhotoService: Service() {
    companion object {
        const val FG_SERVICE_CHANNEL_ID = "fgservice"
        const val FG_SERVICE_NOTIFICATION_ID = 2
        
        const val FRONT_CAM = 1
        const val BACK_CAM = 2
        const val BOTH_CAMS = 3
        private var filename: String? = null
        private var running = false
        private var instance: IntruderPhotoService? = null
        
        fun takePhoto(context: Context, name: String, cam: @Range(from = FRONT_CAM.toLong(), to = BOTH_CAMS.toLong()) Int) {
            filename = name
            if (!running) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, IntruderPhotoService::class.java).apply {
                        putExtra("cam", cam)
                    }
                )
            } else {
                instance?.run(null)
            }
        }
    }

    private fun run(intent: Intent?) {
        try {
            if (filename != null) {
                CameraController.getInstance(this).apply {
                    val cam = (intent ?: Intent()).getIntExtra("cam", FRONT_CAM)
                    if (open(if (cam == BOTH_CAMS) FRONT_CAM else cam)) {
                        Thread.sleep(20)
                        takePicture(filename!!)
                        if (cam == BOTH_CAMS) {
                            Thread.sleep(100)
                            if (open(BACK_CAM)) {
                                Thread.sleep(20)
                                @Suppress("ControlFlowWithEmptyBody")
                                Thread {
                                    try {
                                        while (getWaitingForImage()) {}
                                        Handler(mainLooper).post {
                                            takePicture(filename!!)
                                            close()
                                            stop()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("IntruderPhotoService", e.message?: "Unknown error")
                                        close()
                                    }
                                }.start()
                            } else {
                                Log.e("CameraController", "Error taking photo!")
                            }
                        }
                        else {
                            close()
                        }
                    } else {
                        Log.e("CameraController", "Error taking photo!")
                        close()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IntruderPhotoService", e.message ?: "Unknown error")
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        instance = this
        try {
            startForeground(FG_SERVICE_NOTIFICATION_ID, getNotification())
        } catch (e: Exception) {
            Log.e("IntruderPhotoService", e.message ?: "Unknown error")
            stopSelf()
        }
        run(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Foreground service"
            val descriptionText = "Blank notification, required for Intruder Photo"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                FG_SERVICE_CHANNEL_ID,
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
        return NotificationCompat.Builder(Settings.applicationContext, FG_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
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