package io.github.alexzhirkevich.compottie

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import io.github.alexzhirkevich.compottie.internal.EmptyDrawScope
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.internal.utils.fastReset
import io.github.alexzhirkevich.compottie.internal.utils.preScale
import io.github.alexzhirkevich.compottie.internal.utils.preTranslate
import io.github.alexzhirkevich.compottie.statemachine.SMAction
import io.github.alexzhirkevich.compottie.statemachine.SMConfig
import io.github.alexzhirkevich.compottie.statemachine.SMInteraction
import io.github.alexzhirkevich.compottie.statemachine.SMState
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


/**
 * State machine controller. Can be used to set/observe inputs, fire and subscribe to events,
 * change/get the current state
 * */
@Stable
public sealed interface LottieStateMachine {

    /**
     * Animation state that controls the [LottiePainter] progress
     */
    public val animatable: LottieAnimatable

    /**
     * Current machine state. Is null before initialization
     * */
    public val currentState: StateFlow<String>

    /**
     * Events fired by the interactions and fired manually with [fire]
     * */
    public val events: Flow<String>

    /**
     * Calls [block] on every variable change or event fired
     * */
    public suspend fun collectInputsChanges(block: suspend () -> Unit): Nothing

    /**
     * Immediately move to the requested [state]
     */
    public fun snapToState(state: String): Boolean

    /**
     * Set [Boolean] input property
     */
    public fun setBoolean(key: String, value: Boolean)

    /**
     * Set [String] input property
     */
    public fun setString(key: String, value: String)

    /**
     * Set [Float] input property
     */
    public fun setFloat(key: String, value: Float)

    /**
     * Get [Boolean] input property.
     *
     * The call is stable (value updates the composition, can be used in [snapshotFlow]
     * and [derivedStateOf])
     */
    public fun getBoolean(key: String): Boolean?

    /**
     * Get [String] input property
     *
     * The call is stable (value updates the composition, can be used in [snapshotFlow]
     * and [derivedStateOf])
     * */
    public fun getString(key: String): String?

    /**
     * Get [Float] input property
     *
     * The call is stable (value updates the composition, can be used in [snapshotFlow]
     * and [derivedStateOf])
     * */
    public fun getFloat(key: String): Float?

    /**
     * Fire [event]
     */
    public fun fire(event: String)

    /**
     * Check if [event] was fired and wasn't consumed
     */
    public fun isFired(event: String): Boolean

    /**
     * Consume all fired events
     */
    public fun clearFiredEvents() {}

    /**
     * Reset the state machine input to its initial state
     */
    public fun resetInput(name: String)

    /**
     * Reset the state machine to the initial state
     */
    public fun reset()
}

/**
 * Create and remember [LottieStateMachine]
 *
 * @param id state machine identifier
 * @param composition composition that initialize the [LottiePainter]
 * @param animatable animator that controls the [LottiePainter] progress
 *
 * @see [rememberLottieAnimatable]
 * @see [rememberLottieComposition]
 * */
@Composable
public fun rememberLottieStateMachine(
    id: String,
    composition: LottieComposition?,
    animatable: LottieAnimatable
): LottieStateMachine? {

    return retain(id, composition, animatable) {
        composition?.let {
            LottieStateMachineImpl(
                config = composition.stateMachines?.get(id) ?: return@let null,
                animatable = animatable
            )
        }
    }
}

private class InputsChanged(val isEventFired: Boolean)

