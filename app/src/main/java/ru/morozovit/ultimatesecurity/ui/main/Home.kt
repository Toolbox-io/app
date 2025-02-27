package ru.morozovit.ultimatesecurity.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.os.postDelayed
import kotlinx.coroutines.launch
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.android.plus
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SwitchWithText
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.accessibility
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.download
import ru.morozovit.ultimatesecurity.getContents
import ru.morozovit.ultimatesecurity.services.Accessibility.Companion.waitingForAccessibility
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.LocalNavController
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.ultimatesecurity.ui.protection.ActionsActivity
import ru.morozovit.ultimatesecurity.ui.protection.applocker.SelectAppsActivity
import ru.morozovit.utils.MarkdownHeaderParser
import ru.morozovit.utils.toCamelCase
import kotlin.reflect.KMutableProperty0

@OptIn(ExperimentalLayoutApi::class)
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(topBar: @Composable (TopAppBarScrollBehavior) -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext() as MainActivity
    val handler = remember { Handler(Looper.getMainLooper()) }
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                when(it) {
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
                                    val round by animateDpAsState(
                                        targetValue =
                                        if (
                                            computedAlpha != 1f
                                        ) 28.dp
                                        else 0.dp,
                                        label = ""
                                    )

                                    Crossfade(
                                        targetState = state,
                                        modifier = Modifier.animateContentSize(),
                                        label = ""
                                    ) {
                                        when (it) {
                                            0 /* main content */ -> {
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
                                                                    Modifier
                                                                        .combinedClickable(
                                                                            onClick = onClick,
                                                                            onLongClick = {
                                                                                state = 1
                                                                            }
                                                                        )
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
                                            1 /* settings */ -> {
                                                Column(
                                                    modifier = modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceContainer,
                                                            RoundedCornerShape(round)
                                                        )
                                                        .clip(
                                                            RoundedCornerShape(round)
                                                        )
                                                        .alpha(computedAlpha)
                                                        .padding(16.dp)
                                                            +
                                                            if (onClick != null)
                                                                Modifier.combinedClickable(
                                                                    onClick = onClick,
                                                                    onLongClick = {
                                                                        state = 1
                                                                    }
                                                                )
                                                            else
                                                                Modifier
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
                        if (
                            Settings.UnlockProtection.enabled &&
                            !(
                                Settings.Actions.Alarm.enabled ||
                                Settings.Actions.IntruderPhoto.enabled
                            )
                        ) {
                            var notification: NotificationData? = null
                            notification = NotificationData(
                                title = R.string.turn_on_at_least_one_action,
                                message = R.string.turn_on_at_least_one_action_d,
                                bottomContent = {
                                    TextButton(
                                        onClick = {
                                            Intent(context, ActionsActivity::class.java).let {
                                                context.activityLauncher.launch(it) {
                                                    if (
                                                        !(
                                                            Settings.UnlockProtection.enabled &&
                                                            !(
                                                                Settings.Actions.Alarm.enabled ||
                                                                Settings.Actions.IntruderPhoto.enabled
                                                            )
                                                        )
                                                    ) {
                                                        notification!!.visible = false
                                                    }
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
                        if (Settings.Applocker.used) {
                            if (!accessibility) {
                                var notification: NotificationData? = null
                                notification = NotificationData(
                                    title = R.string.reenable_accessibility,
                                    message = R.string.reenable_accessibility_d,
                                    bottomContent = {
                                        TextButton(
                                            onClick = {
                                                val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                                intent.flags = FLAG_ACTIVITY_NEW_TASK
                                                var resumeHandler: (() -> Unit)? = null
                                                resumeHandler = {
                                                    if (accessibility) {
                                                        notification!!.visible = false
                                                        Settings.Applocker.used = true
                                                    }
                                                    waitingForAccessibility = false
                                                    context.resumeHandlers.remove(resumeHandler)
                                                }
                                                context.resumeHandlers.add(resumeHandler)
                                                waitingForAccessibility = true
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Text(text = "Enable")
                                        }
                                    },
                                    type = NotificationType.ENABLE_ACCESSIBILITY
                                )
                                notifications += notification
                            }
                            if (!Settings.Keys.Applocker.isSet) {
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
                            if (Settings.Applocker.apps.isEmpty()) {
                                var notification: NotificationData? = null
                                notification = NotificationData(
                                    title = R.string.select_apps_1,
                                    message = R.string.select_apps_1_d,
                                    bottomContent = {
                                        TextButton(
                                            onClick = {
                                                context.activityLauncher.launch(
                                                    Intent(context, SelectAppsActivity::class.java)
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

                    Crossfade(
                        targetState = notifications.isNotEmpty(),
                        label = ""
                    ) { notEmpty ->
                        if (notEmpty) {
                            Column {
                                notifications.forEachIndexed { index, notification ->
                                    if (notification.type.isEnabled) {
                                        notification.onVisibilityChange = {
                                            set(it)
                                            if (!it) {
                                                handler.postDelayed(1000) {
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
//                    val sheetState = rememberModalBottomSheetState()
//                    var showBottomSheet by remember { mutableStateOf(false) }
                    var uri: Uri? by remember { mutableStateOf(null) }

                    /*fun hideBottomSheet() {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }*/

                    LaunchedEffect(Unit) {
                        async {
                            Log.d("Guides", "Loading guides")
                            try {
                                val guidesDir = getContents("guides")!!.asJsonArray

                                for (it in guidesDir) {
                                    try {
                                        val entry = it.asJsonObject

                                        val name0 = entry["name"].asString
                                        Log.d("Guides", name0)

                                        if (
                                            name0 == "README.md" ||
                                            !name0.endsWith(".md")
                                        ) {
                                            Log.d("Guides", "File doesn't meet the requirements")
                                            continue
                                        }

                                        val contents = String(
                                            download(entry["download_url"].asString)!!.toByteArray(Charsets.ISO_8859_1),
                                            Charsets.UTF_8
                                        )
                                        Log.d("Guides", contents)
                                        val header = MarkdownHeaderParser.parseHeader(contents)
                                        Log.d("Guides", header.toString())

                                        val name = name0.replace("\\.md$".toRegex(), "")
                                        val title = if (header != null) header["DisplayName"] as String else name
                                        val htmlFile =
                                            Uri.parse(
                                                "https://toolbox-io.ru/guides/${name.lowercase()}_raw.html"
                                            )

                                        val icon = try {
                                            val iconName = (header!!["Icon"] as String)
                                                .toCamelCase()
                                                .replaceFirstChar { it.uppercase() }
                                            Class
                                                .forName(
                                                    "androidx.compose.material.icons.filled.${iconName}Kt"
                                                )
                                                .getMethod(
                                                    "get${iconName}",
                                                    Icons.Filled::class.java
                                                )
                                                .invoke(null, Icons.Filled) as ImageVector
                                        } catch (e: Exception) {
                                            Log.e("Guides", "An error occurred while getting icon: ", e)
                                            Icons.Filled.Description
                                        }

                                        guides += Guide(
                                            title = title,
                                            name = name,
                                            icon = icon,
                                            htmlFile = htmlFile
                                        ).also { Log.d("Guides", "$it") }
                                    } catch (e: Exception) {
                                        Log.e("Guides", "An error occurred: ", e)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("Guides", "An error occurred: ", e)
                            }
                        }
                    }

                    for (i in guides.indices) {
                        val guide = guides[i]

                        val onClick = {
                            uri = guide.htmlFile
//                            showBottomSheet = true
                            context.startActivity(
                                Intent(context, GuideActivity::class.java).apply {
                                    data = uri
                                }
                            )
                        }

                        if (i != guides.size - 1) {
                            ListItem(
                                headline = guide.title,
                                leadingContent = {
                                    Icon(
                                        imageVector = guide.icon,
                                        contentDescription = null
                                    )
                                },
                                onClick = onClick,
                                divider = true,
                                dividerColor = MaterialTheme.colorScheme.surface,
                                dividerThickness = 2.dp
                            )
                        } else {
                            ListItem(
                                headline = guide.title,
                                leadingContent = {
                                    Icon(
                                        imageVector = guide.icon,
                                        contentDescription = null
                                    )
                                },
                                onClick = onClick
                            )
                        }
                    }

                    /*if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                showBottomSheet = false
                            },
                            sheetState = sheetState
                        ) {
                            LazyVerticalGrid(columns = GridCells.Fixed(1)) {
                                item(span = { GridItemSpan(1) }) {
                                    AndroidView(
                                        factory = {
                                            WebView(it).apply {
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                                )
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                                        if (request.url != uri) {
                                                            context.openUrl(request.url)
                                                            return true
                                                        } else {
                                                            return false
                                                        }
                                                    }
                                                }
                                                settings.javaScriptEnabled = true
                                                settings.useWideViewPort = true
                                                loadUrl(uri.toString())
                                            }
                                        },
                                        update = {
                                            it.loadUrl(uri.toString())
                                        }
                                    )
                                }
                            }
                        }
                    }*/
                }
            }
        }
    }
}