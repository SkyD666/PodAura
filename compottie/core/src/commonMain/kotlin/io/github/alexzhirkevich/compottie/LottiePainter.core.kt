package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.alexzhirkevich.compottie.assets.EmptyAssetsManager
import io.github.alexzhirkevich.compottie.assets.EmptyFontManager
import io.github.alexzhirkevich.compottie.assets.LottieAssetsManager
import io.github.alexzhirkevich.compottie.assets.LottieFontManager
import io.github.alexzhirkevich.compottie.dynamic.DynamicCompositionProvider
import io.github.alexzhirkevich.compottie.dynamic.LottieDynamicProperties
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionsEngineFactory
import io.github.alexzhirkevich.compottie.internal.assets.LottieAsset
import io.github.alexzhirkevich.compottie.internal.hasTextLayers
import io.github.alexzhirkevich.compottie.internal.layers.CompositionLayer
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.internal.utils.fastReset
import io.github.alexzhirkevich.compottie.internal.utils.preScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


/**
 * Create and remember Lottie painter
 *
 * @param composition [LottieComposition] usually created by [rememberLottieComposition]
 * @param progress animation progress from 0 to 1 usually derived from [animateLottieCompositionAsState]
 * @param assetsManager used to load animation assets that were not loaded during composition
 * initialization
 * @param fontManager used to load animation fonts
 * @param dynamicProperties dynamically-configurable animation properties. Can be created with
 * [rememberLottieDynamicProperties]
 * @param theme id of the animation theme bundled in a dotLottie file
 * @param applyOpacityToLayers Sets whether to apply opacity to the each layer instead of shape.
 * Opacity is normally applied directly to a shape. In cases where translucent
 * shapes overlap, applying opacity to a layer will be more accurate at the expense of performance.
 * Note: Turning this on can be very expensive and sometimes can cause artifacts. Enable it only if
 * the animation have translucent overlapping shapes and always test if it works fine for your animation
 * @param clipToCompositionBounds if drawing should be clipped to the [composition].width X [composition].height rect
 * @param clipTextToBoundingBoxes if text should be clipped to its bounding boxes (if provided in animation)
 * @param enableTextGrouping disable line-to-char splitting. Enable this to correctly render texts
 * in locales such as Arabic. This feature forces to use fonts over glyphs and disables text tracking.
 * However, if you have texts rendered with fonts and don't use tracking, you can try enable this option
 * for any locales as this feature greatly improves texts performance
 * @param enableMergePaths enable experimental merge paths feature. Most of the time animation doesn't need
 * it even if it contains merge paths. This feature should only be enabled for tested animations
 * @param enableExpressions enable experimental expressions feature. Changing this parameter after
 * composition (with recomposition) may cause performance spike
 * */
@OptIn(InternalCompottieApi::class)
@Composable
public fun rememberLottiePainter(
    composition: LottieComposition?,
    progress: () -> Float,
    assetsManager: LottieAssetsManager? = null,
    fontManager: LottieFontManager? = null,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    dynamicProperties: LottieDynamicProperties? = null,
    theme: String? = null,
    applyOpacityToLayers: Boolean = false,
    clipToCompositionBounds: Boolean = true,
    clipTextToBoundingBoxes: Boolean = false,
    enableTextGrouping: Boolean = false,
    enableMergePaths: Boolean = false,
    enableExpressions: Boolean,
    expressionEngineFactory: ExpressionsEngineFactory
): LottiePainter {

    val textMeasurer = rememberTextMeasurer(20)

    val updatedProgress by rememberUpdatedState(progress)

    val dp = when (dynamicProperties) {
        is DynamicCompositionProvider -> dynamicProperties
        null -> null
    }

    val copy = dp != null

    val coroutineScope = rememberCoroutineScope()

    val painterState = produceState(
        remember {
            if (
                composition != null &&
                (!composition.hasFonts || fontManager == null) &&
                (!composition.hasAssets || assetsManager == null) &&
                !copy
            ) {
                LottiePainterImpl(
                    composition = composition,
                    progress = updatedProgress::invoke,
                    dynamicProperties = dp,
                    theme = theme,
                    clipTextToBoundingBoxes = clipTextToBoundingBoxes,
                    textMeasurer = textMeasurer,
                    clipToCompositionBounds = clipToCompositionBounds,
                    enableTextGrouping = enableTextGrouping,
                    enableMergePaths = enableMergePaths,
                    enableExpressions = enableExpressions,
                    applyOpacityToLayers = applyOpacityToLayers,
                    coroutineContext = coroutineScope.coroutineContext,
                    assets = composition.animation.assets,
                    fonts = emptyMap(),
                    expressionEngineFactory = expressionEngineFactory
                )
            } else null
        },
        composition,
        copy,
        enableExpressions
    ) {

        if (composition != null) {

            val comp = if (copy) composition.deepCopy() else composition

            val assets = if (comp.hasAssets) {
                async(coroutineContext) {
                    comp.loadAssets(assetsManager ?: EmptyAssetsManager, copy)
                }
            } else {
                null
            }

            val fonts = if (comp.hasFonts) {
                async(coroutineContext) {
                    comp.loadFonts(fontManager ?: EmptyFontManager)
                }
            } else {
                null
            }

            if (comp.animation.hasTextLayers()) {
                launch(coroutineContext) {
                    // for some reason the first text measure may take up to 50 ms.
                    // do it in background
                    textMeasurer.measure("A")
                }
            }

            val painter = LottiePainterImpl(
                composition = comp,
                progress = updatedProgress::invoke,
                dynamicProperties = dp,
                theme = theme,
                clipTextToBoundingBoxes = clipTextToBoundingBoxes,
                textMeasurer = textMeasurer,
                clipToCompositionBounds = clipToCompositionBounds,
                enableTextGrouping = enableTextGrouping,
                enableMergePaths = enableMergePaths,
                enableExpressions = enableExpressions,
                applyOpacityToLayers = applyOpacityToLayers,
                assets = assets?.await().orEmpty(),
                fonts = fonts?.await().orEmpty(),
                coroutineContext = coroutineScope.coroutineContext,
                expressionEngineFactory = expressionEngineFactory
            )

            if (enableExpressions) {
                withContext(coroutineContext) {
                    runCatching {
                        painter.withState(comp.animation::prepareExpressions)
                    }
                }
            }

            value = painter
        }
    }

    val painter by painterState

    LaunchedEffect(
        painter,
        clipTextToBoundingBoxes,
        clipToCompositionBounds,
        applyOpacityToLayers,
        enableMergePaths,
        enableExpressions
    ) {
        painter?.let {
            it.enableMergePaths = enableMergePaths
            it.enableExpressions = enableExpressions
            it.applyOpacityToLayers = applyOpacityToLayers
            it.clipToCompositionBounds = clipToCompositionBounds
            it.clipTextToBoundingBoxes = clipTextToBoundingBoxes
        }
    }

    LaunchedEffect(
        painter,
        theme
    ) {
        painter?.theme = theme
    }

    LaunchedEffect(painter, dp) {
        painter?.setDynamicProperties(dp)
    }

    return remember(painterState) {
        LottiePainter(painterState)
    }
}