internal class LottieStateMachineImpl(
    internal val config: SMConfig,
    override val animatable: LottieAnimatable
) : LottieStateMachine {

    private val inputs = mutableStateMapOf<String, Any>()
    private val inputsChangeFlow = MutableStateFlow(InputsChanged(false))

    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    override val events: Flow<String> = _events.asSharedFlow()

    private var _currentState = MutableStateFlow(config.initial)
    override var currentState: StateFlow<String> = _currentState.asStateFlow()

    private var triggeredEvent by mutableStateOf<String?>(null)

    private val inputsLock = reentrantLock()

    init {
        reset()
    }

    override suspend fun collectInputsChanges(block: suspend () -> Unit): Nothing {
        // debounce inputs to imitate batch update
        inputsChangeFlow.debounce(4.milliseconds).collect { block() }
        error("should never happen")
    }

    override fun snapToState(state: String): Boolean = inputsLock.withLock {
        return if (config.statesMap.contains(state)) {
            _currentState.value = state
            true
        } else {
            false
        }
    }

    override fun setBoolean(key: String, value: Boolean) = inputsLock.withLock {
        inputs[key] = value
        inputsChanged()
    }

    override fun setString(key: String, value: String) = inputsLock.withLock {
        inputs[key] = value
        inputsChanged()
    }

    override fun setFloat(key: String, value: Float) = inputsLock.withLock {
        inputs[key] = value
        inputsChanged()
    }

    override fun getBoolean(key: String): Boolean? = inputsLock.withLock {
        return inputs[key] as? Boolean
    }

    override fun getString(key: String): String? = inputsLock.withLock {
        return inputs[key] as? String
    }

    override fun getFloat(key: String): Float? = inputsLock.withLock {
        return inputs[key] as? Float
    }

    override fun fire(event: String) = inputsLock.withLock {
        triggeredEvent = event
        inputsChanged(isEventFired = true)
    }

    override fun isFired(event: String): Boolean = inputsLock.withLock {
        return triggeredEvent == event
    }

    override fun clearFiredEvents() = inputsLock.withLock {
        triggeredEvent = null
    }

    override fun resetInput(name: String) = inputsLock.withLock {
        val value = inputs[name]
        config.inputsMap[name]?.assign(this)
        if (value != inputs[value]) {
            inputsChanged()
        }
    }

    override fun reset() = inputsLock.withLock {
        triggeredEvent = null
        inputs.clear()
        config.assignVariables(this)
        inputsChanged()
    }

    private fun inputsChanged(isEventFired: Boolean = false) {
        inputsChangeFlow.update {
            InputsChanged(
                isEventFired = isEventFired
            )
        }
    }
}

internal const val SMRefPrefix = "$"

internal fun LottieStateMachine.floatOrValue(nameOrValue: String): Float? {
    return if (nameOrValue.startsWith(SMRefPrefix))
        getFloat(nameOrValue.drop(1))
    else
        nameOrValue.toFloatOrNull()
}

internal fun LottieStateMachine.booleanOrValue(nameOrValue: String): Boolean? {
    return if (nameOrValue.startsWith(SMRefPrefix))
        getBoolean(nameOrValue.drop(1))
    else
        nameOrValue.toBooleanStrictOrNull()
}

internal fun LottieStateMachine.stringOrValue(nameOrValue: String): String? {
    return if (nameOrValue.startsWith(SMRefPrefix))
        getString(nameOrValue.drop(1))
    else
        nameOrValue
}

