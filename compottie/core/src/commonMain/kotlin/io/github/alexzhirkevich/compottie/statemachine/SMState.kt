package io.github.alexzhirkevich.compottie.statemachine

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieAnimatable
import io.github.alexzhirkevich.compottie.LottieClipSpec
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.internal.AnimationState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed interface SMState {

    val name: String
    val transitions: List<SMTransition>
    val entryActions: List<SMAction>
    val exitActions: List<SMAction>

    val sortedTransitions: List<SMTransition>

    val modifier: Modifier get() = Modifier

    suspend fun move(
        state: AnimationState,
        progress: LottieAnimatable,
        transition: SMTransition
    )

    suspend fun play(
        composition: LottieComposition,
        progress: LottieAnimatable,
        onLoopComplete: () -> Unit
    )

    @Serializable
    @SerialName("PlaybackState")
    class PlaybackState(
        override val name: String,
        override val transitions: List<SMTransition>,
        val animation: String,
        val loop: Boolean = false,
        val loopCount: Int = 1,
        val autoplay: Boolean = false,
        val final: Boolean = false,
        val mode: SMPlaybackMode = SMPlaybackMode.Forward,
        val speed: Float = 1f,
        val segment: String? = null,
        val backgroundColor: String? = null,
        override val entryActions: List<SMAction> = emptyList(),
        override val exitActions: List<SMAction> = emptyList(),
    ) : SMState {

        @Transient
        private val bgColor: Color? = backgroundColor?.let {
            it.substringAfter(it.lowercase().substringAfter("0x"))
                .toLongOrNull(16)
                ?.let { Color(it) }
        }

        override val modifier: Modifier
            get() = bgColor?.let { Modifier.background(it) } ?: Modifier

        @Transient
        override val sortedTransitions: List<SMTransition> = transitions.sortedBy {
            if (it.guards.isEmpty()) 1 else 0
        }

        override suspend fun move(
            state: AnimationState,
            progress: LottieAnimatable,
            transition: SMTransition
        ) {
            val start = state.composition.marker(segment)?.let {
                state.composition.frameToProgress(
                    if (mode.isReverse)
                        it.startFrame + it.durationFrames
                    else it.startFrame
                )
            }

            if (start != null) {
                transition.move(state, progress, start)
            }
        }

        override suspend fun play(
            composition: LottieComposition,
            progress: LottieAnimatable,
            onLoopComplete: () -> Unit
        ) {
            if (autoplay) {
                progress.animate(
                    initialProgress = progress.progress,
                    composition = composition,
                    iterations = if (loop) Compottie.IterateForever else loopCount,
                    clipSpec = when {
                        segment != null && composition.marker(segment) != null ->
                            LottieClipSpec.Marker(segment)

                        else -> null
                    },
                    reverseOnRepeat = mode.isBounce,
                    speed = speed * if (mode.isReverse) -1f else 1f,
                    onIterationFinish = onLoopComplete
                )
            }
        }
    }

    @Serializable
    @SerialName("GlobalState")
    class GlobalState(
        override val name: String,
        override val transitions: List<SMTransition>,
        override val entryActions: List<SMAction> = emptyList(),
        override val exitActions: List<SMAction> = emptyList(),
    ) : SMState {

        @Transient
        override val sortedTransitions: List<SMTransition> = transitions.sortedBy {
            if (it.guards.isEmpty()) 1 else 0
        }

        override suspend fun move(
            state: AnimationState,
            progress: LottieAnimatable,
            transition: SMTransition
        ) {
        }

        override suspend fun play(
            composition: LottieComposition,
            progress: LottieAnimatable,
            onLoopComplete: () -> Unit
        ) {

        }

    }
}
