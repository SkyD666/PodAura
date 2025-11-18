package com.skyd.htmlrender.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.skyd.htmlrender.core.util.TextUnitParser
import com.skyd.htmlrender.ui.styler.MarginStyler

@Composable
fun MarginWrapper(
    annotatedString: AnnotatedString,
    defaultStyle: TextStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    fun getMarginValue(tag: String): TextUnit? = runCatching {
        with(annotatedString) {
            var result = ""
            getStringAnnotations(tag, 0, length).forEach {
                result = MarginStyler.cumulativeValue(result, it.item)
            }
            TextUnitParser.parse(result)
        }
    }.getOrNull()

    val top = getMarginValue(MarginStyler.TOP)
    val bottom = getMarginValue(MarginStyler.BOTTOM)
    val left = getMarginValue(MarginStyler.LEFT)
    val right = getMarginValue(MarginStyler.RIGHT)

    @Composable
    fun TextUnit?.marginUI(isHor: Boolean) {
        if (this == null) return
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = this@marginUI)) {
                    append(" ")
                }
            },
            style = defaultStyle,
            modifier = if (isHor) {
                Modifier.height(0.dp)
            } else {
                Modifier.width(0.dp)
            }
        )
    }

    Column(modifier = modifier) {
        top.marginUI(false)
        Row(Modifier.fillMaxWidth()) {
            left.marginUI(true)
            content()
            right.marginUI(true)
        }
        bottom.marginUI(false)
    }
}