package io.toolbox.ui.tools.notificationhistory

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.toolbox.App.Companion.context
import io.toolbox.R
import io.toolbox.ui.WindowInsetsHandler
import ru.morozovit.android.SerializableIntent
import ru.morozovit.android.invoke
import ru.morozovit.android.plus
import ru.morozovit.android.ui.ListItem
import ru.morozovit.utils.safeDelete
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.io.Serial
import java.io.Serializable

enum class ActionType {
    ACTIVITY,
    BROADCAST,
    SERVICE,
    FOREGROUND_SERVICE,
    UNKNOWN
}

data class ActionData(
    val label: String,
    val intent: SerializableIntent? = null,
    val type: ActionType
): Serializable {
    companion object {
        @Serial
        const val serialVersionUID = 23452368204683467L
    }
}

@OptIn(ExperimentalLayoutApi::class)
data class NotificationData(
    val title: String,
    val message: String,
    val onClickIntent: SerializableIntent? = null,
    val divider: Boolean = true,
    val actions: List<ActionData> = mutableListOf(),
    val bottomContent: (@Composable FlowRowScope.() -> Unit)? = {
        for ((label, _intent, type) in actions) {
            val intent = _intent?.toIntent()
            TextButton(
                onClick = {
                    if (intent == null) return@TextButton
                    when (type) {
                        ActionType.ACTIVITY -> context.startActivity(intent)
                        ActionType.BROADCAST -> context.sendBroadcast(intent)
                        ActionType.SERVICE -> context.startService(intent)
                        ActionType.FOREGROUND_SERVICE -> ContextCompat.startForegroundService(context, intent)
                        ActionType.UNKNOWN -> {}
                    }
                }
            ) {
                Text(label)
            }
        }
    },
    val sourcePackageName: String,
    @Transient
    val icon: Drawable? = null
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
    var file: File? = null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationHistoryScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        with(LocalContext()) {
            rememberCoroutineScope()
            Bundle().keySet()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.notification_history),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = navigation,
                        actions = {
                            // TODO add actions
                            actions()
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { innerPadding ->
                @Composable
                fun Notification(
                    modifier: Modifier = Modifier,
                    title: String,
                    message: String,
                    onClick: (() -> Unit)? = null,
                    divider: Boolean,
                    bottomContent: (@Composable FlowRowScope.() -> Unit)? = null,
                    visible: Boolean,
                    onVisibilityChange: ((Boolean) -> Unit),
                    sourcePackageName: String,
                    icon: Drawable? = null
                ) {
                    val appInfo = packageManager.getApplicationInfo(sourcePackageName, 0)
                    val appName = appInfo.loadLabel(packageManager).toString()

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            when (it) {
                                SwipeToDismissBoxValue.StartToEnd,
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onVisibilityChange(false)
                                }

                                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
                            }
                            return@rememberSwipeToDismissBoxState true
                        },
                        // positional threshold of 25%
                        positionalThreshold = { it * .5f }
                    )

                    AnimatedVisibility(
                        visible = visible,
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = modifier,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            when (dismissState.dismissDirection) {
                                                SwipeToDismissBoxValue.StartToEnd,
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.surface

                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            }
                                        )
                                        .padding(12.dp, 8.dp)
                                )
                            },
                            content = {
                                val computedAlpha = if (
                                    dismissState.targetValue == SwipeToDismissBoxValue.Settled &&
                                    dismissState.progress == 1f
                                ) 1f
                                else 1f - dismissState.progress

                                Column {
                                    Column(
                                        modifier = modifier
                                            .alpha(computedAlpha) +
                                                if (onClick != null)
                                                    Modifier.clickable(onClick = onClick)
                                                else
                                                    Modifier
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.primary,
                                                            RoundedCornerShape(50)
                                                        )
                                                        .size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (icon != null) {
                                                    Icon(
                                                        painter = rememberDrawablePainter(icon),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.QuestionMark,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp
                                            )
                                        }
                                        ListItem(
                                            headline = title,
                                            supportingText = message,
                                            leadingContent = {
                                                Spacer(Modifier.width(24.dp))
                                            },
                                            bottomContent = if (bottomContent != null) {
                                                {
                                                    FlowRow(
                                                        modifier = Modifier.padding(start = (24 + 16 - 12).dp, end = 16.dp),
                                                        content = bottomContent
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                    if (divider) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        )
                    }
                }
                val notifications = remember { mutableStateListOf<NotificationData>() }

                // --- TEST ---
                /*notifications += NotificationData(
                    title = "Notification 1",
                    message = "This is a notification message.",
                    sourcePackageName = "com.example.app1",
                    bottomContent = {
                        TextButton(onClick = {}) {
                            Text(text = "Action 1")
                        }
                        TextButton(onClick = {}) {
                            Text(text = "Action 2")
                        }
                    }
                )
                notifications += NotificationData(
                    title = "Notification 2",
                    message = "This is a notification message.",
                    sourcePackageName = "com.example.app1",
                    bottomContent = {
                        TextButton(onClick = {}) {
                            Text(text = "Action 1")
                        }
                        TextButton(onClick = {}) {
                            Text(text = "Action 2")
                        }
                    }
                )
                notifications += NotificationData(
                    title = "Notification 3",
                    message = "This is a notification message.",
                    sourcePackageName = "com.example.app1",
                    bottomContent = {
                        TextButton(onClick = {}) {
                            Text(text = "Action 1")
                        }
                        TextButton(onClick = {}) {
                            Text(text = "Action 2")
                        }
                    }
                )*/
                // --- TEST ---

                val notificationHistoryDir = File(filesDir, "notification_history")
                if (notificationHistoryDir.exists()) {
                    notificationHistoryDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                        try {
                            val inputStream = FileInputStream(file)
                            val objectInputStream = ObjectInputStream(inputStream)
                            val notificationData = objectInputStream.readObject() as NotificationData
                            notificationData.file = file
                            notifications += notificationData
                            objectInputStream.close()
                            inputStream.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                LazyColumn(contentPadding = innerPadding) {
                    items(notifications.size) { index ->
                        with (notifications[index]) {
                            Notification(
                                title = title,
                                message = message,
                                onClick = null,
                                divider = index != notifications.size - 1,
                                bottomContent = bottomContent,
                                visible = visible,
                                onVisibilityChange = {
                                    visible = it
                                    if (!visible) {
                                        file?.safeDelete()
                                    }
                                },
                                sourcePackageName = sourcePackageName
                            )
                        }
                    }
                }
            }
        }
    }
}