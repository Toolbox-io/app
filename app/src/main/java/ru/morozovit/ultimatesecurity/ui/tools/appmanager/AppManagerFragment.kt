package ru.morozovit.ultimatesecurity.ui.tools.appmanager

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.ListItem
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit) {
    // TODO implement
    WindowInsetsHandler {
        with (LocalContext()) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()

            val apps = remember { mutableStateListOf<PackageInfo>() }
            var searchInputState by remember { mutableStateOf("") }

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
                async {
                    val appsList = packageManager.getInstalledPackages(GET_ACTIVITIES).toMutableList()

                    val sorted = appsList.sortedBy {
                        it.applicationInfo?.loadLabel(packageManager).toString()
                    }

                    apps.addAll(sorted)

                    val toRemove = mutableListOf<PackageInfo>()

                    apps.forEach { app ->
                        if (app.activities.let { it.isNullOrEmpty() }) {
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
                            TopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.apkextractor),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = navigation,
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
                                    actions()
                                },
                                scrollBehavior = scrollBehavior
                            )
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
}

@Composable
@PhonePreview
private fun APKExtractorScreenPreview() {
    AppManagerScreen(actions = {}, navigation = {})
}