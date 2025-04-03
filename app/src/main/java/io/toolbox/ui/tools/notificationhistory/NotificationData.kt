package io.toolbox.ui.tools.notificationhistory

import android.graphics.drawable.Drawable
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileInputStream
import java.io.Serial
import java.io.Serializable

@Suppress("EqualsOrHashCode")
data class NotificationData(
    val title: String,
    val message: String,
    val sourcePackageName: String,
    @Transient
    var icon: Drawable? = null
): Serializable {
    companion object {
        @Serial
        const val serialVersionUID = 23592935634587396L
    }

    @Transient
    var notificationFile: File? = null
        set(value) {
            field = value
            if (value != null) {
                notificationIconFile = File(value.parent, "${value.nameWithoutExtension}.notificationIcon.png")
            }
        }
    @Transient
    var notificationIconFile: File? = null
        @WorkerThread
        set(value) {
            field = value
            if (value != null) {
                runCatching {
                    icon = Drawable.createFromStream(
                        FileInputStream(notificationIconFile!!),
                        notificationIconFile!!.name
                    )
                }
            }
        }

    @Transient
    var onVisibilityChange: ((Boolean) -> Unit)? = null

    val time: String? get() = notificationFile?.nameWithoutExtension?.split(":")?.get(1)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is NotificationData) return false
        return title == other.title &&
            message == other.message &&
            sourcePackageName == other.sourcePackageName &&
            icon == other.icon
    }
}