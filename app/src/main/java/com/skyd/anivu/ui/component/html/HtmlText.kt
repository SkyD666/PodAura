package com.skyd.anivu.ui.component.html

import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.text.parseAsHtml
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun HtmlText(
    modifier: Modifier = Modifier,
    htmlFlags: Int = FROM_HTML_MODE_LEGACY,
    text: String,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = TextUnit.Unspecified,
    onImageClick: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val textSize = with(LocalDensity.current) { fontSize.toPx() }
    var componentWidth by remember { mutableIntStateOf(0) }
    AndroidView(
        modifier = modifier.onGloballyPositioned {
            componentWidth = it.size.width
        },
        factory = { c ->
            TextView(c).apply {
                // setTextIsSelectable should come before movementMethod,
                // otherwise movementMethod is invalid.
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(color.toArgb())
            }
        },
        update = { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            textView.text = text.parseAsHtml(
                htmlFlags,
                imageGetter = ImageGetter(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    maxWidth = { componentWidth },
                    onSuccess = { _, _ ->
                        textView.text = textView.text
                    }
                ),
                tagHandler = TagHandler(
                    handlers = mutableListOf<Html.TagHandler>().apply {
                        if (onImageClick != null) {
                            add(ImgTagHandler(onImageClick))
                        }
                    }
                ),
            )
        }
    )
}