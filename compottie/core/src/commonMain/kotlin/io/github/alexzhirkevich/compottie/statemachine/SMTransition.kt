package io.github.alexzhirkevich.compottie.statemachine

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastAll
import io.github.alexzhirkevich.compottie.LottieAnimatable
import io.github.alexzhirkevich.compottie.LottieStateMachine
import io.github.alexzhirkevich.compottie.internal.AnimationState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Stable
internal sealed interface SMTransition {

    val toState: String
    val guards: List<SMGuard>

    suspend fun move(
        state: AnimationState,
        progress: LottieAnimatable,
        toProgress: Float
    )

    fun canMove(machine: LottieStateMachine): Boolean {
        return machine.currentState.value != toState &&
                guards.fastAll { it.check(machine) }
    }

    @Serializable
    @SerialName("Transition")
    class Default(
        override val toState: String,
        override val guards: List<SMGuard> = emptyList()
    ) : SMTransition {

        override suspend fun move(
            state: AnimationState,
            progress: LottieAnimatable,
            toProgress: Float
        ) {
            progress.snapTo(state.composition, toProgress)
        }
    }

    @Serializable
    @SerialName("Tweened")
    class Tweened(
        override val toState: String,
        val duration: Float,
        val easing: List<Float>,
        override val guards: List<SMGuard> = emptyList()
    ) : SMTransition {

        @Transient
        private val animationSpec = tween<Float>(
            durationMillis = (duration * 1000).toInt(),
            easing = runCatching {
                CubicBezierEasing(
                    easing[0],
                    easing[1],
                    easing[2],
                    easing[3],
                )
            }.getOrDefault(LinearEasing)
        )

        override suspend fun move(
            state: AnimationState,
            progress: LottieAnimatable,
            toProgress: Float
        ) {
            state.tweenTo(
                frame = state.composition.progressToFrame(toProgress),
                spec = animationSpec
            ) {
                progress.snapTo(
                    composition = state.composition,
                    progress = toProgress
                )
            }
        }
    }
}
