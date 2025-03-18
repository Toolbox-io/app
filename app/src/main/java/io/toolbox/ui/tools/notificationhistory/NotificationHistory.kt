package io.toolbox.ui.tools.notificationhistory

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.toolbox.R
import io.toolbox.ui.WindowInsetsHandler
import ru.morozovit.android.invoke
import ru.morozovit.android.plus
import ru.morozovit.android.runOrLog
import ru.morozovit.android.ui.ListItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationHistoryScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        with(LocalContext()) context@ {
            rememberCoroutineScope()

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
                        actions = actions /* TODO actions */,
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
                                            }
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

                LaunchedEffect(Unit) {
                    notifications += NotificationDatabase.list
                }

                LazyColumn(contentPadding = innerPadding) {
                    items(notifications.size) { index ->
                        with (notifications[index]) {
                            Notification(
                                title = title,
                                message = message,
                                onClick = {
                                    runOrLog("NotificationHistory") {
                                        startActivity(packageManager.getLaunchIntentForPackage(sourcePackageName))
                                    }
                                },
                                divider = index != notifications.size - 1,
                                visible = visible,
                                onVisibilityChange = {
                                    visible = it
                                    if (!visible) {
                                        NotificationDatabase -= this
                                    }
                                },
                                sourcePackageName = sourcePackageName,
                                icon = icon
                            )
                        }
                    }
                }
            }
        }
    }
}