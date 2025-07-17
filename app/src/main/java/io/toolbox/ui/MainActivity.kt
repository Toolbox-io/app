package io.toolbox.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.toolbox.App.Companion.authenticated
import io.toolbox.BaseActivity
import io.toolbox.IssueReporter
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.UpdateChecker
import io.toolbox.ui.Auth.Companion.started
import io.toolbox.ui.MainActivity.Screen.Companion.ABOUT
import io.toolbox.ui.MainActivity.Screen.Companion.APP_LOCKER
import io.toolbox.ui.MainActivity.Screen.Companion.APP_MANAGER
import io.toolbox.ui.MainActivity.Screen.Companion.DONT_TOUCH_MY_PHONE
import io.toolbox.ui.MainActivity.Screen.Companion.HOME
import io.toolbox.ui.MainActivity.Screen.Companion.NOTIFICATION_HISTORY
import io.toolbox.ui.MainActivity.Screen.Companion.PROFILE
import io.toolbox.ui.MainActivity.Screen.Companion.SETTINGS
import io.toolbox.ui.MainActivity.Screen.Companion.SHORTCUTS
import io.toolbox.ui.MainActivity.Screen.Companion.UNLOCK_PROTECTION
import io.toolbox.ui.customization.shortcuts.ShortcutsScreen
import io.toolbox.ui.main.AboutScreen
import io.toolbox.ui.main.HomeScreen
import io.toolbox.ui.main.ProfileScreen
import io.toolbox.ui.main.SettingsScreen
import io.toolbox.ui.protection.DontTouchMyPhoneScreen
import io.toolbox.ui.protection.UnlockProtectionScreen
import io.toolbox.ui.protection.applocker.ApplockerScreen
import io.toolbox.ui.tools.appmanager.AppManagerScreen
import io.toolbox.ui.tools.notificationhistory.NotificationHistoryScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.morozovit.android.utils.ActivityLauncher
import ru.morozovit.android.utils.activityResultLauncher
import ru.morozovit.android.utils.runOrLog
import ru.morozovit.android.utils.ui.ComposeView
import ru.morozovit.android.utils.ui.WidthSizeClass
import ru.morozovit.android.utils.ui.compareTo
import ru.morozovit.android.utils.ui.compositionLocalOf
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.left
import ru.morozovit.android.utils.ui.link
import ru.morozovit.android.utils.ui.right
import ru.morozovit.android.utils.ui.verticalScroll
import ru.morozovit.android.utils.ui.widthSizeClass
import ru.morozovit.android.utils.unsupported

val LocalNavController = compositionLocalOf<NavController>()
val LocalHazeState = compositionLocalOf<HazeState>()

class MainActivity : BaseActivity() {
    private var prevConfig: Configuration? = null
    lateinit var activityLauncher: ActivityLauncher
    val resumeHandlers = mutableListOf<() -> Unit>()
    private var isLockVisible by mutableStateOf(false)
    private var uriIntent: Intent? by mutableStateOf(intent)

    sealed class BaseScreen

