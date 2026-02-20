package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkMatcher
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkRequest
import com.skyd.podaura.ui.component.navigation.deeplink.KeyDecoder
import com.skyd.podaura.ui.screen.deepLinkPatterns
import io.ktor.http.URLBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ExternalUrlHandler {
    // Storage for when a Url arrives before the listener is set up
    private var cached: UrlData? = null

    var listener: ((data: UrlData) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                // When a listener is set and `cached` is not empty,
                // immediately invoke the listener with the cached Url
                cached?.let { value.invoke(it) }
                cached = null
            }
        }

    // When a new Url arrives, cache it.
    // If the listener is already set, invoke it and clear the cache immediately.
    fun onNewUrl(data: UrlData) {
        cached = data
        listener?.let {
            it.invoke(data)
            cached = null
        }
    }

    @Serializable
    data class UrlData(
        @SerialName(URL_NAME)
        val url: String? = null,
        @SerialName(MIMETYPE_NAME)
        val mimeType: String? = null,
        @SerialName(ACTION_NAME)
        val action: String? = null,
    ) {
        companion object {
            const val KEY = "urlData"
            const val URL_NAME = "UrlData_Url"
            const val MIMETYPE_NAME = "UrlData_MimeType"
            const val ACTION_NAME = "UrlData_Action"
        }
    }
}

@Composable
expect fun initialNavKey(): NavKey?

@Composable
expect fun ExternalUrlListener(navBackStack: MutableList<NavKey>)

@Composable
internal fun DefaultUrlListener(navBackStack: MutableList<NavKey>) {
    DisposableEffect(navBackStack) {
        ExternalUrlHandler.listener = { data ->
            data.toNavKey()?.let { navBackStack.add(it) }
        }
        onDispose {
            // Removes the listener when the composable is no longer active
            ExternalUrlHandler.listener = null
        }
    }
}

internal fun ExternalUrlHandler.UrlData.toNavKey(): NavKey? {
    val request = DeepLinkRequest(
        uri = url?.let { URLBuilder(it).build() },
        mimeType = mimeType,
        action = action,
    )

    val match = deepLinkPatterns.firstNotNullOfOrNull { pattern ->
        DeepLinkMatcher(request, pattern).match()
    }
    return match?.let {
        KeyDecoder(buildMap {
            putAll(match.args)
            url?.let { put(ExternalUrlHandler.UrlData.URL_NAME, it) }
            mimeType?.let { put(ExternalUrlHandler.UrlData.MIMETYPE_NAME, it) }
            action?.let { put(ExternalUrlHandler.UrlData.ACTION_NAME, it) }
        }).decodeSerializableValue(match.serializer)
    }
}