package io.github.alexzhirkevich.compottie.statemachine

import androidx.compose.ui.platform.UriHandler
import io.github.alexzhirkevich.compottie.LottieStateMachine
import io.github.alexzhirkevich.compottie.floatOrValue
import io.github.alexzhirkevich.compottie.internal.AnimationState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SMAction {

    suspend operator fun invoke(
        uriHandler: UriHandler,
        stateMachine: LottieStateMachine,
        state: AnimationState
    )

    @Serializable
    @SerialName("Url")
    class Url(
        val url: String,
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            uriHandler.openUri(url)
        }
    }

    @Serializable
    @SerialName("Theme")
    class Theme(
        val value: String,
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            state.theme = value
        }
    }

    @Serializable
    @SerialName("Increment")
    class Increment(
        val inputName: String,
        val value: String
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            increment(stateMachine, inputName, value)
        }
    }

    @Serializable
    @SerialName("Decrement")
    class Decrement(
        val inputName: String,
        val value: String
    ) : SMAction {
        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            increment(stateMachine, inputName, value, -1)
        }
    }

    @Serializable
    @SerialName("Toggle")
    class Toggle(
        val inputName: String,
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            val b = stateMachine.getBoolean(inputName) ?: return
            stateMachine.setBoolean(inputName, b.not())
        }
    }

    @Serializable
    @SerialName("SetBoolean")
    class SetBoolean(
        val inputName: String,
        val value: Boolean
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.setBoolean(inputName, value)
        }
    }

    @Serializable
    @SerialName("SetString")
    class SetString(
        val inputName: String,
        val value: String
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.setString(inputName, value)
        }
    }

    @Serializable
    @SerialName("SetNumeric")
    class SetNumeric(
        val inputName: String,
        val value: Float
    ) : SMAction {
        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.setFloat(inputName, value)
        }
    }

    @Serializable
    @SerialName("Fire")
    class Fire(
        val inputName: String,
    ) : SMAction {
        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.fire(inputName)
        }
    }

    @Serializable
    @SerialName("Reset")
    class Reset(
        val inputName: String,
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState,
        ) {
            stateMachine.resetInput(inputName)
        }
    }

    @Serializable
    @SerialName("SetFrame")
    class SetFrame(
        val value: String
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {

            val frame = stateMachine.floatOrValue(value) ?: return

            stateMachine.animatable.snapTo(
                composition = state.composition,
                progress = state.composition.frameToProgress(frame)
            )
        }
    }

    @Serializable
    @SerialName("SetProgress")
    class SetProgress(
        val inputName: String,
        val value: String
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.animatable.snapTo(
                composition = state.composition,
                progress = stateMachine.floatOrValue(value) ?: return
            )
        }
    }

    @Serializable
    @SerialName("FireCustomEvent")
    class FireCustomEvent(
        val value: String
    ) : SMAction {

        override suspend fun invoke(
            uriHandler: UriHandler,
            stateMachine: LottieStateMachine,
            state: AnimationState
        ) {
            stateMachine.fire(value)
        }
    }
}

private fun increment(
    stateMachine: LottieStateMachine,
    inputName: String,
    value: String,
    sign: Int = 1
) {
    val v = stateMachine.getFloat(inputName) ?: return
    val diff = stateMachine.floatOrValue(value) ?: return

    stateMachine.setFloat(inputName, v + diff * sign)
}