    /**
     * To add a screen:
     * 1. Create a constant holding the internal string name of the screen.
     *    ```
     *    const val SCREEN = "screen"
     *    ```
     * 2. Create a `data object` holding the screen data.
     *    ```
     *    @Serializable data object Screen: Screen(
     *         SCREEN,
     *         R.string.screen,
     *         Icons.Filled.Android
     *    )
     *    ```
     * 3. Add `CONSTANT -> ScreenObject` to the `when` branches in the [get()][Screen.get] method.
     *    Replace the placeholder names with the actual values.
     *    ```
     *    operator fun get(name: String) = when (name) {
     *        // ...
     *        SCREEN -> Screen
     *        // ...
     *    }
     *    ```
     * 4. Add a `@`[Composable] to the navigation graph with
     *    route set to the constant
     *    ```
     *    composable(route = SCREEN) { Screen() }
     *    ```
     * 5. Add your `data object` to the `items` list.
     *    ```
     *    val items = listOf(
     *        // ...
     *        Screen.Screen
     *        // ...
     *    )
     *    ```
     */
    @Serializable private sealed class Screen(
        val internalName: String,
        @StringRes val displayName: Int,
        @Transient val icon: ImageVector = unsupported
    ): BaseScreen() {
        companion object {
            operator fun get(name: String) = when (name) {
                HOME -> Home
                SETTINGS -> Settings
                PROFILE -> Profile
                ABOUT -> About
                APP_LOCKER -> AppLocker
                UNLOCK_PROTECTION -> UnlockProtection
                SHORTCUTS -> Shortcuts
                APP_MANAGER -> AppManager
                DONT_TOUCH_MY_PHONE -> DontTouchMyPhone
                NOTIFICATION_HISTORY -> NotificationHistory
                else -> {
                    Log.e("MainActivity", "Unknown screen index: $name")
                    null
                }
            }

            const val HOME = "home"
            const val SETTINGS = "settings"
            const val PROFILE = "profile"
            const val ABOUT = "about"

            const val APP_LOCKER = "appLocker"
            const val UNLOCK_PROTECTION = "unlockProtection"

            const val SHORTCUTS = "shortcuts"

            const val APP_MANAGER = "appManager"
            const val DONT_TOUCH_MY_PHONE = "dontTouchMyPhone"
            const val NOTIFICATION_HISTORY = "notificationHistory"
        }

        override fun equals(other: Any?) = other is Screen && displayName == other.displayName

        override fun hashCode() = displayName.hashCode()

        class Label(@StringRes val label: Int): BaseScreen()

        @Serializable data object Home: Screen(HOME, R.string.home, Icons.Filled.Home)
        @Serializable data object Settings: Screen(SETTINGS, R.string.settings, Icons.Filled.Settings)
        @Serializable data object Profile: Screen(PROFILE, R.string.profile, Icons.Filled.AccountCircle)
        @Serializable data object About: Screen(ABOUT, R.string.about, Icons.Filled.Info)

        @Serializable data object AppLocker: Screen(APP_LOCKER, R.string.applocker, Icons.Outlined.PhonelinkLock)
        @Serializable data object UnlockProtection: Screen(UNLOCK_PROTECTION, R.string.unlock_protection, Icons.Filled.Lock)

        @Serializable data object Shortcuts: Screen(SHORTCUTS, R.string.shortcuts, Icons.AutoMirrored.Filled.Shortcut)

        @Serializable data object AppManager: Screen(APP_MANAGER, R.string.app_manager, Icons.Filled.Apps)
        @Serializable data object DontTouchMyPhone: Screen(DONT_TOUCH_MY_PHONE, R.string.dont_touch_my_phone, Icons.Filled.DoNotTouch)
        @Serializable data object NotificationHistory: Screen(
            NOTIFICATION_HISTORY,
            R.string.notification_history,
            Icons.Filled.NotificationsActive
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
    @Composable
    fun MainScreen() {
        AppTheme(consumeLeftInsets = true, consumeRightInsets = true) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            var selectedItem by rememberSaveable { mutableStateOf(HOME) }
            val navController = rememberNavController()

            val currentEntry by navController.currentBackStackEntryAsState()
            val hazeState = rememberHazeState(blurEnabled = true)
            val isScreenBig = currentWindowAdaptiveInfo().widthSizeClass >= WidthSizeClass.MEDIUM

            // Crash dialog
            var isCrashDialogOpen by remember { mutableStateOf(false) }
            var crashStackTrace: String? by remember { mutableStateOf(null) }
            var crashActivityLog: String? by remember { mutableStateOf(null) }

            // Set currentItem based on the route
            LaunchedEffect(currentEntry) {
                runCatching {
                    selectedItem = currentEntry!!.destination.route!!
                }
            }

            // Process new intents
            LaunchedEffect(uriIntent) {
                val intent = uriIntent

                // Process "toolbox-io://" URIs
                intent?.data
                    .takeIf {
                        it?.scheme == "toolbox-io"
                    }
                    ?.let {
                        "${it.host}/${it.path}"
                    }
                    ?.let {
                        when {
                            it.startsWith("page/") -> {
                                try {
                                    navController.navigate(it.replaceFirst("page//", ""))
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.invalid_uri,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                if (intent == null) {
                    Log.w("IssueReporter", "intent is null")
                }

                // Process crashes
                if (
                    intent != null &&
                    runCatching {
                        CustomActivityOnCrash.getCustomCrashDataFromIntent(intent)
                    }.also {
                        val str = it.exceptionOrNull() ?: it.getOrNull() ?: "<null>"
                        Log.d("IssueReporter", "Crash data: $str")
                    }.getOrNull() == "crash_mainactivity"
                ) {
                    crashStackTrace = CustomActivityOnCrash.getStackTraceFromIntent(intent)
                    crashActivityLog = CustomActivityOnCrash.getActivityLogFromIntent(intent)

                    Log.e(
                        "IssueReporter",
                        """
                        |App crashed!
                        |
                        |Stack trace:
                        |${crashStackTrace}
                        |
                        |Activity log:
                        |${crashActivityLog}
                        """.trimMargin()
                    )

                    isCrashDialogOpen = true
                }
            }

            if (isCrashDialogOpen) {
                AlertDialog(
                    icon = {
                        Icon(Icons.Filled.BugReport, null)
                    },
                    title = {
                        Text(stringResource(R.string.im_sorry))
                    },
                    text = {
                        Text(stringResource(R.string.crash_d))
                    },
                    onDismissRequest = { isCrashDialogOpen = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isCrashDialogOpen = false
                                startActivity(
                                    Intent(this@MainActivity, IssueReporter.ExceptionDetailsActivity::class.java).apply {
                                        putExtra(
                                            "exception",
                                            """
                                            |Stack trace:
                                            |${crashStackTrace}
                                            |
                                            |Activity log:
                                            |${crashActivityLog}
                                            """.trimMargin()
                                        )
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(R.string.details))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { isCrashDialogOpen = false }
                        ) {
                            Text(stringResource(R.string.continue1))
                        }
                    }
                )
            }


            val drawerContent: @Composable ColumnScope.() -> Unit = {
                Column(
                    Modifier
                        .verticalScroll()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top +
                                        WindowInsetsSides.Bottom +
                                        WindowInsetsSides.Left
                            )
                        )
                ) {
                    ConstraintLayout(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        val (icon, title, subtitle) = createRefs()
                        AppIcon(
                            Modifier.constrainAs(icon) {
                                top link parent.top
                                left link parent.left
                            }
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .constrainAs(title) {
                                    top link parent.top
                                    right link parent.right
                                    left link icon.right
                                    width = Dimension.fillToConstraints
                                }
                                .padding(start = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_desc),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .constrainAs(subtitle) {
                                    top link title.bottom
                                    right link parent.right
                                    left link icon.right
                                    width = Dimension.fillToConstraints
                                }
                                .padding(start = 12.dp)
                        )
                    }


                    listOf(
                        Screen.Home,
                        Screen.Settings,
                        Screen.Profile,
                        Screen.About,
                        Screen.Label(R.string.security),
                        Screen.AppLocker,
                        Screen.UnlockProtection,
                        Screen.DontTouchMyPhone,
                        Screen.Label(R.string.customization),
                        Screen.Shortcuts,
                        Screen.Label(R.string.tools),
                        Screen.AppManager,
                        Screen.NotificationHistory
                    ).forEach {
                        when (it) {
                            is Screen -> NavigationDrawerItem(
                                icon = { Icon(it.icon, contentDescription = null) },
                                label = { Text(stringResource(it.displayName)) },
                                selected = it == Screen[selectedItem],
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (Screen[selectedItem] != it) {
                                        selectedItem = it.internalName
                                        navController.navigate(it.internalName)
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            is Screen.Label -> Text(
                                text = stringResource(it.label),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 28.dp, end = 36.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            val content = @Composable {
                val navigation = @Composable {
                    if (!isScreenBig) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                    }
                }

                val actions: @Composable RowScope.() -> Unit = {
                    if (isLockVisible) {
                        IconButton(
                            onClick = {
                                authenticated = false
                                auth()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                }

                val bar: @Composable (TopAppBarScrollBehavior) -> Unit = { scrollBehavior ->
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(Color.Unspecified, Color.Transparent),
                        title = {
                            val titleRes = Screen[selectedItem]?.displayName
                            val title = if (titleRes != null) {
                                stringResource(titleRes)
                            } else {
                                ""
                            }
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = navigation,
                        actions = actions,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Right))
                            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()),
                        scrollBehavior = scrollBehavior
                    )
                }

                @Suppress("LocalVariableName")
                val EdgeToEdgeBar = @Composable { content: @Composable (PaddingValues) -> Unit ->
                    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = { bar(scrollBehavior) },
                        contentWindowInsets = WindowInsets.systemBars
                    ) {
                        content(it)
                    }
                }

                val start by remember { mutableStateOf(selectedItem) }
                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalHazeState provides hazeState
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier.consumeWindowInsets(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Left)
                        )
                    ) {
                        // Core
                        composable(route = HOME) {
                            HomeScreen(
                                bar,
                                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            )
                        }
                        composable(route = SETTINGS) { SettingsScreen(EdgeToEdgeBar) }
                        composable(route = PROFILE) {
                            ProfileScreen(
                                bar,
                                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            )
                        }
                        composable(route = ABOUT) { AboutScreen(EdgeToEdgeBar) }

                        // Security
                        composable(route = APP_LOCKER) {
                            ApplockerScreen(
                                bar,
                                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            )
                        }
                        composable(route = UNLOCK_PROTECTION) { UnlockProtectionScreen(EdgeToEdgeBar) }
                        composable(route = DONT_TOUCH_MY_PHONE) { DontTouchMyPhoneScreen(EdgeToEdgeBar) }

                        // Customization
                        composable(route = SHORTCUTS) { ShortcutsScreen(EdgeToEdgeBar) }

                        // Tools
                        composable(route = APP_MANAGER) {
                            AppManagerScreen(
                                actions,
                                navigation,
                                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            )
                        }
                        composable(route = NOTIFICATION_HISTORY) {
                            NotificationHistoryScreen(
                                actions,
                                navigation,
                                TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            )
                        }
                    }
                }
            }

            if (isScreenBig) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            content = drawerContent,
                            drawerShape = RoundedCornerShape(0.dp, 20.dp, 20.dp, 0.dp),
                            modifier = Modifier.widthIn(
                                max = (
                                    if (LocalWindowInfo().containerSize.width * 0.5 < 360)
                                        300
                                    else 360
                                ).dp
                            )
                        )
                    },
                    content = content
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            drawerState = drawerState,
                            modifier = Modifier.widthIn(
                                max = (LocalWindowInfo().containerSize.width * 0.9)
                                    .coerceIn(300.0..360.0)
                                    .dp
                            ),
                            content = drawerContent
                        )
                    },
                    content = content
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityLauncher = activityResultLauncher
        prevConfig = resources.configuration
        uriIntent = intent

        enableEdgeToEdge()
        setContentView(
            ComposeView {
                MainScreen()
            }.also {
                if (pendingAuth) {
                    // if not started don't draw the UI
                    it.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            if (started) {
                                it.viewTreeObserver.removeOnPreDrawListener(this)
                            }
                            return false
                        }
                    })
                } else {
                    startEnterAnimation(it)
                }
            }
        )
        updateLock()

        // Start update checker
        runOrLog("MainActivity") {
            UpdateChecker.schedule(this)
        }
    }

    override fun finish() {
        super.finish()
        authenticated = false
    }

    override fun onResume() {
        super.onResume()
        if (!pendingAuth) updateLock()
        for (handler in resumeHandlers) handler()
    }

    fun updateLock() {
        isLockVisible = Settings.Keys.App.isSet
        interactionDetector()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            authenticated = false
            splashScreenDisplayed = false
            isSplashScreenVisible = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        uriIntent = intent
    }
}