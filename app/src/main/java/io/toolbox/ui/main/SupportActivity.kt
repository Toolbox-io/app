package io.toolbox.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.ui.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.morozovit.android.utils.openUrl
import ru.morozovit.android.utils.settings
import ru.morozovit.android.utils.ui.invoke

class SupportActivity: BaseActivity(configTheme = false) {
    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun SupportScreen() {
        AppTheme(darkTheme = true, dynamicColor = false) {
            val context = LocalContext()
            val scope = rememberCoroutineScope()
            val uri = "http://192.168.1.16:8000/support?iframe=true".toUri()

            var loading by remember { mutableStateOf(true) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Чат с поддержкой")
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
                contentWindowInsets = WindowInsets.safeDrawing.exclude(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                )
            ) { innerPadding ->
                AndroidView(
                    factory = {
                        WebView(it).apply {
                            webViewClient = object: WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    if (request.url != uri) {
                                        context.openUrl(request.url)
                                        return true
                                    } else {
                                        return false
                                    }
                                }
                            }

                            webChromeClient = object: WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)

                                    scope.launch {
                                        if (newProgress == 100) {
                                            delay(1000)
                                            loading = false
                                            
                                            // Set actual viewport height for Android WebView
                                            view.evaluateJavascript(
                                                """
                                                (() => {
                                                    function setHeight() {
                                                        const style = innerHeight + "px";
                                                        document.body.style.height = style;
                                                        document.documentElement.style.height = style;
                                                    }
                                                    
                                                    setHeight();
                                                    window.addEventListener('resize', setHeight);
                                                    window.addEventListener('orientationchange', setHeight);
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }
                                    }
                                }
                            }

                            settings {
                                javaScriptEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                useWideViewPort = true
                            }

                            loadUrl("$uri")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(innerPadding)
                )
            }

            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(Modifier.size(75.dp))
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SupportScreen()
        }
    }
}