package io.toolbox.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.databinding.GuideActivityBinding
import ru.morozovit.android.openUrl

class GuideActivity: BaseActivity(configTheme = false) {
    private lateinit var binding: GuideActivityBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GuideActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.data == null) {
            return
        }

        intent.data!!.let { uri ->
            binding.guideToolbar.apply {
                setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.guide_share -> {
                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, "$uri")
                                        type = "text/plain"
                                    },
                                    null
                                )
                            )
                        }
                    }
                    true
                }
            }

            binding.guideWebview.apply {
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
                settings.apply {
                    javaScriptEnabled = true
                    useWideViewPort = true
                }
                loadUrl("$uri")
            }
        }
    }
}