package com.skyd.podaura.model.repository.fullcontent

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

internal class AndroidRenderedPageProvider(
    context: Context,
    private val json: Json,
) : RenderedPageProvider {
    private val applicationContext = context.applicationContext
    private val renderMutex = Mutex()

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun render(url: String): RenderedPageSnapshot = renderMutex.withLock {
        withContext(Dispatchers.Main) {
            withTimeout(RENDER_TIMEOUT_MILLIS.milliseconds) {
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    throw RenderedPageException("Isolated WebView profiles are unavailable")
                }
                val profileName = "podaura-render-${Uuid.random().toHexString()}"
                val webView = WebView(applicationContext)
                try {
                    WebViewCompat.setProfile(webView, profileName)
                    configure(webView)
                    awaitPageLoad(webView, url)
                    primeLazyContent(webView)
                    val observerKey = "__podaura_${Uuid.random().toHexString()}"
                    awaitDomStability(webView, observerKey)
                    val snapshotPayload = evaluate(webView, RenderedPageSnapshotScript.snapshot)
                    val snapshot = parseSnapshot(snapshotPayload)
                    if (snapshot.html.length > RenderedPageSnapshotScript.MAX_HTML_CHARS) {
                        throw RenderedPageException("Rendered article is too large")
                    }
                    snapshot
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: RenderedPageException) {
                    throw exception
                } catch (exception: Throwable) {
                    throw RenderedPageException("Unable to render article page", exception)
                } finally {
                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.clearHistory()
                    webView.clearFormData()
                    webView.destroy()
                    runCatching { ProfileStore.getInstance().deleteProfile(profileName) }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
            databaseEnabled = false
            mediaPlaybackRequiresUserGesture = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        val width = View.MeasureSpec.makeMeasureSpec(RENDER_VIEWPORT_WIDTH, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(RENDER_VIEWPORT_HEIGHT, View.MeasureSpec.EXACTLY)
        webView.measure(width, height)
        webView.layout(0, 0, RENDER_VIEWPORT_WIDTH, RENDER_VIEWPORT_HEIGHT)
    }

    private suspend fun awaitPageLoad(webView: WebView, url: String) {
        suspendCancellableCoroutine { continuation ->
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = request.isForMainFrame && !request.url.toString().isHttpOrHttpsUrl()

                @Deprecated("Deprecated in Android")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    !url.isHttpOrHttpsUrl()

                override fun onPageFinished(view: WebView, url: String?) {
                    if (continuation.isActive && url?.isHttpOrHttpsUrl() == true) {
                        continuation.resume(Unit)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame && continuation.isActive) {
                        continuation.resumeWithException(
                            RenderedPageException("Page load failed: ${error.errorCode}")
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame && continuation.isActive && errorResponse.statusCode >= 400) {
                        continuation.resumeWithException(
                            RenderedPageException("HTTP ${errorResponse.statusCode}")
                        )
                    }
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: SslError,
                ) {
                    handler.cancel()
                    if (continuation.isActive) {
                        continuation.resumeWithException(RenderedPageException("TLS error"))
                    }
                }
            }
            continuation.invokeOnCancellation { webView.stopLoading() }
            webView.loadUrl(url)
        }
    }

    private suspend fun awaitDomStability(webView: WebView, observerKey: String) {
        val startedAt = SystemClock.elapsedRealtime()
        delay(MINIMUM_RENDER_DELAY_MILLIS.milliseconds)
        while (SystemClock.elapsedRealtime() - startedAt < DOM_STABILITY_TIMEOUT_MILLIS) {
            val payload = evaluate(
                webView,
                RenderedPageSnapshotScript.stabilityState(observerKey),
            )
            val state = runCatching {
                json.parseToJsonElement(payload) as? JsonObject
            }.getOrNull()
            val ready = (state?.get("ready") as? JsonPrimitive)?.contentOrNull
            val quietMillis = (state?.get("quietMillis") as? JsonPrimitive)?.longOrNull ?: 0L
            if (ready == "complete" && quietMillis >= DOM_QUIET_MILLIS) return
            delay(DOM_POLL_MILLIS.milliseconds)
        }
    }

    private suspend fun primeLazyContent(webView: WebView) {
        repeat(MAX_LAZY_LOAD_STEPS) {
            if (evaluate(webView, RenderedPageSnapshotScript.lazyLoadStep) == "done") {
                return
            }
            delay(LAZY_LOAD_STEP_DELAY_MILLIS.milliseconds)
        }
        evaluate(webView, "window.scrollTo(0, 0); 'done'")
    }

    private suspend fun evaluate(webView: WebView, script: String): String =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { encodedResult ->
                if (!continuation.isActive) return@evaluateJavascript
                val decoded = runCatching {
                    json.decodeFromString<String>(encodedResult)
                }.getOrNull()
                if (decoded == null) {
                    continuation.resumeWithException(
                        RenderedPageException("Invalid rendered page snapshot")
                    )
                } else {
                    continuation.resume(decoded)
                }
            }
        }

    private fun parseSnapshot(payload: String): RenderedPageSnapshot {
        val value = runCatching { json.parseToJsonElement(payload) as? JsonObject }.getOrNull()
            ?: throw RenderedPageException("Invalid rendered page snapshot")
        val html = (value["html"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: throw RenderedPageException("Rendered page is empty")
        val finalUrl = (value["url"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf(String::isHttpOrHttpsUrl)
            ?: throw RenderedPageException("Rendered page has an unsafe URL")
        return RenderedPageSnapshot(html = html, finalUrl = finalUrl)
    }
}

private fun String.isHttpOrHttpsUrl(): Boolean = runCatching {
    Url(this).protocol.name in setOf("http", "https")
}.getOrDefault(false)

private const val RENDER_TIMEOUT_MILLIS = 18_000L
private const val MINIMUM_RENDER_DELAY_MILLIS = 300L
private const val DOM_STABILITY_TIMEOUT_MILLIS = 8_000L
private const val DOM_QUIET_MILLIS = 800L
private const val DOM_POLL_MILLIS = 250L
private const val MAX_LAZY_LOAD_STEPS = 24
private const val LAZY_LOAD_STEP_DELAY_MILLIS = 100L
private const val RENDER_VIEWPORT_WIDTH = 1080
private const val RENDER_VIEWPORT_HEIGHT = 1920
