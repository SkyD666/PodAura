package io.github.alexzhirkevich.compottie

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State

/**
 * [LottieAnimationState] is a value holder that contains information about the current Lottie animation.
 *
 * The primary values are [LottieAnimationState.progress] and [LottieAnimationState.composition]. These
 * value should be passed into the main [Lottie] composable.
 *
 * @see progress
 * @see composition
 * @see animateLottieCompositionAsState
 */
@Stable
public interface LottieAnimationState : State<Float> {
    public val isPlaying: Boolean

    public val progress: Float

    public val iteration: Int

    public val iterations: Int

    public val reverseOnRepeat: Boolean

    public val clipSpec: LottieClipSpec?

    public val speed: Float

    public val useCompositionFrameRate: Boolean

    public val composition: LottieComposition?

    public val lastFrameNanos: Long get() = AnimationConstants.UnspecifiedTime

    public val isAtEnd: Boolean
}
