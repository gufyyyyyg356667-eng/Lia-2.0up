package com.Lia.assistant.ui.screens.forge

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Shows a generated page in a sandboxed WebView: scripts on, no JavaScript bridge, no file or content
 * access, mixed content blocked, geolocation off. Links that leave the page open in the browser.
 */
@Composable
fun GeneratedSiteView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setGeolocationEnabled(false)
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (uri.host == "localhost") return false
                        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { view.context.startActivity(intent) }
                        return true
                    }
                }
            }
        },
        update = { web ->
            // Reload only when the page itself changes, not on every recomposition.
            if (web.tag != html) {
                web.tag = html
                web.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
    )
}
