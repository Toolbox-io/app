package io.toolbox.ui.main

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import io.toolbox.download
import io.toolbox.ui.AppTheme
import ru.morozovit.android.utils.ui.verticalScroll
import kotlin.concurrent.thread

class OSSLicensesActivity: BaseActivity() {
    data class LicenseComponent(
        val name: String,
        var license: String
    ) {
        var website = false
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    @Composable
    fun OpenSourceLicensesScreen() {
        AppTheme {
            SharedTransitionLayout {
                val navController = rememberNavController()
                val licenses = remember { mutableStateListOf<LicenseComponent>() }
                var loading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    val licensesData = resources
                        .openRawResource(R.raw.third_party_licenses)
                        .bufferedReader()
                        .use { it.readText() }

                    resources
                        .openRawResource(R.raw.third_party_license_metadata)
                        .bufferedReader()
                        .useLines {
                            val downloadedLicenses = mutableMapOf<String, String>()

                            it.forEach { line ->
                                val offsetLength = line.substringBefore(' ').split(":")
                                val offset = offsetLength[0].toInt()
                                val length = offsetLength[1].toInt()
                                Log.d("OSSActivity", "${offset}:${length}")

                                val chars = mutableListOf<Char>()
                                for (i in offset until offset + length) {
                                    try {
                                        chars.add(licensesData[i])
                                    } catch (_: StringIndexOutOfBoundsException) {}
                                }
                                val license = chars.joinToString("")
                                Log.d("OSSActivity", license)

                                val name = line.substringAfter(' ')
                                Log.d("OSSActivity", name)

                                licenses += LicenseComponent(name, license)
                            }
                            thread {
                                licenses.forEach { i ->
                                    if (i.license.startsWith("http://") || i.license.startsWith("https://")) {
                                        if (i.license in downloadedLicenses.keys) {
                                            i.license = downloadedLicenses[i.license]!!
                                        }
                                        val downloadedLicense = download(i.license)
                                        if (downloadedLicense != null) {
                                            if (downloadedLicense.trim().startsWith("<!DOCTYPE html>", true)) {
                                                i.website = true
                                            } else {
                                                downloadedLicenses[i.license] = downloadedLicense
                                                i.license = downloadedLicense
                                            }
                                        }
                                    }
                                }
                                loading = false
                            }
                        }
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
                        arguments = listOf(navArgument("i") { type = NavType.IntType })
                    ) { bse ->
                        val i = bse.arguments!!.getInt("i")
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
                                            loadUrl(licenses[i].license)
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
                                    Text(
                                        modifier = Modifier.padding(16.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        text = licenses[i].license
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