package org.example.mywebapp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.mywebapp.ui.screens.SettingsActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 백 버튼 처리를 위한 콜백 등록
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("MyWebApp") },
                                actions = {
                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "메뉴")
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("설정") },
                                            onClick = {
                                                startActivity(
                                                    Intent(
                                                        this@MainActivity,
                                                        SettingsActivity::class.java
                                                    )
                                                )
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            var refreshing by remember { mutableStateOf(false) }
                            val coroutineScope = rememberCoroutineScope()

                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { context ->
                                    SwipeRefreshLayout(context).apply {
                                        setOnRefreshListener {
                                            coroutineScope.launch {
                                                refreshing = true
                                                // 웹뷰 로딩 후 딜레이
                                                webView.reload()
                                                delay(500)

                                                refreshing = false
                                            }
                                        }
                                        // 웹뷰를 SwipeRefreshLayout의 자식으로 추가
                                        addView(WebView(context).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            webViewClient = object : CustomWebViewClient() {
                                                override fun onPageStarted(
                                                    view: WebView?,
                                                    url: String?,
                                                    favicon: Bitmap?
                                                ) {
                                                    super.onPageStarted(view, url, favicon)
                                                    // 웹뷰 로딩 시작시 처리
                                                }

                                                override fun onPageFinished(
                                                    view: WebView?,
                                                    url: String?
                                                ) {
                                                    super.onPageFinished(view, url)
                                                    // 웹뷰 로딩 완료시 처리
                                                }
                                            }

                                            webChromeClient = WebChromeClient()

                                            settings.apply {
                                                // 기본 설정
                                                javaScriptEnabled = context.getSharedPreferences(
                                                    "AppSettings",
                                                    Context.MODE_PRIVATE
                                                )
                                                    .getBoolean("javascriptEnabled", true)
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                loadsImagesAutomatically = true

                                                // 보안 설정
                                                mixedContentMode =
                                                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                                allowFileAccess = false
                                                allowContentAccess = false

                                                // 성능 설정
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                                setGeolocationEnabled(false)
                                            }

                                            // XSS 방어
                                            clearCache(true)
                                            clearHistory()

                                            loadUrl("https://www.google.com")
                                            // 웹뷰 생성 후 MainActivity의 webView에 할당
                                            this@MainActivity.webView = this
                                        })
                                    }
                                },
                                update = { swipeRefreshLayout ->
                                    swipeRefreshLayout.isRefreshing = refreshing
                                }
                            )

                        }
                    }
                }
            }
        }
    }
}

private open class CustomWebViewClient : WebViewClient() {
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // 에러 처리 로직을 여기에 추가할 수 있습니다
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        return false // 모든 URL을 WebView 내에서 로드
    }
}