@Composable
internal fun Modifier.stateMachine(
    painter: LottiePainter,
    stateMachine: LottieStateMachine?,
    contentScale: ContentScale,
    alignment: Alignment
): Modifier {

    if (stateMachine == null)
        return this

    val painter by painter.painter

    val p by rememberUpdatedState(painter ?: return this)

    val smConfig by remember(stateMachine) {
        derivedStateOf {
            when (stateMachine) {
                is LottieStateMachineImpl -> stateMachine.config
            }
        }
    }

    val density by rememberUpdatedState(LocalDensity.current)
    val layoutDirection by rememberUpdatedState(LocalLayoutDirection.current)
    val uriHandler by rememberUpdatedState(LocalUriHandler.current)
    val stateMachine by rememberUpdatedState(stateMachine)

    val state by remember(stateMachine, smConfig) {
        stateMachine.currentState.map(smConfig.statesMap::get)
    }.collectAsState(null)

    if (smConfig.interactions.isEmpty())
        return then(state?.modifier ?: Modifier)

    var size by remember {
        mutableStateOf(Size.Zero)
    }

    val drawScope = remember(density, layoutDirection) {
        EmptyDrawScope(density, layoutDirection)
    }

    val scale = remember(contentScale, p.intrinsicSize, size) {
        contentScale.computeScaleFactor(p.intrinsicSize, size)
    }

    val translate = remember(alignment, p.intrinsicSize, scale, size, layoutDirection) {
        alignment.align(
            size = (p.intrinsicSize * scale).toIntSize(),
            space = size.toIntSize(),
            layoutDirection = layoutDirection
        ).toOffset()
    }

    val matrix = remember { Matrix() }
    val bounds = remember { MutableRect(0f, 0f, 0f, 0f) }

    val listenedLayers = remember(smConfig, p.composition) {
        smConfig.interactions
            .filterIsInstance<SMInteraction.Pointer>()
            .fastMapNotNull { it.layerName }
            .mapNotNull { p.composition.animation.layersMap[it] }
    }

    val coroutineScope = rememberCoroutineScope()

    suspend fun invokeActionsSuspend(actions: List<SMAction>) {
        withContext(NonCancellable) {
            p.withState { state ->
                actions.fastForEach {
                    it.invoke(uriHandler, stateMachine, state)
                }
            }
        }
    }

    fun invokeActions(actions: List<SMAction>) {
        coroutineScope.launch {
            invokeActionsSuspend(actions)
        }
    }

    LaunchedEffect(smConfig, p, stateMachine, stateMachine.animatable) {
        launch {
            stateMachine.currentState
                .mapNotNull { smConfig.statesMap[it] }
                .collectLatest {
                    invokeActionsSuspend(it.entryActions)
                    try {
                        it.play(
                            composition = p.composition,
                            progress = stateMachine.animatable,
                            onLoopComplete = {
                                smConfig.iterCompleteInteractions[it.name]?.let {
                                    invokeActions(it.actions)
                                }
                            }
                        )
                    } finally {
                        smConfig.completeInteractions[it.name]?.let {
                            invokeActionsSuspend(it.actions)
                        }
                    }
                }
        }

        stateMachine.collectInputsChanges {
            try {
                val stateId = stateMachine.currentState.value

                val currentState = smConfig.statesMap[stateId]
                    ?: return@collectInputsChanges

                if (currentState is SMState.PlaybackState && currentState.final)
                    return@collectInputsChanges

                val transition = smConfig.globalState?.sortedTransitions
                    ?.fastFirstOrNull { it.canMove(stateMachine) }
                    ?: currentState.transitions
                        .fastFirstOrNull { it.canMove(stateMachine) }
                    ?: return@collectInputsChanges

                val newState = smConfig.statesMap[transition.toState]
                    ?: return@collectInputsChanges

                try {
                    invokeActionsSuspend(currentState.exitActions)

                    p.withState { animationState ->
                        newState.move(
                            state = animationState,
                            progress = stateMachine.animatable,
                            transition = transition
                        )
                    }
                } finally {
                    stateMachine.snapToState(newState.name)
                }
            } finally {
                stateMachine.clearFiredEvents()
            }
        }
    }


    fun getLayersAtPosition(position: Offset): List<Layer> {
        return p.withState { state ->
            listenedLayers.fastFilter {
                if (!it.isActive(state))
                    return@fastFilter false

                bounds.set(0f, 0f, 0f, 0f)
                it.getBounds(
                    drawScope = drawScope,
                    parentMatrix = matrix,
                    applyParents = true,
                    state = state,
                    outBounds = bounds
                )

                bounds.contains(position)
            }
        }
    }

    LaunchedEffect(matrix, scale, translate) {
        matrix.fastReset()
        matrix.preTranslate(translate.x, translate.y)
        matrix.preScale(scale.scaleX, scale.scaleY)
    }

    val pointerModifier = Modifier.stateMachinePointerInput(
        smConfig = smConfig,
        getLayersAtPosition = ::getLayersAtPosition,
        invokeActions = ::invokeActions
    )

    return onSizeChanged { size = it.toSize() }
        .then(
            if (smConfig.hasPointerInteractions() && p.withState { !it.isTweenRunning })
                pointerModifier else Modifier
        )
        .then(state?.modifier ?: Modifier)
//        .drawWithContent {
//            drawContent()
//
//            p.withState { animationState ->
//                listenedLayers.fastForEach {
//                    if (it.isActive(animationState)) {
//                        bounds.set(0f, 0f, 0f, 0f)
//                        it.getBounds(
//                            drawScope = drawScope,
//                            parentMatrix = matrix,
//                            applyParents = true,
//                            state = animationState,
//                            outBounds = bounds
//                        )
//                        drawRect(
//                            color = Color.Red,
//                            topLeft = bounds.topLeft,
//                            size = bounds.size,
//                            style = Stroke(1.dp.toPx())
//                        )
//                    }
//                }
//            }
//        }
}

