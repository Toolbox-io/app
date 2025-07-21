package io.toolbox.ui.main

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.api.Utils
import io.toolbox.ui.AppTheme
import ru.morozovit.android.utils.getRawText
import ru.morozovit.android.utils.ui.verticalScroll

class OSSLicensesActivity: BaseActivity() {
    data class License(
        val name: String,
        var text: String,
        var website: Boolean = false
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    @Composable
    fun OpenSourceLicensesScreen() {
        AppTheme {
            val navController = rememberNavController()
            val licenses = remember { mutableStateListOf<License>() }
            var loading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                val licensesData = resources.getRawText(R.raw.third_party_licenses)

                val downloadedLicenses = mutableMapOf<String, String>()

                resources
                    .openRawResource(R.raw.third_party_license_metadata)
                    .bufferedReader()
                    .useLines {
                        it.forEach { line ->
                            val (offset, length) = line
                                .substringBefore(' ')
                                .split(":")
                                .map { it.toInt() }

                            licenses += License(
                                licensesData.substring(
                                    offset,
                                    (offset + length).coerceAtMost(licensesData.lastIndex)
                                ),
                                line.substringAfter(' ')
                            )
                        }
                    }

                licenses.forEach { license ->
                    if (license.text.startsWith("http://") || license.text.startsWith("https://")) {
                        if (license.text in downloadedLicenses.keys) {
                            license.text = downloadedLicenses[license.text]!!
                        }

                        Utils.download(license.text)?.let {
                            if (it.trim().startsWith("<!DOCTYPE", true)) {
                                license.website = true
                            } else {
                                downloadedLicenses[license.text] = it
                                license.text = it
                            }
                        }
                    }
                }

                loading = false
            }

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            MediumTopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.osl),
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
                                scrollBehavior = scrollBehavior
                            )
                        },
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            if (loading) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                )
                            }

                            LazyColumn {
                                items(licenses.size) { index ->
                                    val it = licenses[index]
                                    Column {
                                        Box(
                                            Modifier
                                                .clickable {
                                                    navController.navigate("license/$index")
                                                }
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = it.name,
                                                fontSize = 20.sp,
                                                modifier = Modifier
                                                    .padding(16.dp)
                                            )
                                        }
                                        if (index != licenses.size - 1) {
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                composable(
                    "license/{i}",
                    arguments = listOf(
                        navArgument("i") { type = NavType.IntType }
                    )
                ) {
                    val i = it.arguments!!.getInt("i")
                    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = licenses[i].name
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                }
                            )
                        },
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    ) { innerPadding ->
                        if (licenses[i].website) {
                            AndroidView(
                                factory = {
                                    WebView(it).apply {
                                        loadUrl(licenses[i].text)
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            Box(
                                Modifier
                                    .padding(innerPadding)
                                    .verticalScroll()
                            ) {
                                SelectionContainer {
                                    Text(
                                        modifier = Modifier.padding(16.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        text = licenses[i].text
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenSourceLicensesScreen()
        }
    }
}