package ru.morozovit.ultimatesecurity.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PhonelinkLock
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.WidthSizeClass
import ru.morozovit.android.compareTo
import ru.morozovit.android.invoke
import ru.morozovit.android.left
import ru.morozovit.android.link
import ru.morozovit.android.right
import ru.morozovit.android.unsupported
import ru.morozovit.android.widthSizeClass
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.services.UpdateChecker
import ru.morozovit.ultimatesecurity.ui.AuthActivity.Companion.started
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.ABOUT
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.APK_EXTRACTOR
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.APP_LOCKER
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.HOME
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.SETTINGS
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.SHORTCUTS
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.TILES
import ru.morozovit.ultimatesecurity.ui.MainActivity.Screen.Companion.UNLOCK_PROTECTION
import ru.morozovit.ultimatesecurity.ui.customization.TilesScreen
import ru.morozovit.ultimatesecurity.ui.customization.shortcuts.ShortcutsScreen
import ru.morozovit.ultimatesecurity.ui.main.AboutScreen
import ru.morozovit.ultimatesecurity.ui.main.HomeScreen
import ru.morozovit.ultimatesecurity.ui.main.SettingsScreen
import ru.morozovit.ultimatesecurity.ui.protection.applocker.ApplockerScreen
import ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.UnlockProtectionScreen
import ru.morozovit.ultimatesecurity.ui.tools.APKExtractorScreen

