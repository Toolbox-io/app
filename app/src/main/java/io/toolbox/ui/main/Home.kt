@file:OptIn(ExperimentalLayoutApi::class)

package io.toolbox.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.net.toUri
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.Settings.update_dsa
import io.toolbox.api.GuidesAPI
import io.toolbox.services.Accessibility
import io.toolbox.services.Accessibility.Companion.returnBack
import io.toolbox.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import io.toolbox.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import io.toolbox.services.UpdateChecker.Companion.checkForUpdates
import io.toolbox.ui.LocalNavController
import io.toolbox.ui.MainActivity
import io.toolbox.ui.TopBarType
import io.toolbox.ui.protection.actions.ActionsActivity
import io.toolbox.ui.protection.applocker.SelectAppsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.morozovit.android.utils.toPascalCase
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SwitchWithText
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.applyIf
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll
import kotlin.reflect.KMutableProperty0

private data class NotificationData(
    @StringRes val title: Int,
    @StringRes val message: Int,
    val onClick: (() -> Unit)? = null,
    val bottomContent: (@Composable FlowRowScope.() -> Unit)? = null,
    val type: NotificationType
) {
    var onVisibilityChange: (KMutableProperty0<Boolean>.(Boolean) -> Unit)? = null
    private var _visible by mutableStateOf(true)
    var visible: Boolean
        get() = _visible
        set(value) {
            if (onVisibilityChange != null) {
                onVisibilityChange!!(::_visible, value)
            } else {
                _visible = value
            }
        }
}
private enum class NotificationSource(
    @StringRes val label: Int,
    val icon: ImageVector
) {
    APP_LOCKER(R.string.applocker, Icons.Filled.PhonelinkLock),
    UNLOCK_PROTECTION(R.string.unlock_protection, Icons.Filled.Lock)
}
private enum class NotificationType(
    val source: NotificationSource,
    @StringRes val label: Int
) {
    // App Locker
    ENABLE_ACCESSIBILITY(NotificationSource.APP_LOCKER, R.string.enable_accessibility),
    SET_PASSWORD(NotificationSource.APP_LOCKER, R.string.set_password),
    SELECT_APPS(NotificationSource.APP_LOCKER, R.string.select_apps),

    // Unlock Protection
    ENABLE_AT_LEAST_ONE_FEATURE(NotificationSource.UNLOCK_PROTECTION, R.string.eaof)
}
private var NotificationType.isEnabled
    get() = Settings.Notifications[name]
    set(value) {
        Settings.Notifications[name] = value
    }

