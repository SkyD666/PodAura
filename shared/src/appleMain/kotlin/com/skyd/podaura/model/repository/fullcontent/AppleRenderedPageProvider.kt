package com.skyd.podaura.model.repository.fullcontent

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
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

internal class AppleRenderedPageProvider(
    private val json: Json,
) : RenderedPageProvider {
    private val renderMutex = Mutex()

    override suspend fun render(url: String): RenderedPageSnapshot = renderMutex.withLock {
        withContext(Dispatchers.Main) {
            withTimeout(RENDER_TIMEOUT_MILLIS.milliseconds) {
                val configuration = WKWebViewConfiguration().apply {
                    websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
                    preferences.javaScriptCanOpenWindowsAutomatically = false
                    defaultWebpagePreferences.allowsContentJavaScript = true
                }
                val webView = WKWebView(
                    frame = CGRectMake(0.0, 0.0, RENDER_VIEWPORT_WIDTH, RENDER_VIEWPORT_HEIGHT),
                    configuration = configuration,
                )
                try {
                    awaitPageLoad(webView, url)
                    primeLazyContent(webView)
                    val observerKey = "__podaura_${Uuid.random().toHexString()}"
                    awaitDomStability(webView, observerKey)
                    val payload = evaluate(webView, RenderedPageSnapshotScript.snapshot)
                    val snapshot = parseSnapshot(payload)
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
                    webView.navigationDelegate = null
                    webView.UIDelegate = null
                }
            }
        }
    }

    private suspend fun awaitPageLoad(webView: WKWebView, url: String) {
        val nsUrl = NSURL.URLWithString(url)
            ?: throw RenderedPageException("Invalid article URL")
        webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        val startedAt = TimeSource.Monotonic.markNow()
        do {
            delay(PAGE_LOAD_POLL_MILLIS.milliseconds)
            if (startedAt.elapsedNow().inWholeMilliseconds >= PAGE_LOAD_TIMEOUT_MILLIS) {
                throw RenderedPageException("Page load timed out")
            }
        } while (webView.loading)

        val finalUrl = webView.URL?.absoluteString
        if (finalUrl == null || !finalUrl.isHttpOrHttpsUrl()) {
            throw RenderedPageException("Rendered page has an unsafe URL")
        }
    }

    private suspend fun awaitDomStability(webView: WKWebView, observerKey: String) {
        val startedAt = TimeSource.Monotonic.markNow()
        delay(MINIMUM_RENDER_DELAY_MILLIS.milliseconds)
        while (startedAt.elapsedNow().inWholeMilliseconds < DOM_STABILITY_TIMEOUT_MILLIS) {
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

    private suspend fun primeLazyContent(webView: WKWebView) {
        repeat(MAX_LAZY_LOAD_STEPS) {
            if (evaluate(webView, RenderedPageSnapshotScript.lazyLoadStep) == "done") {
                return
            }
            delay(LAZY_LOAD_STEP_DELAY_MILLIS.milliseconds)
        }
        evaluate(webView, "window.scrollTo(0, 0); 'done'")
    }

    private suspend fun evaluate(webView: WKWebView, script: String): String =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavaScript(script) { result, error ->
                if (!continuation.isActive) return@evaluateJavaScript
                when {
                    error != null -> continuation.resumeWithException(
                        RenderedPageException(
                            error.localizedDescription
                        )
                    )
                    result is String -> continuation.resume(result)
                    else -> continuation.resumeWithException(
                        RenderedPageException("Invalid rendered page snapshot")
                    )
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
    val scheme = NSURL.URLWithString(this)?.scheme?.lowercase()
    scheme == "http" || scheme == "https"
}.getOrDefault(false)

private const val RENDER_TIMEOUT_MILLIS = 18_000L
private const val PAGE_LOAD_TIMEOUT_MILLIS = 8_000L
private const val PAGE_LOAD_POLL_MILLIS = 100L
private const val MINIMUM_RENDER_DELAY_MILLIS = 300L
private const val DOM_STABILITY_TIMEOUT_MILLIS = 8_000L
private const val DOM_QUIET_MILLIS = 800L
private const val DOM_POLL_MILLIS = 250L
private const val MAX_LAZY_LOAD_STEPS = 24
private const val LAZY_LOAD_STEP_DELAY_MILLIS = 100L
private const val RENDER_VIEWPORT_WIDTH = 1024.0
private const val RENDER_VIEWPORT_HEIGHT = 768.0
