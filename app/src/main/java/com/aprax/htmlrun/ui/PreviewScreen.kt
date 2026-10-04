package com.aprax.htmlrun.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.aprax.htmlrun.AppViewModel
import java.io.File

private const val PREVIEW_HOST = "appassets.androidplatform.net"

/**
 * Full screen preview. The project is mirrored into app storage and served over https so
 * relative paths, styles, scripts and images behave exactly like they would on a server.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val directory = remember(viewModel.previewGeneration) { viewModel.previewDirectory() }
    val entryPage = remember(viewModel.previewGeneration) { viewModel.entryPage }
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val javaScriptEnabled = viewModel.settings.javaScriptEnabled
    val assetLoader = remember(directory) {
        directory?.let {
            WebViewAssetLoader.Builder()
                .setDomain(PREVIEW_HOST)
                .addPathHandler(
                    "/project/",
                    WebViewAssetLoader.InternalStoragePathHandler(context, it),
                )
                .build()
        }
    }

    val webView = remember(directory, javaScriptEnabled) {
        WebView(context).apply {
            setBackgroundColor(backgroundColor)
            settings.javaScriptEnabled = javaScriptEnabled
            settings.domStorageEnabled = javaScriptEnabled
            settings.allowFileAccess = assetLoader == null
            settings.allowContentAccess = false
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
    }

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    BackHandler(enabled = canGoBack) { webView.goBack() }

    LaunchedEffect(webView, directory, entryPage, assetLoader) {
        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                val loader = assetLoader ?: return null
                if (request.url.host != PREVIEW_HOST) return null
                return loader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                canGoBack = view.canGoBack()
                canGoForward = view.canGoForward()
            }
        }

        val page = entryPage ?: "index.html"
        val url = if (assetLoader != null) {
            "https://$PREVIEW_HOST/project/$page"
        } else {
            val root = directory ?: return@LaunchedEffect
            "file://${File(root, page).absolutePath}"
        }
        webView.loadUrl(url)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Back to editor")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entryPage?.let { "/$it" } ?: "No HTML file",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { if (webView.canGoBack()) webView.goBack() },
                enabled = canGoBack,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            IconButton(
                onClick = { if (webView.canGoForward()) webView.goForward() },
                enabled = canGoForward,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = viewModel::reloadPreview) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
            }
        }

        if (directory == null || entryPage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nothing to preview yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "This project needs at least one HTML file",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
