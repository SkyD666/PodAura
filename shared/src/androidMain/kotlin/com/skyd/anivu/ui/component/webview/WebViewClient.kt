package com.skyd.anivu.ui.component.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import co.touchlab.kermit.Logger
import com.skyd.anivu.ext.isUrl
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URI


class WebViewClient(
    private val refererDomain: String?,
    private val onOpenLink: (url: String) -> Unit,
) : WebViewClient() {
    private val log = Logger.withTag("WebViewClient")

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString()
        if (url != null && url.isUrl) {
            try {
                var connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                if (connection.responseCode == 403) {
                    connection.disconnect()
                    connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                    connection.setRequestProperty("Referer", refererDomain)
                    val inputStream = DataInputStream(connection.inputStream)
                    return WebResourceResponse(connection.contentType, "UTF-8", inputStream)
                }
            } catch (e: Exception) {
                log.e("shouldInterceptRequest url: $e")
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        val jsCode = """
            javascript:(function() {
                var imgs = document.getElementsByTagName("img");
                for (var i = 0; i < imgs.length; i++) {
                    imgs[i].pos = i;
                    imgs[i].onclick = function(event) {
                        event.preventDefault();
                        window.${JavaScriptInterface.NAME}.onImageClick(this.src, this.alt);
                    }
                }
            })()
            """
        view.loadUrl(jsCode)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (null == request?.url) return false
        val url = request.url.toString()
        if (url.isNotEmpty()) onOpenLink(url)
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        log.e("onReceivedError: $error")
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
    }
}