@Composable
private fun Modifier.stateMachinePointerInput(
    smConfig: SMConfig,
    getLayersAtPosition: (Offset) -> List<Layer>,
    invokeActions: (List<SMAction>) -> Unit
): Modifier {

    var lastHoveredLayers by remember {
        mutableStateOf(emptyList<Layer>())
    }

    val onEnterAnim = remember(smConfig.enterInteractions) {
        smConfig.enterInteractions.fastFilter { it.layerName == null }
    }

    val onExitAnim = remember(smConfig.exitInteractions) {
        smConfig.exitInteractions.fastFilter { it.layerName == null }
    }

    val hasLayerHoverInteractions = remember(smConfig) {
        onEnterAnim.size != smConfig.enterInteractions.size ||
                onExitAnim.size != smConfig.exitInteractions.size
    }

    return pointerInput(smConfig) {
        awaitEachGesture {

            val p = awaitPointerEvent()

            when (p.type) {
                PointerEventType.Press -> {

                    if (!smConfig.hasTapInteractions())
                        return@awaitEachGesture

                    val down = p.changes.first()
                    val downLayer = getLayersAtPosition(down.position)

                    smConfig.downInteractions.invokeAny(downLayer, invokeActions)

                    var movedTooMuch = false

                    do {
                        val change = awaitPointerEvent().changes.first()

                        if (!change.pressed) {
                            val upPosition = change.position
                            val upLayer = getLayersAtPosition(upPosition)

                            smConfig.upInteractions.invokeAny(upLayer, invokeActions)

                            val distance = (upPosition - down.position).getDistance()

                            if (distance < viewConfiguration.touchSlop && !movedTooMuch) {
                                smConfig.clickInteractions.invokeAny(upLayer, invokeActions)
                            }
                            break
                        } else {
                            val movePosition = change.position

                            val moveDistance = (movePosition - down.position).getDistance()
                            if (moveDistance > viewConfiguration.touchSlop) {
                                movedTooMuch = true
                            }

                            val moveLayer = getLayersAtPosition(movePosition)
                            smConfig.moveInteractions.invokeAny(moveLayer, invokeActions)
                        }
                    } while (change.pressed)
                }

                PointerEventType.Enter -> onEnterAnim.fastForEach {
                    invokeActions(it.actions)
                }

                PointerEventType.Exit -> onExitAnim.fastForEach {
                    invokeActions(it.actions)
                }

                PointerEventType.Move -> {
                    if (!hasLayerHoverInteractions)
                        return@awaitEachGesture

                    val hoveredLayers = getLayersAtPosition(
                        p.changes.firstOrNull()?.position ?: return@awaitEachGesture
                    )

                    val enterLayers = hoveredLayers - lastHoveredLayers.toSet()
                    val exitLayers = lastHoveredLayers - hoveredLayers.toSet()

                    lastHoveredLayers = hoveredLayers

                    smConfig.enterInteractions.invokeNamed(enterLayers, invokeActions)
                    smConfig.exitInteractions.invokeNamed(exitLayers, invokeActions)
                }
            }
        }
    }
}

private fun List<SMInteraction.Pointer>.invokeAny(
    layers: List<Layer>,
    invokeActions: (List<SMAction>) -> Unit
) {
    fastForEach {
        if (
            it.layerName == null ||
            layers.fastAny { l -> l.name == it.layerName }
        ) {
            invokeActions(it.actions)
        }
    }
}

private fun List<SMInteraction.Pointer>.invokeNamed(
    layers: List<Layer>,
    invokeActions: (List<SMAction>) -> Unit
) {
    fastForEach {
        if (
            it.layerName != null
            && layers.fastAny { l -> l.name == it.layerName }
        ) {
            invokeActions(it.actions)
        }
    }
}
