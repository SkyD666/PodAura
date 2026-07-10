package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import io.github.alexzhirkevich.compottie.dynamic.DynamicCompositionProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicTextLayerProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.compottie.internal.animation.expressions.toJs
import io.github.alexzhirkevich.compottie.internal.animation.interpolatedNorm
import io.github.alexzhirkevich.compottie.internal.animation.toColor
import io.github.alexzhirkevich.compottie.internal.assets.CharacterData
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffect
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.helpers.LottieBlendMode
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import io.github.alexzhirkevich.compottie.internal.helpers.MatteMode
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextData
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextDocument
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextJustify
import io.github.alexzhirkevich.compottie.internal.helpers.text.fontScale
import io.github.alexzhirkevich.compottie.internal.platform.addCodePoint
import io.github.alexzhirkevich.compottie.internal.platform.charCount
import io.github.alexzhirkevich.compottie.internal.platform.codePointAt
import io.github.alexzhirkevich.compottie.internal.platform.isModifier
import io.github.alexzhirkevich.compottie.internal.utils.fastReset
import io.github.alexzhirkevich.compottie.internal.utils.preScale
import io.github.alexzhirkevich.compottie.internal.utils.preTranslate
import io.github.alexzhirkevich.compottie.internal.utils.toOffset
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsPropertyAccessor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
@SerialName("5")
internal class TextLayer(

    @SerialName("ks")
    override val transform: Transform = Transform(),

    @SerialName("ind")
    override val index: Int? = null,

    @SerialName("ip")
    override val inPoint: Float? = null,

    @SerialName("op")
    override val outPoint: Float? = null,

    @SerialName("st")
    override val startTime: Float? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("sr")
    override val timeStretch: Float = 1f,

    @SerialName("parent")
    override val parent: Int? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("masksProperties")
    override val masks: List<Mask>? = null,

    @SerialName("hasMask")
    override val hasMask: Boolean? = null,

    @SerialName("ef")
    override var effects: List<LayerEffect> = emptyList(),

    @SerialName("ao")
    @Serializable(with = BooleanIntSerializer::class)
    override val autoOrient: Boolean = false,

    @SerialName("tt")
    override val matteMode: MatteMode? = null,

    @SerialName("tp")
    override val matteParent: Int? = null,

    @SerialName("td") @Serializable(with = BooleanIntSerializer::class)
    override val matteTarget: Boolean? = null,

    @SerialName("bm")
    override val blendMode: LottieBlendMode = LottieBlendMode.Normal,

    @SerialName("t")
    private val textData: TextData,
) : BaseLayer() {

    @Transient
    private val fillProperties = DrawProperties(Fill)

    @Transient
    private val fillPaint = Paint().apply {
        isAntiAlias = true
    }

    @Transient
    private val strokeProperties = DrawProperties(Stroke(0f))

    @Transient
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
    }

    @Transient
    private val textAnimation = textData.ranges.firstOrNull()

    @Transient
    private val textSubLines: MutableList<TextSubLine> = ArrayList()

    @Transient
    private var textStyle: TextStyle = TextStyle.Default

    @Transient
    private val codePointCache = mutableMapOf<Long, String>()

    @Transient
    private val stringBuilder = StringBuilder(2)

    @Transient
    private val matrix = Matrix()

    override fun deepCopy(): Layer {
        return TextLayer(
            transform = transform.deepCopy(),
            index = index,
            inPoint = inPoint,
            outPoint = outPoint,
            startTime = startTime,
            name = name,
            timeStretch = timeStretch,
            parent = parent,
            hidden = hidden,
            masks = masks?.map(Mask::deepCopy),
            hasMask = hasMask,
            effects = effects.map(LayerEffect::copy),
            textData = textData.deepCopy(),
            autoOrient = autoOrient,
            matteMode = matteMode,
            matteParent = matteParent,
            matteTarget = matteTarget,
            blendMode = blendMode,
        )
    }

    override fun drawLayer(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState
    ) {
        val document = textData.document.interpolated(state)

        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.concat(parentMatrix)

            configurePaint(document, parentAlpha, state)

            val hasFontFamily = configureTextStyle(drawScope, document, state)

            val ascent = document.fontFamily?.let {
                state.composition.animation.fonts?.find(it)
            }?.ascent ?: 0f


            if (hasFontFamily || state.enableTextGrouping) {
                drawTextWithFonts(
                    state = state,
                    ascent = ascent,
                    drawScope = drawScope,
                    document = document
                )
            } else {
                val glyphs = state.composition.findGlyphs(document.fontFamily)

                if (glyphs != null) {
                    drawTextWithGlyphs(
                        drawScope = drawScope,
                        document = document,
                        ascent = ascent,
                        state = state,
                        glyphs = glyphs
                    )
                } else {
                    drawTextWithFonts(
                        state = state,
                        ascent = ascent,
                        drawScope = drawScope,
                        document = document
                    )
                }
            }

            canvas.restore()
        }
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect
    ) {
        super.getBounds(drawScope, parentMatrix, applyParents, state, outBounds)

        val composition = state.composition

        outBounds.set(0f, 0f, composition.animation.width, composition.animation.height)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "text" -> jsCache.getOrPut("text") {
                runtime.makeObject(
                    mapOf(
                        "sourceText".toJs(runtime) to JsPropertyAccessor
                            .BackedField(getter = Callable {
                                textData.document.raw(state).text?.toJs(runtime)
                            })
                    )
                )
            }

            else -> super.get(property, runtime)
        }
    }

    override fun setDynamicProperties(
        composition: DynamicCompositionProvider?,
        state: AnimationState
    ): DynamicLayerProvider? {
        return super.setDynamicProperties(composition, state).also {
            textData.document.dynamic = it as? DynamicTextLayerProvider
        }
    }

    private fun configurePaint(document: TextDocument, parentAlpha: Float, state: AnimationState) {

        val transformOpacity = transform.opacity.interpolatedNorm(state)

        val fillOpacity = textAnimation?.style?.fillOpacity?.interpolatedNorm(state) ?: 1f

        val strokeOpacity = textAnimation?.style?.strokeOpacity?.interpolatedNorm(state) ?: 1f

        val fillH = textAnimation?.style?.fillHue?.interpolatedFloat(state) ?: -1f
        val fillS = textAnimation?.style?.fillSaturation?.interpolatedFloat(state) ?: -1f
        val fillB = textAnimation?.style?.fillBrightness?.interpolatedFloat(state) ?: -1f

        fillProperties.color = if (fillH >= 0 && fillS >= 0f && fillB >= 0f) {
            Color.hsl(
                fillH.coerceIn(0f, 360f),
                fillS.coerceIn(0f, 1f),
                fillB.coerceIn(0f, 1f)
            )
        } else {
            textAnimation?.style?.fillColor?.interpolatedColor(state)
                ?.let(::Color)
                ?: document.fillColor?.toColor() ?: Color.Transparent
        }

        fillProperties.alpha = (parentAlpha * transformOpacity * fillOpacity).coerceIn(0f, 1f)

        fillPaint.color = fillProperties.color
        fillPaint.alpha = fillProperties.alpha * fillProperties.color.alpha

        val strokeH = textAnimation?.style?.strokeHue?.interpolatedFloat(state) ?: -1f
        val strokeS = textAnimation?.style?.strokeSaturation?.interpolatedFloat(state) ?: -1f
        val strokeB = textAnimation?.style?.strokeBrightness?.interpolatedFloat(state) ?: -1f

        strokeProperties.color = if (strokeH >= 0 && strokeS >= 0 && strokeB >= 0) {
            Color.hsl(strokeH.coerceIn(0f, 360f), strokeS.coerceIn(0f, 1f), strokeB.coerceIn(0f, 1f))
        } else {
            textAnimation?.style?.strokeColor?.interpolatedColor(state)
                ?.let(::Color)
                ?: document.strokeColor?.toColor() ?: Color.Transparent
        }

        strokeProperties.alpha = (parentAlpha * transformOpacity * strokeOpacity).coerceIn(0f, 1f)

        val strokeWidth = textAnimation?.style?.strokeWidth?.interpolatedFloat(state)
            ?: document.strokeWidth

        if (strokeProperties.style.width != strokeWidth) {
            strokeProperties.style = Stroke(width = strokeWidth)
        }

        strokePaint.color = strokeProperties.color
        strokePaint.alpha = strokeProperties.alpha * strokeProperties.color.alpha
        strokePaint.strokeWidth = strokeWidth
    }

    private fun configureTextStyle(
        drawScope: DrawScope,
        document: TextDocument,
        animationState: AnimationState
    ): Boolean = drawScope.run {
        val fontSize = document.fontSize.toSp()
        val baselineShift = document.baselineShift
            ?.let { BaselineShift(it) }
            ?: textStyle.baselineShift

        val strFontFamily = document.fontFamily ?: return@run false
        val fontFamily = animationState.fonts[strFontFamily]

        val fontSpec = animationState.composition.animation.fonts?.find(strFontFamily)
        val weight = fontSpec?.weight ?: FontWeight.Normal
        val style = fontSpec?.style ?: FontStyle.Normal

        val letterSpacing = textAnimation?.style?.letterSpacing
            ?.interpolatedFloat(animationState)?.toSp()
            ?: textStyle.letterSpacing

        val lineHeight = document.lineHeight.toSp()

        if (
            textStyle.fontSize != fontSize ||
            textStyle.baselineShift != baselineShift ||
            textStyle.lineHeight != lineHeight ||
            textStyle.fontFamily != fontFamily ||
            textStyle.letterSpacing != letterSpacing ||
            textStyle.fontWeight != weight ||
            textStyle.fontStyle != style
        ) {
            textStyle = textStyle.copy(
                baselineShift = baselineShift,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                fontWeight = weight,
                fontStyle = style
            )
        }

        fontFamily != null
    }

    private fun drawTextWithFonts(
        state: AnimationState,
        ascent: Float,
        drawScope: DrawScope,
        document: TextDocument
    ) {
        var tracking = document.textTracking?.div(10f) ?: 0f

        val text = document.text ?: return

        val allLines = getTextLines(text)

        //TODO: tracking animation?
        tracking = drawScope.run {
            tracking.sp.toPx()
        }

        val canvas = drawScope.drawContext.canvas

        allLines.fastForEachIndexed { alLinesIdx, textLine ->
            val boxWidth = document.wrapSize?.firstOrNull() ?: 0f

            val lines = splitGlyphTextIntoLines(
                textMeasurer = state.textMeasurer,
                density = drawScope,
                layoutDirection = drawScope.layoutDirection,
                textLine = textLine,
                fontScale = document.fontScale,
                boxWidth = boxWidth,
                tracking = tracking,
                glyphs = null
            )

            lines.fastForEachIndexed { idx, line ->

                canvas.save()
                if (offsetCanvas(
                        state = state,
                        canvas = canvas,
                        document = document,
                        lineIndex = alLinesIdx + idx,
                        lineWidth = line.width,
                        accent = ascent,
                        withFonts = true
                    )
                ) {
                    drawFontTextLine(
                        text = line.text,
                        textMeasurer = state.textMeasurer,
                        documentData = document,
                        drawScope = drawScope,
                        canvas = canvas,
                        tracking = tracking,
                        drawFullLine = state.enableTextGrouping
                    )
                }

                canvas.restore()
            }
        }
    }

    private fun drawTextWithGlyphs(
        drawScope: DrawScope,
        document: TextDocument,
        ascent: Float,
        state: AnimationState,
        glyphs: Map<String, CharacterData>
    ) {
        // Split full text in multiple lines
        val textLines = getTextLines(document.text ?: return)
        val tracking = (document.textTracking ?: 0f) / 10f
        val canvas = drawScope.drawContext.canvas

        textLines.fastForEachIndexed { outerIndex, line ->
            val boxWidth = document.wrapSize?.getOrNull(0) ?: 0f

            val lines = splitGlyphTextIntoLines(
                textMeasurer = state.textMeasurer,
                density = drawScope,
                layoutDirection = drawScope.layoutDirection,
                textLine = line,
                fontScale = document.fontScale,
                boxWidth = boxWidth,
                tracking = tracking,
                glyphs = glyphs
            )

            lines.forEachIndexed { innerIndex, l ->
                canvas.save()

                if (offsetCanvas(
                        state = state,
                        canvas = canvas,
                        document = document,
                        lineIndex = outerIndex + innerIndex,
                        lineWidth = l.width,
                        accent = ascent,
                        withFonts = false
                    )
                ) {
                    drawGlyphTextLine(
                        text = l.text,
                        state = state,
                        fontScale = document.fontScale,
                        documentData = document,
                        drawScope = drawScope,
                        tracking = tracking,
                        glyphs = glyphs
                    )
                }

                canvas.restore()
            }
        }
    }

    private fun getTextLines(text: String): List<String> {
        // Split full text by carriage return character
        val formattedText = text.replace("\r\n".toRegex(), "\r")
            .replace("\u0003".toRegex(), "\r")
            .replace("\n".toRegex(), "\r")
        return formattedText.split("\r".toRegex())
            .dropLastWhile { it.isEmpty() }
    }

    private fun splitGlyphTextIntoLines(
        textMeasurer: TextMeasurer,
        density: Density,
        layoutDirection: LayoutDirection,
        textLine: String,
        fontScale: Float,
        boxWidth: Float,
        tracking: Float,
        glyphs: Map<String, CharacterData>?,
    ): List<TextSubLine> {
        var lineCount = 0

        var currentLineWidth = 0f
        var currentLineStartIndex = 0

        var currentWordStartIndex = 0
        var currentWordWidth = 0f
        var nextCharacterStartsWord = false

        // The measured size of a space.
        var spaceWidth = 0f

        for (i in textLine.indices) {
            val c = textLine[i]
            val currentCharWidth = if (glyphs != null) {
                val character = glyphs[textLine[i].toString()]
                (character?.width ?: 0f) * fontScale + tracking
            } else {
                textMeasurer.measure(
                    text = textLine[i].toString(),
                    style = textStyle,
                    density = density,
                    layoutDirection = layoutDirection,
                ).size.width + tracking
            }

            if (c == ' ') {
                spaceWidth = currentCharWidth
                nextCharacterStartsWord = true
            } else if (nextCharacterStartsWord) {
                nextCharacterStartsWord = false
                currentWordStartIndex = i
                currentWordWidth = currentCharWidth
            } else {
                currentWordWidth += currentCharWidth
            }
            currentLineWidth += currentCharWidth

            if (boxWidth > 0f && currentLineWidth >= boxWidth) {
                if (c == ' ') {
                    // Spaces at the end of a line don't do anything. Ignore it.
                    // The next non-space character will hit the conditions below.
                    continue
                }
                val subLine: TextSubLine =
                    ensureEnoughSubLines(++lineCount)
                if (currentWordStartIndex == currentLineStartIndex) {
                    // Only word on line is wider than box, start wrapping mid-word.
                    val substr = textLine.substring(currentLineStartIndex, i)
                    val trimmed = substr.trim { it <= ' ' }
                    val trimmedSpace = (trimmed.length - substr.length) * spaceWidth
                    subLine.set(trimmed, currentLineWidth - currentCharWidth - trimmedSpace)
                    currentLineStartIndex = i
                    currentLineWidth = currentCharWidth
                    currentWordStartIndex = currentLineStartIndex
                    currentWordWidth = currentCharWidth
                } else {
                    val substr =
                        textLine.substring(currentLineStartIndex, currentWordStartIndex - 1)
                    val trimmed = substr.trim { it <= ' ' }
                    val trimmedSpace = (substr.length - trimmed.length) * spaceWidth
                    subLine.set(
                        trimmed,
                        currentLineWidth - currentWordWidth - trimmedSpace - spaceWidth
                    )
                    currentLineStartIndex = currentWordStartIndex
                    currentLineWidth = currentWordWidth
                }
            }
        }
        if (currentLineWidth > 0f) {
            val line = ensureEnoughSubLines(++lineCount)
            line.set(textLine.substring(currentLineStartIndex), currentLineWidth)
        }
        return textSubLines.subList(0, lineCount)
    }

    private fun ensureEnoughSubLines(numLines: Int): TextSubLine {
        for (i in textSubLines.size until numLines) {
            textSubLines.add(TextSubLine())
        }
        return textSubLines[numLines - 1]
    }

    private fun offsetCanvas(
        state: AnimationState,
        canvas: Canvas,
        document: TextDocument,
        lineIndex: Int,
        lineWidth: Float,
        accent: Float,
        withFonts: Boolean,
    ): Boolean {

        val position = document.wrapPosition?.toOffset()
            ?.plus(Offset(x = 0f, y = accent / 100f * document.fontSize))
            ?: Offset.Zero

        val size = document.wrapSize?.let { Size(it[0], it[1]) } ?: Size.Zero

        val lineSpacing = textAnimation?.style?.lineSpacing?.interpolatedFloat(state) ?: 0f

        var lineOffsetY = (lineIndex * (document.lineHeight + lineSpacing)) + position.y

        if (withFonts) {
            // compose draws text below the Y. Lottie expects it to be above the Y
            lineOffsetY -= document.fontSize
        }

        if (state.clipTextToBoundingBoxes &&
            lineOffsetY >= position.y + size.height + document.fontSize
        ) {
            return false
        }

        when (document.textJustify) {
            TextJustify.Left, TextJustify.LastLineLeft ->
                canvas.translate(position.x, lineOffsetY)

            TextJustify.Right, TextJustify.LastLineRight ->
                canvas.translate(position.x + size.width - lineWidth, lineOffsetY)

            TextJustify.Center, TextJustify.LastLineCenter ->
                canvas.translate(position.x + (size.width - lineWidth) / 2f, lineOffsetY)
        }
        return true
    }

    private fun drawFontTextLine(
        text: String,
        textMeasurer: TextMeasurer,
        documentData: TextDocument,
        drawScope: DrawScope,
        canvas: Canvas,
        tracking: Float,
        drawFullLine: Boolean
    ) {

        if (drawFullLine) {
            val character = textMeasurer.measure(
                text,
                textStyle,
                density = drawScope,
                layoutDirection = drawScope.layoutDirection,
            )
            drawCharacterFromFont(character, documentData, drawScope)
            return
        }

        var i = 0
        while (i < text.length) {
            val charString: String = codePointToString(text, i)
            i += charString.length

            val measureResult = textMeasurer.measure(charString, textStyle)

            drawCharacterFromFont(measureResult, documentData, drawScope)
            val charWidth = measureResult.size.width
            val tx = charWidth + tracking
            canvas.translate(tx, 0f)
        }
    }

    private fun drawGlyphTextLine(
        text: String,
        state: AnimationState,
        documentData: TextDocument,
        fontScale: Float,
        drawScope: DrawScope,
        tracking: Float,
        glyphs: Map<String, CharacterData>
    ) {
        val canvas = drawScope.drawContext.canvas
        text.forEach { c ->

            val character = glyphs[c.toString()] ?: return@forEach

            drawCharacterAsGlyph(drawScope, state, character, fontScale, documentData)
            val tx = (character.width ?: 0f) * fontScale + tracking
            canvas.translate(tx, 0f)
        }
    }

    private fun drawCharacterAsGlyph(
        drawScope: DrawScope,
        state: AnimationState,
        character: CharacterData,
        fontScale: Float,
        document: TextDocument,
    ) {
        matrix.fastReset()
        matrix.preTranslate(0f, -(document.baselineShift ?: 0f))
        matrix.preScale(fontScale, fontScale)
        character.data?.draw(drawScope, state, matrix, strokePaint, fillPaint)
    }

    private fun codePointToString(text: String, startIndex: Int): String {
        val firstCodePoint: Int = text.codePointAt(startIndex)
        val firstCodePointLength: Int = charCount(firstCodePoint)
        var key = firstCodePoint
        var index = startIndex + firstCodePointLength
        while (index < text.length) {
            val nextCodePoint: Int = text.codePointAt(index)
            if (!isModifier(nextCodePoint)) {
                break
            }
            val nextCodePointLength: Int = charCount(nextCodePoint)
            index += nextCodePointLength
            key = key * 31 + nextCodePoint
        }

        codePointCache[key.toLong()]?.let { return it }

        stringBuilder.setLength(0)
        var i = startIndex
        while (i < index) {
            val codePoint: Int = text.codePointAt(i)
            stringBuilder.addCodePoint(codePoint)
            i += charCount(codePoint)
        }
        val str: String = stringBuilder.toString()
        codePointCache[key.toLong()] = str
        return str
    }

    private fun drawCharacterFromFont(
        character: TextLayoutResult,
        documentData: TextDocument,
        drawScope: DrawScope
    ) {
        if (documentData.strokeOverFill) {
            drawCharacter(character, fillProperties, drawScope)
            drawCharacter(character, strokeProperties, drawScope)
        } else {
            drawCharacter(character, strokeProperties, drawScope)
            drawCharacter(character, fillProperties, drawScope)
        }
    }

    private fun drawCharacter(
        measureResult: TextLayoutResult,
        drawProperties: DrawProperties<*>,
        drawScope: DrawScope
    ) {
        if (drawProperties.color == Color.Transparent || drawProperties.alpha == 0f) {
            return
        }

        if ((drawProperties.style as? Stroke)?.width == 0f) {
            return
        }

        drawScope.drawText(
            textLayoutResult = measureResult,
            color = drawProperties.color,
            alpha = drawProperties.alpha,
        )
    }
}

private class DrawProperties<S : DrawStyle>(
    var style: S,
    var color: Color = Color.Transparent,
    var alpha: Float = 1f
)

private data class TextSubLine(
    var text: String = "",
    var width: Float = 0f
) {
    fun set(text: String, width: Float) {
        this.text = text
        this.width = width
    }
}
