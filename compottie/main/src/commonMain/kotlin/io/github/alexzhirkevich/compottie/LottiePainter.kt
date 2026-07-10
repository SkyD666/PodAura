package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.alexzhirkevich.compottie.assets.LottieAssetsManager
import io.github.alexzhirkevich.compottie.assets.LottieFontManager
import io.github.alexzhirkevich.compottie.dynamic.LottieDynamicProperties
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
    enableExpressions: Boolean = true
): LottiePainter = rememberLottiePainter(
    composition = composition,
    progress = progress,
    assetsManager = assetsManager,
    fontManager = fontManager,
    coroutineContext = coroutineContext,
    dynamicProperties = dynamicProperties,
    theme = theme,
    applyOpacityToLayers = applyOpacityToLayers,
    clipToCompositionBounds = clipToCompositionBounds,
    clipTextToBoundingBoxes = clipTextToBoundingBoxes,
    enableTextGrouping = enableTextGrouping,
    enableMergePaths = enableMergePaths,
    enableExpressions = enableExpressions,
    expressionEngineFactory = remember {
        { coroutineContext, state ->
            ExpressionsEngineImpl(ExpressionsRuntimeImpl(coroutineContext, state))
        }
    }
)

/**
 * Create and remember Lottie painter.
 *
 * Shortcut that combines [rememberLottiePainter] and [animateLottieCompositionAsState]
 * */
@OptIn(InternalCompottieApi::class)
@Composable
public fun rememberLottiePainter(
    composition: LottieComposition?,
    assetsManager: LottieAssetsManager? = null,
    fontManager: LottieFontManager? = null,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    dynamicProperties: LottieDynamicProperties? = null,
    theme: String? = null,
    isPlaying: Boolean = true,
    restartOnPlay: Boolean = true,
    reverseOnRepeat: Boolean = false,
    applyOpacityToLayers: Boolean = false,
    clipSpec: LottieClipSpec? = null,
    speed: Float = composition?.speed ?: 1f,
    iterations: Int = composition?.iterations ?: 1,
    cancellationBehavior: LottieCancellationBehavior = LottieCancellationBehavior.Immediately,
    useCompositionFrameRate: Boolean = false,
    clipToCompositionBounds: Boolean = true,
    clipTextToBoundingBoxes: Boolean = false,
    enableTextGrouping: Boolean = false,
    enableMergePaths: Boolean = false,
    enableExpressions: Boolean = true
): LottiePainter {

    val progress = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = restartOnPlay,
        reverseOnRepeat = reverseOnRepeat,
        clipSpec = clipSpec,
        speed = speed,
        iterations = iterations,
        cancellationBehavior = cancellationBehavior,
        useCompositionFrameRate = useCompositionFrameRate
    )

    return rememberLottiePainter(
        composition = composition,
        progress = progress::value,
        assetsManager = assetsManager,
        fontManager = fontManager,
        coroutineContext = coroutineContext,
        dynamicProperties = dynamicProperties,
        theme = theme,
        applyOpacityToLayers = applyOpacityToLayers,
        clipToCompositionBounds = clipToCompositionBounds,
        clipTextToBoundingBoxes = clipTextToBoundingBoxes,
        enableTextGrouping = enableTextGrouping,
        enableMergePaths = enableMergePaths,
        enableExpressions = enableExpressions
    )
}
