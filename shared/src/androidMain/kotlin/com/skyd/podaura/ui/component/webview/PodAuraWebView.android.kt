package com.skyd.podaura.ui.component.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    val backgroundColor = textStyle.background.toArgb()
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

    val webView by remember(backgroundColor) {
        mutableStateOf(
            androidWebView(
                context = context,
                webViewClient = WebViewClient(
                    refererDomain = refererDomain,
                    onOpenLink = { url -> uriHandler.safeOpenUri(url) }
                ),
                onImageClick = onImageClick
            )
        )
    }
    AndroidView(
        modifier = modifier,
        factory = { webView },
        update = {
            with(it) {
                settings.defaultFontSize = fontSize.toInt()
                loadDataWithBaseURL(
                    null,
                    WebViewHtml.HTML.format(
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
                        url,
                        content,
                        WebViewScript.get(bionicReading),
                    ),
                    "text/HTML",
                    "UTF-8",
                    null,
                )
            }
        },
    )
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