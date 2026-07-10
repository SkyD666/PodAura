package io.github.alexzhirkevich.compottie.statemachine

import io.github.alexzhirkevich.compottie.LottieStateMachine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SMInput {

    val name: String

    fun assign(stateMachine: LottieStateMachine)

    @Serializable
    @SerialName("Numeric")
    class Numeric(
        override val name: String,
        val value: Float
    ) : SMInput {

        override fun assign(stateMachine: LottieStateMachine) {
            stateMachine.setFloat(name, value)
        }
    }

    @Serializable
    @SerialName("String")
    class Str(
        override val name: String,
        val value: String
    ) : SMInput {

        override fun assign(stateMachine: LottieStateMachine) {
            stateMachine.setString(name, value)
        }
    }

    @Serializable
    @SerialName("Boolean")
    class Bool(
        override val name: String,
        val value: Boolean
    ) : SMInput {

        override fun assign(stateMachine: LottieStateMachine) {
            stateMachine.setBoolean(name, value)
        }
    }

    @Serializable
    @SerialName("Event")
    class Event(
        override val name: String,
    ) : SMInput {
        override fun assign(stateMachine: LottieStateMachine) {
            if (stateMachine.isFired(name)) {
                stateMachine.clearFiredEvents()
            }
        }
    }
}
