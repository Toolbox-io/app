package io.toolbox.ui.tools.notificationhistory

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.drawable.Drawable
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxState.Companion.Saver
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.toolbox.R
import io.toolbox.Settings.NotificationHistory.enabled
import io.toolbox.services.NotificationService
import io.toolbox.ui.MainActivity
import io.toolbox.ui.WindowInsetsHandler
import kotlinx.coroutines.launch
import ru.morozovit.android.invoke
import ru.morozovit.android.plus
import ru.morozovit.android.runOrLog
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.android.ui.SwitchCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationHistoryScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        with(LocalContext() as MainActivity) context@ {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()

            val notifications = remember { mutableStateListOf<NotificationData>() }
            var changed by remember { mutableStateOf(false) }

            val notification_deleted = stringResource(R.string.notification_deleted)
            val undo = stringResource(R.string.undo)

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
                icon: Drawable? = null,
                container: Boolean = false
            ) {
                val appInfo = packageManager.getApplicationInfo(sourcePackageName, 0)
                val appName = appInfo.loadLabel(packageManager).toString()

                var prevValue by remember { mutableStateOf<Boolean?>(null) }
                var resetState by remember { mutableStateOf(false) }

                fun callOnVisibilityChange(value: Boolean) {
                    if (value != prevValue) {
                        onVisibilityChange(value)
                        prevValue = value
                    }
                }

                val density = LocalDensity()
                val confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = v@ {
                    when (it) {
                        SwipeToDismissBoxValue.StartToEnd,
                        SwipeToDismissBoxValue.EndToStart -> {
                            callOnVisibilityChange(false)
                        }
                        SwipeToDismissBoxValue.Settled -> return@v false
                    }
                    return@v true
                }
                val positionalThreshold: (Float) -> Float = { it * .5f }

                val dismissState = rememberSaveable(
                    resetState,
                    saver = Saver(
                        confirmValueChange = confirmValueChange,
                        density = density,
                        positionalThreshold = positionalThreshold
                    )
                ) {
                    resetState = false
                    prevValue = null
                    SwipeToDismissBoxState(
                        SwipeToDismissBoxValue.Settled,
                        density,
                        confirmValueChange,
                        positionalThreshold
                    )
                }

                LaunchedEffect(visible) {
                    if (visible) resetState = true
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + expandVertically() + slideInHorizontally(),
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
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                if (container)
                                                    MaterialTheme.colorScheme.surfaceContainer
                                                else MaterialTheme.colorScheme.surface
                                            }

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
                            Log.d("NotificationHistory", computedAlpha.toString())

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

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    val snackbarHostState = remember { SnackbarHostState() }

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
                                    IconButton(
                                        onClick = {
                                            startActivity(
                                                Intent(
                                                    this@context,
                                                    NotificationHistorySettingsActivity::class.java
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = stringResource(R.string.settings)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            navController.navigate("search")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = stringResource(R.string.search)
                                        )
                                    }
                                    actions()
                                },
                                scrollBehavior = scrollBehavior
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(snackbarHostState)
                        }
                    ) { innerPadding ->
                        var loading by remember { mutableStateOf(true) }

                        Box(modifier = Modifier.padding(innerPadding)) {
                            if (loading) {
                                LinearProgressIndicator(
                                    Modifier.fillMaxWidth()
                                )
                            }

                            LaunchedEffect(NotificationService.lastNotification, changed) {
                                loading = true
                                changed = false
                                notifications.clear()
                                NotificationDatabase.optimize()
                                notifications += NotificationDatabase.list
                                loading = false
                            }

                            @Composable
                            fun MainSwitch() {
                                var openPermissionDialog by remember { mutableStateOf(false) }
                                fun onPermissionDialogDismiss() {
                                    openPermissionDialog = false
                                }

                                if (!NotificationService.running) enabled = false
                                var mainSwitch by remember { mutableStateOf(enabled && NotificationService.running) }
                                var mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@{
                                    if (it && !NotificationService.running) {
                                        openPermissionDialog = true
                                        return@sw
                                    }
                                    mainSwitch = it
                                    enabled = it
                                }

                                SimpleAlertDialog(
                                    open = openPermissionDialog,
                                    onDismissRequest = ::onPermissionDialogDismiss,
                                    title = stringResource(R.string.permissions_required),
                                    body = stringResource(R.string.nh_permissions),
                                    positiveButtonText = stringResource(R.string.ok),
                                    onPositiveButtonClick = {
                                        val intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        intent.flags = FLAG_ACTIVITY_NEW_TASK
                                        var handler: (() -> Unit)? = null
                                        handler = {
                                            if (NotificationService.running) {
                                                mainSwitch = true
                                                enabled = true
                                            }
                                            NotificationService.returnBack = false
                                            resumeHandlers.remove(handler)
                                        }
                                        resumeHandlers.add(handler)
                                        NotificationService.returnBack = true
                                        startActivity(intent)
                                    },
                                    negativeButtonText = stringResource(R.string.cancel),
                                    onNegativeButtonClick = ::onPermissionDialogDismiss
                                )

                                SwitchCard(
                                    text = stringResource(R.string.enable),
                                    checked = mainSwitch,
                                    onCheckedChange = mainSwitchOnCheckedChange,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            // TODO date markers and separators
                            if (notifications.isNotEmpty() || loading) {
                                LazyColumn {
                                    item {
                                        MainSwitch()
                                    }

                                    for (index in notifications.indices.reversed()) {
                                        item {
                                            with(notifications[index]) notification@ {
                                                Notification(
                                                    title = title,
                                                    message = message,
                                                    onClick = {
                                                        runOrLog("NotificationHistory") {
                                                            startActivity(packageManager.getLaunchIntentForPackage(sourcePackageName))
                                                        }
                                                    },
                                                    divider = index != notifications.size - 1,
                                                    visible = visible.value,
                                                    onVisibilityChange = {
                                                        Log.d("NotificationHistory", "visibility for $this changed to $it")
                                                        visible.value = it
                                                        if (!visible.value) {
                                                            coroutineScope.launch {
                                                                val result = snackbarHostState.showSnackbar(
                                                                    message = notification_deleted,
                                                                    actionLabel = undo,
                                                                    duration = SnackbarDuration.Long,
                                                                )
                                                                when (result) {
                                                                    SnackbarResult.Dismissed -> {
                                                                        NotificationDatabase -= this@notification
                                                                    }
                                                                    SnackbarResult.ActionPerformed -> {
                                                                        visible.value = true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    sourcePackageName = sourcePackageName,
                                                    icon = icon
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column {
                                    MainSwitch()
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_notifications),
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                composable("search") {
                    val snackbarHostState = remember { SnackbarHostState() }

                    var searchInputState by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }

                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            TopAppBar(
                                title = {
                                    BasicTextField(
                                        value = searchInputState,
                                        onValueChange = { searchInputState = it },
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.focusRequester(focusRequester),
                                        maxLines = 1,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            autoCorrectEnabled = true,
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Search
                                        )
                                    )

                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            navController.navigateUp()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(snackbarHostState)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) { innerPadding ->
                        val filtered = remember { mutableStateListOf<NotificationData>() }

                        LaunchedEffect(searchInputState) {
                            filtered.clear()
                            if (!searchInputState.isBlank()) {
                                val words = searchInputState.split(" ")
                                filtered += notifications.filter {
                                    val result1 = run {
                                        var matches = 0
                                        val filt = it.title
                                        words.forEach { word ->
                                            if (
                                                filt.contains(
                                                    other = word,
                                                    ignoreCase = true
                                                )
                                            ) matches++
                                        }
                                        return@run matches > words.size * 0.5
                                    }
                                    val result2 = run {
                                        var matches = 0
                                        val filt = it.message
                                        words.forEach { word ->
                                            if (
                                                filt.contains(
                                                    other = word,
                                                    ignoreCase = true
                                                )
                                            ) matches++
                                        }
                                        return@run matches > words.size * 0.5
                                    }
                                    result1 || result2
                                }
                            }
                        }

                        LazyColumn(contentPadding = innerPadding) {
                            items(filtered.indices.reversed().toList()) { index ->
                                with(filtered[index]) notification@ {
                                    var visible by remember { mutableStateOf(true) }

                                    Notification(
                                        title = title,
                                        message = message,
                                        onClick = {
                                            runOrLog("NotificationHistory") {
                                                startActivity(packageManager.getLaunchIntentForPackage(sourcePackageName))
                                            }
                                        },
                                        divider = index != filtered.size - 1,
                                        visible = visible,
                                        onVisibilityChange = {
                                            Log.d("NotificationHistory", "visibility for $this changed to $it")
                                            visible = it
                                            if (!visible) {
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = notification_deleted,
                                                        actionLabel = undo,
                                                        duration = SnackbarDuration.Long,
                                                    )
                                                    when (result) {
                                                        SnackbarResult.Dismissed -> {
                                                            NotificationDatabase -= this@notification
                                                            changed = true
                                                        }
                                                        SnackbarResult.ActionPerformed -> {
                                                            visible = true
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        sourcePackageName = sourcePackageName,
                                        icon = icon,
                                        container = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}