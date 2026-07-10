package io.github.alexzhirkevich.compottie.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionComposition
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionsEngineFactory
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionsRuntime
import io.github.alexzhirkevich.compottie.internal.assets.ImageAsset
import io.github.alexzhirkevich.compottie.internal.assets.LottieAsset
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.keight.ScriptEngine
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration


public class AnimationState @PublishedApi internal constructor(
    public val composition: LottieComposition,
    internal val textMeasurer: TextMeasurer,
    internal val assets: Map<String, LottieAsset>,
    internal val fonts: Map<String, FontFamily>,
    theme: String?,
    frame: Float,
    applyOpacityToLayers: Boolean,
    clipToCompositionBounds: Boolean,
    clipTextToBoundingBoxes: Boolean,
    enableTextGrouping: Boolean,
    enableMergePaths: Boolean,
    enableExpressions: Boolean,
    layer: Layer,
    coroutineContext: CoroutineContext,
    expressionEngineFactory: ExpressionsEngineFactory
) {

    /**
     * All successfully loaded images for this animation by the asset id
     * */
    public val images: Map<String, ImageBitmap> = assets
        .filterValues { it is ImageAsset && it.bitmap != null }
        .mapValues { (it.value as ImageAsset).bitmap!! }

    /**
     * Current animation frame
     * */
    public var frame: Float = frame
        private set

    internal var absoluteFrame = frame
        private set

    private val tweenAnimatable = Animatable(0f)
    internal var tweenTargetFrame: Float? by mutableStateOf(0f)
        private set
    internal val tweenProgress: Float get() = tweenAnimatable.value
    internal val isTweenRunning: Boolean get() = tweenAnimatable.isRunning

    /**
     * Current animation progress from 0.0 to 1.0
     * */
    public val progress: Float
        get() = composition.frameToProgress(frame)

    internal val absoluteProgress: Float
        get() {
            val p = (absoluteFrame - composition.animation.inPoint) /
                    (composition.animation.outPoint - composition.animation.inPoint)
            return p.coerceIn(0f, 1f)
        }

    /**
     * Time elapsed from the start of animation
     * */
    public val time: Duration
        get() = composition.duration * progress.toDouble()

    internal val absoluteTime: Duration
        get() = composition.duration * absoluteProgress.toDouble()

    internal val scriptEngine: ScriptEngine<ExpressionsRuntime> by lazy {
        expressionEngineFactory(coroutineContext, this)
    }

    internal var clipToCompositionBounds by mutableStateOf(clipToCompositionBounds)
    internal var clipTextToBoundingBoxes by mutableStateOf(clipTextToBoundingBoxes)
    internal var applyOpacityToLayers by mutableStateOf(applyOpacityToLayers)
    internal var enableMergePaths by mutableStateOf(enableMergePaths)
    internal var enableExpressions by mutableStateOf(enableExpressions)
    internal var enableTextGrouping by mutableStateOf(enableTextGrouping)
    internal var theme by mutableStateOf(theme)

    internal var thisLayer: Layer = layer
        private set

    internal var thisComp: ExpressionComposition = composition.expressionComposition
        private set

    internal var thisProperty: RawProperty<*>? = null
        private set


    internal suspend fun tweenTo(
        frame: Float,
        spec: AnimationSpec<Float>,
        onFinish: suspend () -> Unit
    ) {
        tweenTargetFrame = frame
        tweenAnimatable.snapTo(0f)
        try {
            tweenAnimatable.animateTo(1f, spec)
        } finally {
            onFinish()
            tweenTargetFrame = null
        }
    }

    /**
     * Remaps current state to requested [frame] and performs [block] on it.
     * State is restored after the [block] call
     * */
    internal inline fun <R> onFrame(frame: Float, block: (AnimationState) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val initial = this.frame

        return try {
            this.frame = frame
            block(this)
        } finally {
            this.frame = initial
        }
    }

    internal fun <R> onTime(time: Float, block: (AnimationState) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val start = runCatching {
            thisComp.startTime
        }.getOrElse { composition.startTime }

        return onFrame((time - start) * composition.frameRate, block)
    }

    internal inline fun <R> onLayer(layer: Layer, block: (AnimationState) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val prevLayer = this.thisLayer
        return try {
            this.thisLayer = layer
            block(this)
        } finally {
            this.thisLayer = prevLayer
        }
    }

    internal inline fun <R> onComposition(
        comp: ExpressionComposition,
        block: (AnimationState) -> R
    ): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val prevComp = this.thisComp
        return try {
            this.thisComp = comp
            block(this)
        } finally {
            this.thisComp = prevComp
        }
    }


    internal inline fun <R> onProperty(property: RawProperty<*>, block: (AnimationState) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val prev = this.thisProperty
        return try {
            this.thisProperty = property
            block(this)
        } finally {
            this.thisProperty = prev
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AnimationState

        if (composition != other.composition) return false
        if (assets != other.assets) return false
        if (fonts != other.fonts) return false
        if (frame != other.frame) return false
        if (clipToCompositionBounds != other.clipToCompositionBounds) return false
        if (clipTextToBoundingBoxes != other.clipTextToBoundingBoxes) return false
        if (textMeasurer != other.textMeasurer) return false
        if (enableMergePaths != other.enableMergePaths) return false

        return true
    }

    override fun hashCode(): Int {
        var result = composition.hashCode()
        result = 31 * result + assets.hashCode()
        result = 31 * result + fonts.hashCode()
        result = 31 * result + frame.hashCode()
        result = 31 * result + clipToCompositionBounds.hashCode()
        result = 31 * result + clipTextToBoundingBoxes.hashCode()
        result = 31 * result + textMeasurer.hashCode()
        result = 31 * result + enableMergePaths.hashCode()
        return result
    }
}

internal val AnimationState.timeSeconds get() = time.inWholeMilliseconds / 1_000f
