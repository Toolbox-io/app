package io.toolbox.ui.tools.notificationhistory

import android.graphics.drawable.Drawable
import androidx.annotation.WorkerThread
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileInputStream
import java.io.Serial
import java.io.Serializable

@OptIn(ExperimentalLayoutApi::class)
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
    var _visible: MutableState<Boolean>? = mutableStateOf(true)

    var visible: Boolean
        get() {
            if (_visible == null) {
                _visible = mutableStateOf(true)
            }
            return _visible!!.value
        }
        set(value) {
            if (_visible == null) {
                _visible = mutableStateOf(true)
            }
            _visible!!.value = value
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

    val date: String? get() = notificationFile?.nameWithoutExtension?.split(":")[0]
    @Suppress("unused")
    val time: String? get() = notificationFile?.nameWithoutExtension?.split(":")[1]

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is NotificationData) return false
        return title == other.title &&
            message == other.message &&
            sourcePackageName == other.sourcePackageName &&
            icon == other.icon
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + sourcePackageName.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (_visible?.hashCode() ?: 0)
        result = 31 * result + (notificationFile?.hashCode() ?: 0)
        result = 31 * result + (notificationIconFile?.hashCode() ?: 0)
        result = 31 * result + visible.hashCode()
        return result
    }
}