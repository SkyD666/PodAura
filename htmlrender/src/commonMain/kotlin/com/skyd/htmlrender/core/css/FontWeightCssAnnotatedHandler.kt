package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.model.TextStyler
import kotlin.math.roundToInt

open class FontWeightCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parse(value)?.also { weight ->
            list.add(SpanStyleStyler { SpanStyle(fontWeight = weight) })
        }
    }

    internal open fun parse(value: String): FontWeight? = runCatching {
        when (value) {
            "normal" -> FontWeight.Normal
            "bold" -> FontWeight.Bold
            else -> {
                val int = value.toFloatOrNull()?.roundToInt()
                if (int != null) {
                    FontWeight(int)
                } else {
                    logFail(value)
                    null
                }
            }
        }
    }.onFailure {
        logFail(value, it)
    }.getOrNull()

    private fun logFail(value: String, throwable: Throwable? = null) {
        Logger.w(MODULE, throwable) {
            "parse FontWeight fail: $value"
        }
    }

    companion object {
        const val MODULE = "FontWeightCssAnnotatedHandler"
    }
}