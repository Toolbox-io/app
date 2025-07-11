package io.toolbox.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.ui.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.morozovit.android.evaluateJavascript
import ru.morozovit.android.invoke
import ru.morozovit.android.openUrl

class GuideActivity: BaseActivity(configTheme = false) {
    @Suppress("unused")
    class WebViewScrollInterface(
        private val onH1Scrolled: (Boolean) -> Unit,
        private val onTitleExtracted: (String) -> Unit
    ) {
        @JavascriptInterface
        fun onHeaderScrolled(scrolled: Boolean) = onH1Scrolled(scrolled)
        
        @JavascriptInterface
        fun onTitleExtractedCallback(title: String) = onTitleExtracted(title)
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun GuideScreen(uri: Uri) {
        AppTheme(darkTheme = true, dynamicColor = false) {
            val context = LocalContext()
            val scope = rememberCoroutineScope()

            var loading by remember { mutableStateOf(true) }
            var isScrolled by remember { mutableStateOf(false) }
            val topBarColor by animateColorAsState(
                if (isScrolled) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surface
            )
            var isH1Scrolled by remember { mutableStateOf(false) }
            var guideTitle by remember { mutableStateOf("") }

            var menu by remember { mutableStateOf(false) }
            var webview: WebView? by remember { mutableStateOf(null) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            AnimatedVisibility(
                                visible = isH1Scrolled,
                                enter = fadeIn(tween(500)),
                                exit = fadeOut(tween(500))
                            ) {
                                Text(
                                    text = guideTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                            Box {
                                IconButton(
                                    onClick = {
                                        menu = true
                                    }
                                ) {
                                    Icon(Icons.Filled.MoreVert, stringResource(R.string.more))
                                }
                                DropdownMenu(
                                    expanded = menu,
                                    onDismissRequest = { menu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                        onClick = {
                                            menu = false
                                            startActivity(
                                                Intent.createChooser(
                                                    Intent(Intent.ACTION_SEND).apply {
                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            "$uri".replace(
                                                                "https://(.*?)/guides/(\\w+)/raw".toRegex(),
                                                                "https://$1/guides/$2"
                                                            )
                                                        )
                                                        type = "text/plain"
                                                    },
                                                    null
                                                )
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                        onClick = {
                                            scope.launch {
                                                menu = false
                                                loading = true
                                                delay(500)
                                                webview!!.reload()
                                            }
                                        },
                                        enabled = webview != null
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarColor
                        )
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing.exclude(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                )
            ) { innerPadding ->
                AndroidView(
                    factory = {
                        WebView(it).apply {
                            webview = this

                            webViewClient = object: WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    if (request.url != uri) {
                                        context.openUrl(request.url)
                                        return true
                                    } else {
                                        return false
                                    }
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    
                                    // Inject JavaScript to detect h1 scroll and extract title
                                    evaluateJavascript(
                                        """
                                        (function() {
                                            const h1Elements = document.querySelectorAll('h1');
                                            
                                            if (h1Elements.length > 0) {
                                                const firstH1 = h1Elements[0];
                                                const title = firstH1.textContent || '';
                                                
                                                // Send title to Android
                                                Android.onTitleExtractedCallback(title);
                                                
                                                // Check initial scroll position
                                                const isInitiallyScrolled = firstH1.getBoundingClientRect().bottom <= 0;
                                                Android.onHeaderScrolled(isInitiallyScrolled);
                                                
                                                // Listen for scroll events
                                                window.addEventListener('scroll', function() {
                                                    const currentRect = firstH1.getBoundingClientRect();
                                                    const isScrolledPast = currentRect.bottom <= 0;
                                                    Android.onHeaderScrolled(isScrolledPast);
                                                });
                                            }
                                        })();
                                        """
                                    )
                                }
                            }

                            webChromeClient = object: WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)

                                    scope.launch {
                                        if (newProgress == 100) {
                                            delay(1000)
                                            loading = false
                                        }
                                    }
                                }
                        
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                useWideViewPort = true
                            }

                            // Listen for scroll changes
                            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                                isScrolled = scrollY > 0
                            }

                            // Add JavaScript interface
                            addJavascriptInterface(
                                WebViewScrollInterface(
                                    onH1Scrolled = { scrolled ->
                                        isH1Scrolled = scrolled
                                    },
                                    onTitleExtracted = { title ->
                                        guideTitle = title
                                    }
                                ),
                                "Android"
                            )

                            loadUrl("$uri")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
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

        val uri = intent.data ?: run {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            GuideScreen(uri)
        }
    }
}