class MainActivity : BaseActivity(
    backButtonBehavior = Companion.BackButtonBehavior.DEFAULT,
    savedInstanceStateEnabled = true
) {
    private var prevConfig: Configuration? = null

    lateinit var activityLauncher: ActivityLauncher

    val resumeHandlers = mutableListOf<() -> Unit>()

    private var isLockVisible by mutableStateOf(false)

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
    @Serializable sealed class Screen(
        val internalName: String,
        @StringRes val displayName: Int,
        @Transient val icon: ImageVector = unsupported
    ): BaseScreen() {
        companion object {
            operator fun get(name: String) = when (name) {
                HOME -> Home
                SETTINGS -> Settings
                ABOUT -> About
                APP_LOCKER -> AppLocker
                UNLOCK_PROTECTION -> UnlockProtection
                TILES -> Tiles
                SHORTCUTS -> Shortcuts
                APK_EXTRACTOR -> APKExtractor
                else -> {
                    Log.e("MainActivity", "Unknown screen index: $name")
                    null
                }
            }

            const val HOME = "home"
            const val SETTINGS = "settings"
            const val ABOUT = "about"

            const val APP_LOCKER = "appLocker"
            const val UNLOCK_PROTECTION = "unlockProtection"

            const val TILES = "tiles"
            const val SHORTCUTS = "shortcuts"

            const val APK_EXTRACTOR = "apkExtractor"
        }

        override fun equals(other: Any?) = other is Screen && displayName == other.displayName

        override fun hashCode() = displayName.hashCode()

        class Label(@StringRes val label: Int): BaseScreen()

        @Serializable data object Home: Screen(HOME, R.string.home, Icons.Filled.Home)
        @Serializable data object Settings: Screen(SETTINGS, R.string.settings, Icons.Filled.Settings)
        @Serializable data object About: Screen(ABOUT, R.string.about, Icons.Filled.Info)

        @Serializable data object AppLocker: Screen(APP_LOCKER, R.string.applocker, Icons.Outlined.PhonelinkLock)
        @Serializable data object UnlockProtection: Screen(UNLOCK_PROTECTION, R.string.unlock_protection, Icons.Filled.Lock)

        @Serializable data object Tiles: Screen(TILES, R.string.tiles, Icons.Filled.Apps)
        @Serializable data object Shortcuts: Screen(SHORTCUTS, R.string.shortcuts, Icons.AutoMirrored.Filled.Shortcut)

        @Serializable data object APKExtractor: Screen(APK_EXTRACTOR, R.string.apkextractor, Icons.Filled.Android)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @PhonePreview
    fun MainScreen() {
        AppTheme(consumeLeftInsets = true, consumeRightInsets = true) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            var selectedItem by rememberSaveable { mutableStateOf(HOME) }
            val navController = rememberNavController()

            val currentEntry = navController.currentBackStackEntryAsState()

            LaunchedEffect(Unit) {
                snapshotFlow { currentEntry.value }.collect {
                    runCatching {
                        selectedItem = it!!.destination.route!!
                    }
                }
            }

            val isScreenBig = currentWindowAdaptiveInfo().widthSizeClass >= WidthSizeClass.MEDIUM

            val drawerContent: @Composable ColumnScope.() -> Unit = {
                Column(Modifier
                    .verticalScroll(rememberScrollState())
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
                    val items = listOf(
                        Screen.Home,
                        Screen.Settings,
                        Screen.About,
                        Screen.Label(R.string.security),
                        Screen.AppLocker,
                        Screen.UnlockProtection,
                        Screen.Label(R.string.customization),
                        Screen.Tiles,
                        Screen.Shortcuts,
                        Screen.Label(R.string.tools),
                        Screen.APKExtractor
                    )
                    items.forEach { item ->
                        when (item) {
                            is Screen -> NavigationDrawerItem(
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.displayName)) },
                                selected = item == Screen[selectedItem],
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (Screen[selectedItem] != item) {
                                        selectedItem = item.internalName
                                        navController.navigate(item.internalName)
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            is Screen.Label -> {
                                Text(
                                    text = stringResource(item.label),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 28.dp, end = 36.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            val content = @Composable {
                Scaffold(
                    topBar = {
                        TopAppBar(
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
                            navigationIcon = {
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
                            },
                            actions = {
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
                            },
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Right))
                        )
                    },
                ) { innerPadding ->
                    val start by remember { mutableStateOf(selectedItem) }
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Left))
                    ) {
                        composable(route = HOME) { HomeScreen() }
                        composable(route = SETTINGS) { SettingsScreen() }
                        composable(route = ABOUT) { AboutScreen() }

                        composable(route = APP_LOCKER) { ApplockerScreen() }
                        composable(route = UNLOCK_PROTECTION) { UnlockProtectionScreen() }

                        composable(route = TILES) { TilesScreen() }
                        composable(route = SHORTCUTS) { ShortcutsScreen() }

                        composable(route = APK_EXTRACTOR) { APKExtractorScreen() }
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
                            modifier = Modifier.widthIn(max = 360.dp)
                        )
                    },
                    content = content
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        val max0 = LocalConfiguration().screenWidthDp * 0.9
                        val max = when {
                            max0 > 360.0 -> 360.0
                            max0 < 300.0 -> 300.0
                            else -> max0
                        }

                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            drawerState = drawerState,
                            modifier = Modifier.widthIn(max = max.dp),
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

        /*if (intent.getBooleanExtra("anim", false)) {
            overridePendingTransition(R.anim.scale_down, R.anim.alpha_down)
        }*/

        activityLauncher = ActivityLauncher.registerActivityForResult(this)
        updateLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enableEdgeToEdge()
            window.isNavigationBarContrastEnforced = false
        }

        val content = ComposeView(this).apply {
            setContent {
                MainScreen()
            }
        }
        setContentView(content)

        // Start update checker
        try {
            UpdateChecker.schedule(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "${e::class.qualifiedName}: ${e.message}")
        }

        prevConfig = resources.configuration

        if (pendingAuth) {
            content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (started) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    return false
                }
            })
            /*finish()*/
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO rewrite in Jetpack Compose
                Snackbar.make(
                    content,
                    R.string.grant_notification,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.grant) {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                    }
                    .show()
            }
        }

        if (!pendingAuth /*&& !intent.getBooleanExtra("noAnim", false)*/) startEnterAnimation(content)
    }

    override fun finish() {
        super.finish()
        authenticated = false
    }

    override fun onResume() {
        super.onResume()
        if (!pendingAuth) updateLock()
        for (handler in resumeHandlers) {
            handler()
        }
    }


    fun updateLock() {
        isLockVisible = globalPassword != "" && globalPasswordEnabled
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
}