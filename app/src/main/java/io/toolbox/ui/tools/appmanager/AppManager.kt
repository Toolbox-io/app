package io.toolbox.ui.tools.appmanager

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.toolbox.R
import kotlinx.coroutines.launch
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        with(LocalContext()) {
            val loadingStr = stringResource(R.string.loading)

            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val focusRequester = remember { FocusRequester() }

            val apps = remember { mutableStateListOf<PackageInfo>() }
            var searchInputState by remember { mutableStateOf("") }

            var loading by remember { mutableStateOf(true) }

            fun LazyListScope.apps(list: List<PackageInfo>, container: Boolean = false) {
                items(list.size) {
                    var appName by remember { mutableStateOf("") }
                    var appPackage: String? by remember { mutableStateOf("") }
                    var appIcon: ImageBitmap? by remember { mutableStateOf(null) }

                    var compose by remember { mutableStateOf(true) }

                    LaunchedEffect(list[it]) {
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

                    if (compose) {
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
                            onClick = {
                                startActivity(
                                    Intent(this@with, AppInfoActivity::class.java).apply {
                                        putExtra("appPackage", appPackage)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                thread {
                    val appsList = packageManager.getInstalledPackages(GET_ACTIVITIES).toMutableList()

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

                    loading = false
                }
            }

            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    val snackbarHostState = remember { SnackbarHostState() }

                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.app_manager),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = navigation,
                                actions = {
                                    IconButton(
                                        onClick = {
                                            if (loading) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(loadingStr)
                                                }
                                            } else {
                                                navController.navigate("search")
                                            }
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
                        }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            if (loading) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopStart)
                                )
                            }
                            LazyColumn {
                                apps(apps)
                            }
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
                                }
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
}