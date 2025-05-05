package com.skyd.podaura.ui.component.webview

import android.webkit.JavascriptInterface

class JavaScriptInterface(
    private val onImageClick: ((imageUrl: String, altText: String) -> Unit)?,
) {
    @JavascriptInterface
    fun onImageClick(imageUrl: String?, alt: String?) {
        if (onImageClick != null && imageUrl != null) {
            onImageClick.invoke(imageUrl, alt.orEmpty())
        }
    }

    companion object {
        const val NAME = "JavaScriptInterface"
    }
}
