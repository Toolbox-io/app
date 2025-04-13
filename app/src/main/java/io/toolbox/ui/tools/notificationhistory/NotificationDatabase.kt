@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui.tools.notificationhistory

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import io.toolbox.App.Companion.context
import io.toolbox.Settings
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

object NotificationDatabase: MutableList<NotificationData> {
    private var init = false
    private val _notifications = mutableListOf<NotificationData>()

    private val notificationHistoryDir by lazy {
        File(context.filesDir, "notification_history").also { it.mkdir() }
    }

    fun optimize() {
        val delete = mutableListOf<NotificationData>()

        _notifications.forEach action@ { n ->
            if (n in delete) return@action
            delete += _notifications.filter {
                if (Settings.NotificationHistory.removeDuplicates) {
                    return@filter (
                        it !== n &&
                        it.title == n.title &&
                        it.message == n.message &&
                        it.sourcePackageName == n.sourcePackageName
                    )
                }
                if (Settings.NotificationHistory.removeUselessNotifications) {
                    return@filter (
                        "^\\s*(\\d+\\s+)?(new)?\\s+messages?(\\s+from\\s+\\d+\\s+chats?)?\\s*$"
                            .toRegex()
                            .matches(it.message)
                    )
                }
                true
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

                val callback = callback@ { file: File ->
                    try {
                        if (file.nameWithoutExtension.substringBeforeLast(".") in needsPair.keys && file.extension == "png") {
                            needsPair[file.nameWithoutExtension.substringBeforeLast(".")]!!.notificationIconFile = file
                            needsPair.remove(file.nameWithoutExtension.substringBeforeLast("."))
                            return@callback
                        }
                        if (file.extension != "notification") {
                            if (file !in processLater) processLater += file
                            return@callback
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

                files?.sortedBy { it.name }?.forEach(callback)
                processLater.forEach(callback)
            }
        }
    }

    val list: List<NotificationData> get() {
        init()
        return _notifications.toList()
    }

    override fun add(element: NotificationData): Boolean {
        with (element) {
            init()

            // Check if the notification with the same date already exists
            if (
                _notifications.find {
                    it.title == title &&
                    it.message == message &&
                    it.sourcePackageName == sourcePackageName
                } != null
            ) return false

            _notifications += element

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
                            it.writeObject(element)
                        }
                    }
                    Log.d("NotificationService", "Notification data saved to ${notificationFile.absolutePath}")
                }
            }
        }
        return true
    }

    @Suppress("SameReturnValue")
    override fun remove(element: NotificationData): Boolean {
        init()
        _notifications -= element
        element.notificationFile?.delete()
        return true
    }

    override operator fun contains(element: NotificationData) = element in _notifications

    inline operator fun plusAssign(notification: NotificationData) {
        add(notification)
    }
    inline operator fun minusAssign(notification: NotificationData) {
        remove(notification)
    }

    override val size: Int get() = _notifications.size

    override fun clear() {
        forEach {
            remove(it)
        }
    }

    override fun get(index: Int) = _notifications[index]

    override fun isEmpty() = _notifications.isEmpty()

    override fun iterator() = _notifications.iterator()

    override fun listIterator() = _notifications.listIterator()

    override fun listIterator(index: Int) = _notifications.listIterator(index)

    override fun removeAt(index: Int): NotificationData {
        val item = get(index)
        remove(item)
        return item
    }

    override fun subList(fromIndex: Int, toIndex: Int) = _notifications.subList(fromIndex, toIndex)

    override fun set(index: Int, element: NotificationData) = throw UnsupportedOperationException()

    override fun retainAll(elements: Collection<NotificationData>): Boolean {
        var removed = false
        forEach {
            if (it !in elements) {
                val result = remove(it)
                if (!removed) removed = result
            }
        }
        return removed
    }

    override fun removeAll(elements: Collection<NotificationData>): Boolean {
        var removed = false
        forEach {
            if (it in elements) {
                val result = remove(it)
                if (!removed) removed = result
            }
        }
        return removed
    }

    override fun lastIndexOf(element: NotificationData) = _notifications.lastIndexOf(element)

    override fun indexOf(element: NotificationData) = _notifications.indexOf(element)

    override fun containsAll(elements: Collection<NotificationData>) = _notifications.containsAll(elements)

    override fun addAll(index: Int, elements: Collection<NotificationData>) = throw UnsupportedOperationException()

    override fun addAll(elements: Collection<NotificationData>): Boolean {
        var changed = false
        elements.forEach {
            val result = add(it)
            if (!changed) changed = result
        }
        return changed
    }

    override fun add(index: Int, element: NotificationData) = throw UnsupportedOperationException()
}