internal expect fun mockFontFamilyResolver(): FontFamily.Resolver

public class LottiePainter internal constructor(
    internal val painter: State<LottiePainterImpl?>
) : Painter() {

    private var alpha by mutableStateOf(1f)
    private var colorFilter by mutableStateOf<ColorFilter?>(null)

    override val intrinsicSize: Size by derivedStateOf {
        painter.value?.intrinsicSize ?: Size(1f, 1f)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {

        painter.value?.run {
            draw(size, alpha, colorFilter)
        }
    }
}

internal class LottiePainterImpl(
    val composition: LottieComposition,
    progress: () -> Float,
    assets: List<LottieAsset>,
    fonts: Map<String, FontFamily>,
    theme: String?,
    dynamicProperties: DynamicCompositionProvider?,
    textMeasurer: TextMeasurer,
    applyOpacityToLayers: Boolean,
    clipTextToBoundingBoxes: Boolean,
    enableTextGrouping: Boolean,
    clipToCompositionBounds: Boolean,
    enableMergePaths: Boolean,
    enableExpressions: Boolean,
    coroutineContext: CoroutineContext,
    expressionEngineFactory: ExpressionsEngineFactory
) : Painter() {

    override val intrinsicSize: Size = Size(
        composition.animation.width,
        composition.animation.height
    )

    private val matrix = Matrix()

    private var alpha by mutableStateOf(1f)

    private val compositionLayer: Layer = CompositionLayer(composition)

    internal val frame: Float by derivedStateOf {
        composition.progressToFrame(progress())
    }

    private val animationState = AnimationState(
        composition = composition,
        assets = assets.associateBy(LottieAsset::id),
        fonts = fonts,
        theme = theme,
        frame = frame,
        textMeasurer = textMeasurer,
        applyOpacityToLayers = applyOpacityToLayers,
        clipToCompositionBounds = clipToCompositionBounds,
        clipTextToBoundingBoxes = clipTextToBoundingBoxes,
        enableMergePaths = enableMergePaths,
        layer = compositionLayer,
        enableExpressions = enableExpressions,
        enableTextGrouping = enableTextGrouping,
        coroutineContext = coroutineContext,
        expressionEngineFactory = expressionEngineFactory
    )

    internal var applyOpacityToLayers: Boolean by animationState::applyOpacityToLayers
    internal var clipTextToBoundingBoxes: Boolean by animationState::clipTextToBoundingBoxes
    internal var clipToCompositionBounds: Boolean by animationState::clipToCompositionBounds
    internal var enableMergePaths: Boolean by animationState::enableMergePaths
    internal var enableExpressions: Boolean by animationState::enableExpressions
    internal var theme: String? by animationState::theme

    init {
        setDynamicProperties(dynamicProperties)
    }

    fun setDynamicProperties(provider: DynamicCompositionProvider?) {
        compositionLayer.setDynamicProperties(provider, animationState)
    }

    public override fun applyAlpha(alpha: Float): Boolean {
        if (alpha !in 0f .. 1f)
            return false

        this.alpha = alpha
        return true
    }

    override fun DrawScope.onDraw() {
        try {
            matrix.fastReset()
            matrix.preScale(
                size.width / intrinsicSize.width,
                size.height / intrinsicSize.height
            )

            animationState.onFrame(frame) {
                compositionLayer.draw(
                    drawScope = this,
                    parentMatrix = matrix,
                    parentAlpha = alpha,
                    state = it
                )
            }
        } catch (t: Throwable) {
            Compottie.logger?.error("Lottie crashed in draw :C", t)
        }
    }

    internal inline fun <T> withState(block: (AnimationState) -> T): T {
        return animationState.onFrame(frame, block)
    }
}
