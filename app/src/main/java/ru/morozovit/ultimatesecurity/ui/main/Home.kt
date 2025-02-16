package ru.morozovit.ultimatesecurity.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.android.plus
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@OptIn(ExperimentalLayoutApi::class)
private data class NotificationData(
    val title: String,
    val message: String,
    val onClick: (() -> Unit)? = null,
    val divider: Boolean,
    val bottomContent: (@Composable FlowRowScope.() -> Unit)? = null,
    val type: NotificationType
)

private enum class NotificationSource(
    @StringRes val label: Int,
    val icon: ImageVector
) {
    APP_LOCKER(R.string.applocker, Icons.Filled.PhonelinkLock),
    UNLOCK_PROTECTION(R.string.unlock_protection, Icons.Filled.Lock)
}

private enum class NotificationType(val source: NotificationSource) {
    // App Locker
    ENABLE_ACCESSIBILITY(NotificationSource.APP_LOCKER),
    SET_PASSWORD(NotificationSource.APP_LOCKER),
    SELECT_APPS(NotificationSource.APP_LOCKER),

    // Unlock Protection
    ENABLE_AT_LEAST_ONE_FEATURE(NotificationSource.UNLOCK_PROTECTION)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(topBar: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext() as MainActivity
    WindowInsetsHandler {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = topBar
        ) { innerPadding ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                // UPDATE
                if (!update_dsa) {
                    var isUpdateCardVisible by remember { mutableStateOf(false) }

                    val versionFormat = stringResource(R.string.update_version)
                    var version by remember { mutableStateOf("") }
                    var body by remember { mutableStateOf("") }
                    var downloadOnClick by remember { mutableStateOf({}) }

                    AnimatedVisibility(
                        visible = isUpdateCardVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.7f),
                        exit = fadeOut() + scaleOut(targetScale = 0.7f)
                    ) {
                        Card(
                            modifier = Modifier
                                .padding()
                                .padding(16.dp)
                                .fillMaxWidth(),
                            colors = cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.update),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                HorizontalDivider()
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                                )
                                Row {
                                    TextButton(onClick = downloadOnClick) {
                                        Text(text = stringResource(R.string.download))
                                    }
                                    TextButton(
                                        onClick = {
                                            update_dsa = true
                                            isUpdateCardVisible = false
                                        }
                                    ) {
                                        Text(text = stringResource(R.string.dsa))
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        async {
                            runCatching {
                                val info = checkForUpdates()!!
                                if (info.available) {
                                    version = String.format(versionFormat, info.version)
                                    body = info.description
                                    downloadOnClick = {
                                        context.sendBroadcast(
                                            Intent(App.context, DownloadBroadcastReceiver::class.java).apply {
                                                action = DOWNLOAD_BROADCAST
                                                putExtra("updateInfo", info)
                                            }
                                        )
                                    }
                                    isUpdateCardVisible = true
                                }
                            }
                        }
                    }
                }

                // Notifications permission request
                val grant_notification = stringResource(R.string.grant_notification)
                val grant = stringResource(R.string.grant)

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (
                            context.checkSelfPermission(
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            val result = snackbarHostState.showSnackbar(
                                message = grant_notification,
                                actionLabel = grant,
                                duration = SnackbarDuration.Long
                            )
                            when (result) {
                                SnackbarResult.ActionPerformed -> context.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                                SnackbarResult.Dismissed -> {}
                            }
                        }
                    }
                }

                @Composable
                fun Notification(
                    modifier: Modifier = Modifier,
                    title: String,
                    message: String,
                    onClick: (() -> Unit)? = null,
                    divider: Boolean,
                    bottomContent: (@Composable FlowRowScope.() -> Unit)? = null,
                    type: NotificationType
                ) {
                    var visible by remember { mutableStateOf(true) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            when(it) {
                                SwipeToDismissBoxValue.StartToEnd,
                                SwipeToDismissBoxValue.EndToStart -> {
                                    visible = false
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
                                val round by animateDpAsState(
                                    targetValue =
                                        if (
                                            computedAlpha != 1f
                                        ) 28.dp
                                        else 0.dp,
                                    label = ""
                                )

                                Column {
                                    Column(
                                        modifier = modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainer,
                                                RoundedCornerShape(round)
                                            )
                                            .clip(
                                                RoundedCornerShape(round)
                                            )
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
                                                Icon(
                                                    imageVector = type.source.icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = stringResource(type.source.label),
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
                                    AnimatedVisibility(
                                        visible = divider,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.surface,
                                            thickness = 2.dp
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                val notifications = mutableListOf<NotificationData>()


                // Notifications
                Category(containerColor = MaterialTheme.colorScheme.surface) {
                    notifications.forEachIndexed { index, it ->
                        Notification(
                            title = it.title,
                            message = it.message,
                            onClick = it.onClick,
                            divider = index != notifications.size - 1,
                            bottomContent = it.bottomContent,
                            type = it.type
                        )
                    }
                }

                // TODO add some content
            }
        }
    }
}