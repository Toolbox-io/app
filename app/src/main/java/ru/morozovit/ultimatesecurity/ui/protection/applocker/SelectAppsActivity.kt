package ru.morozovit.ultimatesecurity.ui.protection.applocker

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import ru.morozovit.android.backCallback
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.ui.AppTheme

class SelectAppsActivity: BaseActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SelectAppsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()

            val selectedApps = remember { mutableStateSetOf<String>() }
            val apps = remember { mutableStateListOf<PackageInfo>() }

            var unsavedChanges by remember { mutableStateOf(false) }

            var exitConfirmationExpanded by remember { mutableStateOf(false) }
            var resetConfirmationExpanded by remember { mutableStateOf(false) }

            val backCallback = backCallback(false) {
                exitConfirmationExpanded = true
            }

            var searchInputState by remember { mutableStateOf("") }

            onBackPressedDispatcher.addCallback(backCallback)

            LaunchedEffect(Unit) {
                snapshotFlow { unsavedChanges }.collect {
                    backCallback.isEnabled = it
                }
            }

            fun onExitConfirmDismiss() {
                exitConfirmationExpanded = false
            }
            fun onResetConfirmDismiss() {
                resetConfirmationExpanded = false
            }
            fun saveChangesAndFinish() {
                Settings.Applocker.apps = selectedApps.toSet()
                finish()
            }
            fun updateUnsavedChanges() {
                unsavedChanges = selectedApps.toSet() != Settings.Applocker.apps.toSet()
            }
            fun discardChanges() {
                coroutineScope.launch {
                    selectedApps.clear()
                    selectedApps.addAll(Settings.Applocker.apps)
                    updateUnsavedChanges()
                }
            }

            SimpleAlertDialog(
                open = exitConfirmationExpanded,
                onDismissRequest = ::onExitConfirmDismiss,
                title = stringResource(R.string.are_you_sure),
                body = stringResource(R.string.unsaved_changes),
                onPositiveButtonClick = ::saveChangesAndFinish,
                onNegativeButtonClick = ::finish,
                positiveButtonText = stringResource(R.string.save),
                negativeButtonText = stringResource(R.string.discard),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.are_you_sure)
                    )
                }
            )
            SimpleAlertDialog(
                open = resetConfirmationExpanded,
                onDismissRequest = ::onResetConfirmDismiss,
                title = stringResource(R.string.are_you_sure),
                body = stringResource(R.string.discard_d),
                onPositiveButtonClick = {
                    discardChanges()
                    onResetConfirmDismiss()
                },
                onNegativeButtonClick = ::onResetConfirmDismiss,
                positiveButtonText = stringResource(R.string.yes),
                negativeButtonText = stringResource(R.string.no),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.are_you_sure)
                    )
                }
            )

            fun LazyListScope.apps(list: List<PackageInfo>, container: Boolean = false) {
                items(list.size) {
                    var appName by remember { mutableStateOf("") }
                    var appPackage: String? by remember { mutableStateOf("") }
                    var appIcon: ImageBitmap? by remember { mutableStateOf(null) }

                    var compose by remember { mutableStateOf(true) }

                    LaunchedEffect(list[it]) {
                        coroutineScope.launch {
                            val info = list[it].applicationInfo
                            if (info != null) {
                                appName = info.loadLabel(packageManager).toString()
                                appPackage = if (appName != list[it].packageName) {
                                    list[it].packageName
                                } else {
                                    null
                                }
                                appIcon = info.loadIcon(packageManager).toBitmap().asImageBitmap()
                            } else {
                                compose = false
                            }
                        }
                    }

                    if (compose) {
                        fun onCheckedChange(it: Boolean) {
                            if (it) {
                                selectedApps.add(appPackage ?: appName)
                            } else {
                                selectedApps.remove(appPackage ?: appName)
                            }
                            coroutineScope.launch {
                                updateUnsavedChanges()
                            }
                        }

                        ListItem(
                            headline = appName,
                            supportingText = appPackage,
                            divider = !container,
                            leadingContent = {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon!!,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(30.dp))
                                    )
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = selectedApps.contains(appPackage ?: appName),
                                    onCheckedChange = ::onCheckedChange
                                )
                            },
                            onClick = {
                                onCheckedChange(!selectedApps.contains(appPackage ?: appName))
                            }
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val appsList = packageManager.getInstalledPackages(GET_ACTIVITIES).toMutableList()
                    selectedApps.addAll(Settings.Applocker.apps)

                    val sorted = appsList.sortedBy {
                        it.applicationInfo?.loadLabel(packageManager).toString()
                    }

                    apps.addAll(sorted)

                    val toRemove = mutableListOf<PackageInfo>()

                    apps.forEach { app ->
                        if (app.activities.isNullOrEmpty()) {
                            toRemove += app
                        }
                    }

                    toRemove.forEach {
                        apps.remove(it)
                    }
                }
            }

            val focusRequester = remember { FocusRequester() }

            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            MediumTopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.select_apps),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                },
                                actions = {
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
                                    Box {
                                        var expanded by remember { mutableStateOf(false) }

                                        fun onDismissRequest() {
                                            expanded = false
                                        }

                                        IconButton(onClick = {
                                            expanded = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = stringResource(R.string.more)
                                            )
                                        }

                                        DropdownMenu(expanded = expanded, onDismissRequest = ::onDismissRequest) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.select_all)) },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        apps.forEach {
                                                            selectedApps.add(it.packageName)
                                                        }
                                                        updateUnsavedChanges()
                                                    }
                                                    onDismissRequest()
                                                },
                                                leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.clear)) },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        selectedApps.clear()
                                                        updateUnsavedChanges()
                                                    }
                                                    onDismissRequest()
                                                },
                                                leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null) },
                                                enabled = selectedApps.isNotEmpty()
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.discard_changes)) },
                                                onClick = {
                                                    resetConfirmationExpanded = true
                                                    onDismissRequest()
                                                },
                                                leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
                                                enabled = unsavedChanges
                                            )
                                        }
                                    }
                                },
                                scrollBehavior = scrollBehavior
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = ::saveChangesAndFinish,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.done)
                                )
                            }
                        }
                    ) { innerPadding ->
                        LazyColumn(
                            contentPadding = innerPadding
                        ) {
                            apps(apps)
                        }
                    }
                }

                composable("search") {
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
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        }
                    ) { innerPadding ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                contentPadding = innerPadding
                            ) {
                                apps(
                                    list = if (searchInputState == "")
                                        mutableListOf()
                                    else {
                                        val words = searchInputState.split(" ")
                                        apps.filter {
                                            val result1 = run {
                                                var matches = 0
                                                val filt = it
                                                    .applicationInfo
                                                    ?.loadLabel(packageManager)
                                                    .toString()
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
                                                val filt = it.packageName
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
                                    },
                                    container = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SelectAppsScreen()
        }
    }
}