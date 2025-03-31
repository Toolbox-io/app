@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui.tools.notificationhistory

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import io.toolbox.App.Companion.context
import ru.morozovit.android.async
import ru.morozovit.android.runOrLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationDatabase {
    private var init = false
    private val _notifications = mutableListOf<NotificationData>()

    val notificationHistoryDir by lazy {
        File(context.filesDir, "notification_history").also { it.mkdir() }
    }

    private inline fun optimize() {
        val delete = mutableListOf<NotificationData>()

        _notifications.forEach action@ { n ->
            if (n in delete) return@action
            delete += _notifications.filter {
                // TODO filter useless messages by regex
                //      "^\s*(\d+\s+)?(new)?\s+messages?(\s+from\s+\d+\s+chats?)?\s*$"
                it !== n &&
                it.title == n.title &&
                it.message == n.message &&
                it.sourcePackageName == n.sourcePackageName
            }
        }
        delete.forEach {
            _notifications -= it
        }
    }

    private fun init() {
        if (!init) {
            load()
            optimize()
            init = true
        }
    }

    private inline fun load() {
        with (context) {
            val notificationHistoryDir = File(filesDir, "notification_history")
            if (notificationHistoryDir.exists()) {
                val files = notificationHistoryDir.listFiles()
                val needsPair = mutableMapOf<String, NotificationData>()

                val processLater = mutableListOf<File>()

                fun callback(file: File) {
                    try {
                        if (file.nameWithoutExtension.substringBeforeLast(".") in needsPair.keys && file.extension == "png") {
                            needsPair[file.nameWithoutExtension.substringBeforeLast(".")]!!.notificationIconFile = file
                            needsPair.remove(file.nameWithoutExtension.substringBeforeLast("."))
                            return
                        }
                        if (file.extension != "notification") {
                            if (file !in processLater) processLater += file
                            return
                        }
                        val inputStream = FileInputStream(file)
                        val objectInputStream = ObjectInputStream(inputStream)
                        val notificationData = objectInputStream.readObject() as NotificationData
                        notificationData.notificationFile = file
                        _notifications += notificationData
                        needsPair[file.nameWithoutExtension] = notificationData
                        objectInputStream.close()
                        inputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                files?.sortedBy { it.name }?.forEach(::callback)
                processLater.forEach(::callback)
            }
        }
    }

    val list: List<NotificationData> get() {
        init()
        return _notifications.toList()
    }

    fun add(notification: NotificationData) {
        with (context) {
            with (notification) {
                init()

                // Check if the notification with the same date already exists
                if (
                    _notifications.find {
                        it.title == title &&
                        it.message == message &&
                        it.sourcePackageName == sourcePackageName
                    } != null
                ) return

                _notifications += notification

                // Serialize to disk
                async {
                    val formattedDate = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
                    val time = currentTimeMillis()
                    val notificationFile = File(notificationHistoryDir, "$formattedDate:$time.notification")
                    val notificationIconFile = File(notificationHistoryDir, "$formattedDate:$time.notification_icon.png")
                    if (icon != null) {
                        val fos = FileOutputStream(notificationIconFile)
                        try {
                            icon!!.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)
                        } catch (e: Exception) {
                            Log.e("NotificationService", "Error saving notification icon:", e)
                        } finally {
                            fos.close()
                        }
                    }

                    this.notificationFile = notificationFile
                    this.notificationIconFile = notificationIconFile

                    runOrLog("NotificationService") {
                        FileOutputStream(notificationFile).use { fos ->
                            ObjectOutputStream(fos).use {
                                it.writeObject(notification)
                            }
                        }
                        Log.d("NotificationService", "Notification data saved to ${notificationFile.absolutePath}")
                    }
                }
            }
        }
    }

    fun remove(notification: NotificationData) {
        with (context) {
            init()
            _notifications -= notification
            notification.notificationFile?.delete()
        }
    }

    operator fun contains(notification: NotificationData) = notification in _notifications

    inline operator fun plusAssign(notification: NotificationData) = add(notification)
    inline operator fun minusAssign(notification: NotificationData) = remove(notification)
}