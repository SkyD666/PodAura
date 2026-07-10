package io.github.alexzhirkevich.compottie.statemachine

import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.LottieStateMachine
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


@Serializable
public class SMConfig internal constructor(
    internal val initial: String = "",
    internal val states: List<SMState> = emptyList(),
    internal val interactions: List<SMInteraction> = emptyList(),
    internal val inputs: List<SMInput> = emptyList()
) {

    internal val globalState = states.filterIsInstance<SMState.GlobalState>().firstOrNull()
    internal val statesMap = states.associateBy { it.name }
    internal val inputsMap = inputs.associateBy { it.name }
    internal val clickInteractions = interactions.filterIsInstance<SMInteraction.Click>()
    internal val downInteractions = interactions.filterIsInstance<SMInteraction.PointerDown>()
    internal val upInteractions = interactions.filterIsInstance<SMInteraction.PointerUp>()
    internal val enterInteractions = interactions.filterIsInstance<SMInteraction.PointerEnter>()
    internal val exitInteractions = interactions.filterIsInstance<SMInteraction.PointerExit>()
    internal val moveInteractions = interactions.filterIsInstance<SMInteraction.PointerMove>()
    internal val completeInteractions = interactions.filterIsInstance<SMInteraction.OnComplete>()
        .associateBy { it.stateName }
    internal val iterCompleteInteractions = interactions.filterIsInstance<SMInteraction.OnLoopComplete>()
        .associateBy { it.stateName }

    internal fun hasPointerInteractions(): Boolean {
        return hasTapInteractions() || hasHoverInteractions()
    }

    internal fun hasTapInteractions(): Boolean {
        return clickInteractions.isNotEmpty() ||
                downInteractions.isNotEmpty() ||
                upInteractions.isNotEmpty()
    }

    internal fun hasHoverInteractions(): Boolean {
        return enterInteractions.isNotEmpty() ||
                exitInteractions.isNotEmpty()
    }

    internal fun assignVariables(machine: LottieStateMachine) {
        inputs.fastForEach {
            it.assign(machine)
        }
    }

    public companion object {
        public val serializersModule: SerializersModule = SerializersModule {

            polymorphic(SMState::class) {
                subclass(SMState.GlobalState::class)
                subclass(SMState.PlaybackState::class)
            }

            polymorphic(SMAction::class) {
                subclass(SMAction.Url::class)
                subclass(SMAction.Theme::class)
                subclass(SMAction.Increment::class)
                subclass(SMAction.Decrement::class)
                subclass(SMAction.Toggle::class)
                subclass(SMAction.SetBoolean::class)
                subclass(SMAction.SetNumeric::class)
                subclass(SMAction.SetString::class)
                subclass(SMAction.Fire::class)
                subclass(SMAction.Reset::class)
                subclass(SMAction.SetFrame::class)
                subclass(SMAction.SetProgress::class)
                subclass(SMAction.FireCustomEvent::class)
            }

            polymorphic(SMGuard::class) {
                subclass(SMGuard.Bool::class)
                subclass(SMGuard.Numeric::class)
                subclass(SMGuard.Str::class)
                subclass(SMGuard.Event::class)
            }

            polymorphic(SMInput::class) {
                subclass(SMInput.Bool::class)
                subclass(SMInput.Numeric::class)
                subclass(SMInput.Str::class)
                subclass(SMInput.Event::class)
            }

            polymorphic(SMInteraction::class) {
                subclass(SMInteraction.PointerUp::class)
                subclass(SMInteraction.PointerDown::class)
                subclass(SMInteraction.PointerMove::class)
                subclass(SMInteraction.PointerEnter::class)
                subclass(SMInteraction.PointerExit::class)
                subclass(SMInteraction.Click::class)
                subclass(SMInteraction.OnComplete::class)
                subclass(SMInteraction.OnLoopComplete::class)
            }

            polymorphic(SMInput::class) {
                subclass(SMInput.Str::class)
                subclass(SMInput.Numeric::class)
                subclass(SMInput.Bool::class)
                subclass(SMInput.Event::class)
            }

            polymorphic(SMTransition::class) {
                subclass(SMTransition.Default::class)
                subclass(SMTransition.Tweened::class)
            }
        }
    }
}
