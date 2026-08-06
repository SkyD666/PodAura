package com.skyd.podaura.ui.component.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Build
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.read.ReadTextSizePreference


@Composable
actual fun PodAuraWebView(
    modifier: Modifier,
    content: String,
    refererDomain: String?,
    horizontalPadding: Float,
    onImageClick: ((imageUrl: String, alt: String) -> Unit)?,
) {
    val context = LocalContext.current
    val textStyle = LocalTextStyle.current
    val tonalElevation = ReadContentTonalElevationPreference.current
    val selectionTextColor = Color.Black.toArgb()
    val selectionBgColor = LocalTextSelectionColors.current.backgroundColor.toArgb()
    val textColor: Int = textStyle.color.takeOrElse { LocalContentColor.current }.toArgb()
    val textWeight = textStyle.fontWeight?.weight
    val textAlign = "start"
    val boldTextColor: Int = textColor
    val linkTextColor: Int = MaterialTheme.colorScheme.primary.toArgb()
    val subheadBold = true
    val subheadUpperCase = false
    val fontSize = ReadTextSizePreference.current
    val letterSpacing: Float =
        if (textStyle.letterSpacing.isSp) textStyle.letterSpacing.value else 0f
    val lineHeight = textStyle.lineHeight.run { if (isEm) "${this}em" else "${this}px" }
    val imgMargin = horizontalPadding
    val imgBorderRadius = 0
    val codeTextColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val codeBgColor =
        MaterialTheme.colorScheme.surfaceColorAtElevation((tonalElevation + 6).dp).toArgb()
    val bionicReading = false

    val uriHandler = LocalUriHandler.current

    val holderKey = "PodAuraWebView:$currentCompositeKeyHashCode"
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    // Navigation3 gives every back-stack entry its own ViewModelStoreOwner. Retain the WebView
    // only in that scope; a root Activity owner would otherwise retain short-lived WebViews until
    // the Activity is destroyed.
    val retainInBackStack = viewModelStoreOwner != null && viewModelStoreOwner !is ComponentActivity
    val holder = if (retainInBackStack) {
        viewModel<PodAuraWebViewHolder>(
            viewModelStoreOwner = viewModelStoreOwner,
            key = holderKey,
        ) {
            PodAuraWebViewHolder(context)
        }
    } else {
        remember(holderKey) { PodAuraWebViewHolder(context) }
    }
    val webViewClient = remember(refererDomain, uriHandler) {
        WebViewClient(
            refererDomain = refererDomain,
            onOpenLink = { url -> uriHandler.safeOpenUri(url) }
        )
    }
    val javaScriptInterface = remember(onImageClick) {
        JavaScriptInterface(onImageClick = onImageClick)
    }
    val html = WebViewHtml.HTML.format(
        WebViewStyle.get(
            fontSize = fontSize,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            horizontalPadding = horizontalPadding,
            textColor = textColor,
            textWeight = textWeight,
            textAlign = textAlign,
            boldTextColor = boldTextColor,
            subheadBold = subheadBold,
            subheadUpperCase = subheadUpperCase,
            imgMargin = imgMargin,
            imgBorderRadius = imgBorderRadius,
            linkTextColor = linkTextColor,
            codeTextColor = codeTextColor,
            codeBgColor = codeBgColor,
            tablePadding = horizontalPadding,
            selectionTextColor = selectionTextColor,
            selectionBgColor = selectionBgColor,
        ),
        holder.baseUrl,
        content,
        WebViewScript.get(bionicReading),
    )
    AndroidView(
        modifier = modifier,
        factory = {
            holder.attach(context)
            holder.webView
        },
        update = {
            with(it) {
                holder.attach(context)
                this.webViewClient = webViewClient
                removeJavascriptInterface(JavaScriptInterface.NAME)
                addJavascriptInterface(javaScriptInterface, JavaScriptInterface.NAME)
                settings.defaultFontSize = fontSize.toInt()
                if (holder.loadedHtml != html) {
                    holder.loadedHtml = html
                    loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
                }
            }
        },
        onRelease = {
            if (retainInBackStack) holder.detach(refererDomain) else holder.release()
        },
    )
}

private class PodAuraWebViewHolder(context: Context) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val contextWrapper = MutableContextWrapper(context)
    private var released = false

    val webView = androidWebView(
        context = contextWrapper,
        webViewClient = WebViewClient(refererDomain = null, onOpenLink = {}),
    )
    val baseUrl: String? = webView.url
    var loadedHtml: String? = null

    fun attach(context: Context) {
        if (released) return
        contextWrapper.baseContext = context
    }

    fun detach(refererDomain: String?) {
        if (released) return
        // Keep handling page completion and resource requests while this WebView is retained in
        // the back stack. Only disable link navigation, whose callback belongs to the detached UI.
        webView.webViewClient = WebViewClient(refererDomain = refererDomain, onOpenLink = {})
        webView.removeJavascriptInterface(JavaScriptInterface.NAME)
        contextWrapper.baseContext = applicationContext
    }

    fun release() {
        if (released) return
        detach(refererDomain = null)
        released = true
        webView.stopLoading()
        webView.destroy()
    }

    override fun onCleared() = release()
}

fun androidWebView(
    context: Context,
    webViewClient: WebViewClient,
    onImageClick: ((imageUrl: String, altText: String) -> Unit)? = null,
) = WebView(context).apply {
    this.webViewClient = webViewClient
    scrollBarSize = 0
    isHorizontalScrollBarEnabled = false
    isVerticalScrollBarEnabled = true
    setBackgroundColor(Color.Transparent.toArgb())
    with(settings) {
        domStorageEnabled = true
        @SuppressLint("SetJavaScriptEnabled")
        javaScriptEnabled = true
        addJavascriptInterface(
            JavaScriptInterface(onImageClick = onImageClick),
            JavaScriptInterface.NAME,
        )
        setSupportZoom(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isAlgorithmicDarkeningAllowed = true
        }
    }
}
