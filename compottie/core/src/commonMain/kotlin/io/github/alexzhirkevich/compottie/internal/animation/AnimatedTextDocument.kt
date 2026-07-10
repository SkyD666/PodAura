package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.compottie.dynamic.DynamicTextLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionEvaluator
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextDocument
import io.github.alexzhirkevich.compottie.internal.utils.toOffset
import io.github.alexzhirkevich.compottie.internal.utils.toSize
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal class AnimatedTextDocument(

    @SerialName("k")
    override val keyframes: List<TextDocumentKeyframe>,

    @SerialName("x")
    val expression: String? = null,

    @SerialName("ix")
    override val index: Int? = null,

    @SerialName("sid")
    val slotID: String? = null
) : AnimatedKeyframeProperty<TextDocument, TextDocumentKeyframe>, ExpressionHolder {

    private val document = TextDocument()

    @Transient
    override val jsCache: MutableMap<String, JsAny?> = HashMap()

    override var group: PropertyGroup? = null

    private val evaluator by lazy {
        expression?.let { ExpressionEvaluator(it, this) }
    }

    override fun prepareExpressions(state: AnimationState) {
        evaluator
    }

    @Transient
    var dynamic: DynamicTextLayerProvider? = null

    private val fillColorList by lazy {
        FloatArray(4)
    }

    private val strokeColorList by lazy {
        FloatArray(4)
    }

    private val sizeList by lazy {
        FloatArray(2)
    }

    private val positionList by lazy {
        FloatArray(2)
    }

    @Transient
    private val delegate = BaseKeyframeAnimation(
        index = index,
        sourceKeyframes = keyframes,
        emptyValue = document,
        map = { s, e, p ->
            //TODO: lerp properties?
            if (p != 1.0f) s else e
        }
    )

    override fun raw(state: AnimationState): TextDocument {
        return delegate.raw(state)
    }

    override fun interpolated(state: AnimationState): TextDocument {
        val raw = raw(state)

        val evaluatedText = if (state.enableExpressions) {
            evaluator?.evaluate(state) as? String ?: raw.text
        } else {
            raw.text
        }

        return document.apply {
            fontFamily = raw.fontFamily
            fillColor = dynamic?.fillColor
                ?.derive(raw.fillColor?.toColor() ?: Color.Unspecified, state)
                ?.let(fillColorList::fill)
                ?: raw.fillColor

            strokeColor = dynamic?.strokeColor
                ?.derive(raw.strokeColor?.toColor() ?: Color.Unspecified, state)
                ?.let(strokeColorList::fill)
                ?: raw.strokeColor
            strokeWidth = dynamic?.strokeWidth.derive(raw.strokeWidth, state)
            strokeOverFill = dynamic?.strokeOverFill.derive(raw.strokeOverFill, state)
            fontSize = dynamic?.fontSize.derive(raw.fontSize, state)
            lineHeight = dynamic?.lineHeight.derive(raw.lineHeight, state)
            wrapSize = dynamic?.wrapSize?.let {
                it.derive(raw.wrapSize?.toSize() ?: Size.Unspecified, state).let {
                    sizeList[0] = it.width
                    sizeList[1] = it.height
                    sizeList
                }
            } ?: raw.wrapSize
            wrapPosition = dynamic?.wrapPosition?.let {
                it.derive(raw.wrapPosition?.toOffset() ?: Offset.Unspecified, state).let {
                    positionList[0] = it.x
                    positionList[1] = it.y
                    positionList
                }
            } ?: raw.wrapPosition
            text = dynamic?.text.derive(evaluatedText.orEmpty(), state)
            textJustify = dynamic?.textJustify.derive(raw.textJustify, state)
            textTracking = dynamic?.tracking.derive(raw.textTracking ?: 0f, state)
            baselineShift =
                dynamic?.baselineShift.derive(raw.baselineShift ?: 0f, state)
            textCaps = raw.textCaps
        }
    }

    fun copy() = AnimatedTextDocument(
        keyframes = keyframes,
        expression = expression,
        slotID = slotID,
        index = index
    )
}

private fun FloatArray.fill(color: Color): FloatArray {
    this[0] = color.red
    this[1] = color.green
    this[2] = color.blue
    this[3] = color.alpha

    return this
}
