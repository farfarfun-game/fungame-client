package com.farfarfun.fungame.shell

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.farfarfun.fungame.BuildConfig
import com.farfarfun.fungame.core.NavDecision
import com.farfarfun.fungame.core.NavigationPolicy
import com.farfarfun.fungame.core.ServerAddress

/** 单 Activity:全屏 WebView 加载配置的服务地址。 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var server: ServerAddress
    private lateinit var policy: NavigationPolicy

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 构建期已校验过,这里解析失败只可能是代码被改坏,直接报错比白屏好
        server = requireNotNull(ServerAddress.parse(BuildConfig.SERVER_ADDRESS)) {
            "Invalid fungameServer: ${BuildConfig.SERVER_ADDRESS}"
        }
        policy = NavigationPolicy(server)

        webView = WebView(this)
        setContentView(webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // 安全基线:不读本地文件/content
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                when (policy.decide(request.url.toString())) {
                    NavDecision.ALLOW -> false
                    NavDecision.EXTERNAL -> { openExternal(request.url); true }
                    NavDecision.BLOCK -> true
                }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                // 渲染进程崩溃:重建 Activity,避免整个应用被带走
                recreate()
                return true
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
        if (savedInstanceState == null) webView.loadUrl(server.startUrl) else webView.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            // 没有可处理的应用:忽略
        }
    }
}
