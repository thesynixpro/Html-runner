package com.openprojects.htmlrunner.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.openprojects.htmlrunner.runner.JsBridge

private val PreviewBackground = Color(0xFFFFFFFF)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewPane(
    document: String,
    onConsoleMessage: (level: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestCallback = rememberUpdatedState(onConsoleMessage)

    val webView = remember(context) {
        WebView(context.applicationContext).apply {
            setBackgroundColor(AndroidColor.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.loadsImagesAutomatically = true
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            isFocusable = false
            isFocusableInTouchMode = false
            webViewClient = WebViewClient()
            addJavascriptInterface(
                JsBridge { level, text -> latestCallback.value(level, text) },
                "AndroidRunner",
            )
        }
    }

    Box(modifier = modifier.background(PreviewBackground)) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.tag != document) {
                    view.tag = document
                    view.loadDataWithBaseURL(null, document, "text/html", "utf-8", null)
                }
            },
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
}