private data class Guide(
    val name: String,
    val title: String,
    val htmlFile: Uri,
    val icon: ImageVector = Icons.Filled.Description,
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(topBar: TopBarType, scrollBehavior: TopAppBarScrollBehavior) {
    with (LocalContext() as MainActivity) {
        val mainNavController = LocalNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        val ycaeuats = stringResource(R.string.ycaeuats)

        WindowInsetsHandler {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                topBar = { topBar(scrollBehavior) }
            ) { innerPadding ->
                Column(
                    Modifier
                        .verticalScroll()
                        .padding(innerPadding)
                ) {
                    // Update
                    if (!update_dsa) {
                        var isUpdateCardVisible by remember { mutableStateOf(false) }

                        val versionFormat = stringResource(R.string.update_version)
                        var version by remember { mutableStateOf("") }
                        var body by remember { mutableStateOf("") }
                        var downloadOnClick by remember { mutableStateOf({}) }

                        Column(Modifier.animateContentSize()) {
                            AnimatedVisibility(
                                visible = isUpdateCardVisible,
                                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                                exit = fadeOut() + scaleOut(targetScale = 0.7f),
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
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = ycaeuats,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            ) {
                                                Text(text = stringResource(R.string.dsa))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
                            runCatching {
                                val info = checkForUpdates()!!

                                if (info.available) {
                                    version = versionFormat.format(info.version)
                                    body = info.description
                                    downloadOnClick = {
                                        sendBroadcast(
                                            Intent(
                                                this@with,
                                                DownloadBroadcastReceiver::class.java
                                            ).apply {
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

                    // Notifications permission request
                    val grant_notification = stringResource(R.string.grant_notification)
                    val grant = stringResource(R.string.grant)

                    LaunchedEffect(Unit) {
                        if (
                            Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            when (
                                snackbarHostState.showSnackbar(
                                    message = grant_notification,
                                    actionLabel = grant,
                                    duration = SnackbarDuration.Long
                                )
                            ) {
                                SnackbarResult.ActionPerformed -> requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                                SnackbarResult.Dismissed -> {}
                            }
                        }
                    }

                    // In-app notifications
                    Category(containerColor = MaterialTheme.colorScheme.surface, title = stringResource(R.string.notifications)) {
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
                            type: NotificationType
                        ) {
                            var state by remember { mutableIntStateOf(0) }

                            @Suppress("DEPRECATION")
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = v@{
                                    when (it) {
                                        SwipeToDismissBoxValue.StartToEnd,
                                        SwipeToDismissBoxValue.EndToStart -> onVisibilityChange(false)

                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                    true
                                },
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
                                        ) 1f else 1f - dismissState.progress

                                        val round by animateDpAsState(
                                            targetValue = if (computedAlpha != 1f) 28.dp else 0.dp
                                        )

                                        Crossfade(
                                            targetState = state,
                                            modifier = Modifier.animateContentSize()
                                        ) { screen ->
                                            when (screen) {
                                                0 /* main content */ -> {
                                                    Column {
                                                        Column(
                                                            modifier = modifier
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                                                    shape = RoundedCornerShape(round)
                                                                )
                                                                .clip(RoundedCornerShape(round))
                                                                .alpha(computedAlpha)
                                                                .applyIf(onClick != null) {
                                                                    Modifier.combinedClickable(
                                                                        onClick = onClick!!,
                                                                        onLongClick = { state = 1 }
                                                                    )
                                                                }
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
                                                                bottomContent = bottomContent?.let {
                                                                    {
                                                                        FlowRow(
                                                                            modifier = Modifier.padding(start = (24 + 16 - 12).dp, end = 16.dp),
                                                                            content = it
                                                                        )
                                                                    }
                                                                }
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

                                                1 /* settings */ -> {
                                                    Column(
                                                        modifier = modifier
                                                            .background(
                                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                                shape = RoundedCornerShape(round)
                                                            )
                                                            .clip(RoundedCornerShape(round))
                                                            .alpha(computedAlpha)
                                                            .padding(16.dp)
                                                            .applyIf(onClick != null) {
                                                                Modifier.combinedClickable(
                                                                    onClick = onClick!!,
                                                                    onLongClick = { state = 1 }
                                                                )
                                                            }
                                                    ) {
                                                        var enabled by remember { mutableStateOf(type.isEnabled) }
                                                        SwitchWithText(
                                                            checked = enabled,
                                                            onCheckedChange = { v ->
                                                                enabled = v
                                                                type.isEnabled = v
                                                            }
                                                        ) {
                                                            Text(
                                                                text = stringResource(type.label),
                                                            )
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                onVisibilityChange(enabled)
                                                                state = 0
                                                            }
                                                        ) {
                                                            Text(text = stringResource(R.string.save))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        val notifications = remember { mutableStateListOf<NotificationData>() }
                        LaunchedEffect(Unit) {
                            when {
                                Settings.UnlockProtection.enabled && !(
                                        Settings.Actions.Alarm.enabled ||
                                                Settings.Actions.IntruderPhoto.enabled
                                        ) -> {
                                    var notification: NotificationData? = null
                                    notification = NotificationData(
                                        title = R.string.turn_on_at_least_one_action,
                                        message = R.string.turn_on_at_least_one_action_d,
                                        bottomContent = {
                                            TextButton(
                                                onClick = {
                                                    Intent(this@with, ActionsActivity::class.java).let {
                                                        activityLauncher.launch(it) {
                                                            if (
                                                                !(
                                                                    Settings.UnlockProtection.enabled && !(
                                                                        Settings.Actions.Alarm.enabled ||
                                                                        Settings.Actions.IntruderPhoto.enabled
                                                                    )
                                                                )
                                                            ) notification!!.visible = false
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(text = "View actions")
                                            }
                                        },
                                        type = NotificationType.ENABLE_AT_LEAST_ONE_FEATURE
                                    )
                                    notifications += notification
                                }

                                Settings.Applocker.used -> when {
                                    !Accessibility.running -> {
                                        var notification: NotificationData? = null
                                        notification = NotificationData(
                                            title = R.string.reenable_accessibility,
                                            message = R.string.reenable_accessibility_d,
                                            bottomContent = {
                                                TextButton(
                                                    onClick = {
                                                        var resumeHandler: (() -> Unit)? = null
                                                        resumeHandler = {
                                                            if (Accessibility.running) {
                                                                notification!!.visible = false
                                                                Settings.Applocker.used = true
                                                            }
                                                            returnBack = false
                                                            resumeHandlers.remove(resumeHandler)
                                                        }
                                                        resumeHandlers.add(resumeHandler)
                                                        returnBack = true
                                                        startActivity(
                                                            Intent(ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                                flags = FLAG_ACTIVITY_NEW_TASK
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    Text(text = "Enable")
                                                }
                                            },
                                            type = NotificationType.ENABLE_ACCESSIBILITY
                                        )
                                        notifications += notification
                                    }

                                    !Settings.Keys.Applocker.isSet -> {
                                        notifications += NotificationData(
                                            title = R.string.set_password_1,
                                            message = R.string.set_password_1_d,
                                            bottomContent = {
                                                TextButton(
                                                    onClick = {
                                                        mainNavController.navigate("applocker")
                                                    }
                                                ) {
                                                    Text(text = "Set password")
                                                }
                                            },
                                            type = NotificationType.SET_PASSWORD
                                        )
                                    }

                                    Settings.Applocker.apps.isEmpty() -> {
                                        var notification: NotificationData? = null
                                        notification = NotificationData(
                                            title = R.string.select_apps_1,
                                            message = R.string.select_apps_1_d,
                                            bottomContent = {
                                                TextButton(
                                                    onClick = {
                                                        activityLauncher.launch(
                                                            Intent(this@with, SelectAppsActivity::class.java)
                                                        ) {
                                                            if (Settings.Applocker.apps.isNotEmpty()) {
                                                                notification!!.visible = false
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text(text = "Select")
                                                }
                                            },
                                            type = NotificationType.SELECT_APPS
                                        )
                                        notifications += notification
                                    }
                                }
                            }
                        }

                        Crossfade(
                            targetState = notifications.isNotEmpty(),
                            label = ""
                        ) { notEmpty ->
                            if (notEmpty) {
                                Column {
                                    notifications.forEachIndexed { index, notification ->
                                        if (notification.type.isEnabled) {
                                            notification.onVisibilityChange = {
                                                coroutineScope.launch {
                                                    set(it)
                                                    if (!it) {
                                                        delay(1000)
                                                        notifications.remove(notification)
                                                    }
                                                }
                                            }
                                            Notification(
                                                title = stringResource(notification.title),
                                                message = stringResource(notification.message),
                                                onClick = notification.onClick,
                                                divider = index != notifications.size - 1,
                                                bottomContent = notification.bottomContent,
                                                type = notification.type,
                                                visible = notification.visible,
                                                onVisibilityChange = { notification.visible = it }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.CenterHorizontally),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.no_notifications),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Guides
                    Category(title = stringResource(R.string.guides)) {
                        val guides = remember { mutableStateListOf<Guide>() }

                        LaunchedEffect(Unit) {
                            try {
                                for (it in GuidesAPI.list()) {
                                    val name = it.name.removeSuffix(".md")
                                    val header = it.header

                                    guides += Guide(
                                        title = header.DisplayName,
                                        name = name,
                                        icon = try {
                                            val iconName = header.Icon.toPascalCase()
                                            Class
                                                .forName("androidx.compose.material.icons.filled.${iconName}Kt")
                                                .getMethod(
                                                    "get${iconName}",
                                                    Icons.Filled::class.java
                                                )
                                                .invoke(null, Icons.Filled) as ImageVector
                                        } catch (e: Exception) {
                                            Log.e("Guides", "An error occurred while getting icon: ", e)
                                            Icons.Filled.Description
                                        },
                                        htmlFile = "${GuidesAPI.BASE_URL}/${name.lowercase()}/raw".toUri()
                                    ).also { Log.d("Guides", "$it") }
                                }
                            } catch (e: Exception) {
                                Log.e("Guides", "An error occurred: ", e)
                            }
                        }

                        guides.forEachIndexed { i, (_, title, htmlFile, icon) ->
                            ListItem(
                                headline = title,
                                leadingContent = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    startActivity(
                                        Intent(this@with, GuideActivity::class.java).apply {
                                            data = htmlFile
                                        }
                                    )
                                },
                                materialDivider = i != guides.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}