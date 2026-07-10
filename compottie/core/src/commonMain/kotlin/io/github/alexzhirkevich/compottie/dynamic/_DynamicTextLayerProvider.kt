package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextJustify

internal class DynamicTextLayerProvider : DynamicLayerProvider(), DynamicTextLayer {

    var text: PropertyProvider<String>? = null
        private set

    var fontSize: PropertyProvider<Float>? = null
        private set

    var lineHeight: PropertyProvider<Float>? = null
        private set

    var lineSpacing: PropertyProvider<Float>? = null
        private set

    var fillColor: PropertyProvider<Color>? = null
        private set

    var strokeColor: PropertyProvider<Color>? = null
        private set

    var strokeWidth: PropertyProvider<Float>? = null
        private set

    var strokeOverFill: PropertyProvider<Boolean>? = null
        private set

    var textJustify: PropertyProvider<TextJustify>? = null
        private set

    var baselineShift: PropertyProvider<Float>? = null
        private set

    var tracking: PropertyProvider<Float>? = null
        private set

    var wrapSize: PropertyProvider<Size>? = null
        private set

    var wrapPosition: PropertyProvider<Offset>? = null
        private set

    override fun text(provider: PropertyProvider<String>) {
        text = provider
    }

    override fun fontSize(provider: PropertyProvider<Float>) {
        fontSize = provider
    }

    override fun lineHeight(provider: PropertyProvider<Float>) {
        lineHeight = provider
    }

    override fun lineSpacing(provider: PropertyProvider<Float>) {
        lineSpacing = provider
    }

    override fun fillColor(provider: PropertyProvider<Color>) {
        fillColor = provider
    }

    override fun strokeColor(provider: PropertyProvider<Color>) {
        strokeColor = provider
    }

    override fun strokeWidth(provider: PropertyProvider<Float>) {
        strokeWidth = provider
    }

    override fun strokeOverFill(provider: PropertyProvider<Boolean>) {
        strokeOverFill = provider
    }

    override fun textJustify(provider: PropertyProvider<TextJustify>) {
        textJustify = provider
    }

    override fun baselineShift(provider: PropertyProvider<Float>) {
        baselineShift = provider
    }

    override fun tracking(provider: PropertyProvider<Float>) {
        tracking = provider
    }

    override fun size(provider: PropertyProvider<Size>) {
        wrapSize = provider
    }

    override fun position(provider: PropertyProvider<Offset>) {
        wrapPosition = provider
    }
}