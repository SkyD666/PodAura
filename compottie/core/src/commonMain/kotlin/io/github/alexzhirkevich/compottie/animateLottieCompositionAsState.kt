package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Returns a [LottieAnimationState] representing the progress of an animation.
 *
 * This is the declarative version of [rememberLottieAnimatable].
 *
 * @param composition The composition to render. This should be retrieved with [rememberLottieComposition].
 * @param isPlaying Whether the animation is currently playing. Note that the internal
 * animation may end due to reaching the target iterations count. If that happens, the animation may
 * stop even if this is still true. You can observe the returned [LottieAnimationState.isPlaying]
 * to determine whether the underlying animation is still playing.
 * @param restartOnPlay If isPlaying switches from false to true, restartOnPlay determines whether
 * the progress and iteration gets reset.
 * @param reverseOnRepeat Defines what this animation should do when it reaches the end. This setting
 * is applied only when [iterations] is either greater than 0 or [Compottie.IterateForever].
 * Defaults to `false`.
 * @param clipSpec A [LottieClipSpec] that specifies the bound the animation playback
 * should be clipped to.
 * @param speed The speed the animation should play at. Numbers larger than one will speed it up.
 * Numbers between 0 and 1 will slow it down. Numbers less than 0 will play it backwards.
 * @param iterations The number of times the animation should repeat before stopping. It must be
 * a positive number. [Compottie.IterateForever] can be used to repeat forever.
 * @param cancellationBehavior The behavior that this animation should have when canceled.
 * In most cases, you will want it to cancel immediately. However, if you have a state based
 * transition, and you want an animation to finish playing before moving on to the next one then you
 * may want to set this to [LottieCancellationBehavior.OnIterationFinish].
 * @param useCompositionFrameRate Use frame rate declared in animation instead of screen refresh rate.
 * Animation may seem junky if parameter is set to true and composition frame rate is less than screen
 * refresh rate
 */
@Composable
public fun animateLottieCompositionAsState(
    composition: LottieComposition?,
    isPlaying: Boolean = true,
    restartOnPlay: Boolean = true,
    reverseOnRepeat: Boolean = false,
    clipSpec: LottieClipSpec? = null,
    speed: Float = composition?.speed ?: 1f,
    iterations: Int = composition?.iterations ?: 1,
    cancellationBehavior: LottieCancellationBehavior = LottieCancellationBehavior.Immediately,
    useCompositionFrameRate: Boolean = false,
): LottieAnimationState {

    require(iterations > 0) { "Iterations must be a positive number ($iterations)." }
    require(speed.isFinite()) { "Speed must be a finite number. It is $speed." }

    val animatable = rememberLottieAnimatable()
    var wasPlaying by remember { mutableStateOf(isPlaying) }

    LaunchedEffect(
        composition,
        isPlaying,
        clipSpec,
        speed,
        iterations,
    ) {
        if (isPlaying && !wasPlaying && restartOnPlay) {
            animatable.resetToBeginning()
        }
        wasPlaying = isPlaying
        if (!isPlaying) return@LaunchedEffect

        animatable.animate(
            composition = composition,
            iterations = iterations,
            reverseOnRepeat = reverseOnRepeat,
            speed = speed,
            clipSpec = clipSpec,
            initialProgress = if (animatable.composition === composition)
                animatable.progress
            else defaultProgress(composition, clipSpec, speed),
            continueFromPreviousAnimate = false,
            cancellationBehavior = cancellationBehavior,
            useCompositionFrameRate = useCompositionFrameRate,
        )
    }

    return animatable
}
