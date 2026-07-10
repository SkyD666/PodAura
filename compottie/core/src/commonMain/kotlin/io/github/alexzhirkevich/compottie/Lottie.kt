package io.github.alexzhirkevich.compottie

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale

/**
 * Creates a composable that lays out and draws a given [LottiePainter] with support of state machines.
 * If no state machines are needed you can just use the [Image] with [painter]
 *
 * @param painter to draw
 * @param contentDescription text used by accessibility services to describe what this animation
 *   represents. This should always be provided unless this animation is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content
 * @param stateMachine animation state machine if present. Usually created with
 * [rememberLottieStateMachine] or [LottieStateMachine]
 * @param alignment Optional alignment parameter used to place the [painter] in the given bounds
 *   defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used if the bounds are a different size from the intrinsic size of the [painter]
 * @param alpha Optional opacity to be applied to the [painter] when it is rendered onscreen the
 *   default renders the [painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [painter] when it is rendered onscreen
 *
 * @see rememberLottieStateMachine
 * @see rememberLottiePainter
 */
@Composable
public fun Lottie(
    painter: LottiePainter,
    contentDescription: String?,
    stateMachine: LottieStateMachine? = null,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    Image(
        modifier = modifier.stateMachine(
            painter = painter,
            stateMachine = stateMachine,
            contentScale = contentScale,
            alignment = alignment
        ),
        painter = painter,
        contentDescription = contentDescription,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}
