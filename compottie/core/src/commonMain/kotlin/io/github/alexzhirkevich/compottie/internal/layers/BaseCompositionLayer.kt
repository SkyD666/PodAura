package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import io.github.alexzhirkevich.compottie.dynamic.DynamicCompositionProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicLayerProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.helpers.isSupported
import io.github.alexzhirkevich.compottie.internal.platform.clipRect
import io.github.alexzhirkevich.compottie.internal.platform.saveLayer
import io.github.alexzhirkevich.compottie.internal.utils.union
import kotlinx.serialization.Transient
import kotlin.math.absoluteValue

internal abstract class BaseCompositionLayer : BaseLayer() {

    abstract val width: Float

    abstract val height: Float

    abstract val timeRemapping: AnimatedNumber?

    private val rect = MutableRect(0f, 0f, 0f, 0f)

    @Transient
    private val newClipRect = MutableRect(0f, 0f, 0f, 0f)

    @Transient
    private val layerPaint = Paint().apply {
        isAntiAlias = true
    }

    protected var loadedLayers: List<BaseLayer>? = null

    abstract fun compose(state: AnimationState): List<Layer>

    override fun drawLayer(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState
    ) {

        val layers = getLayers(state)

        newClipRect.set(0f, 0f, width, height)
        parentMatrix.map(newClipRect)

        // Apply off-screen rendering only when needed in order to improve rendering performance.
        val isDrawingWithOffScreen = state.applyOpacityToLayers &&
                layers.isNotEmpty() && parentAlpha < .99f

        val canvas = drawScope.drawContext.canvas

        if (isDrawingWithOffScreen) {
            layerPaint.alpha = parentAlpha
            canvas.saveLayer(newClipRect, layerPaint)
        } else {
            canvas.save()
        }

        try {
            val childAlpha = if (isDrawingWithOffScreen) 1f else parentAlpha

            state.onFrame(getRemappedFrame(state)) { remappedState ->
                layers.fastForEachReversed { layer ->
                    if (state.clipToCompositionBounds && !newClipRect.isEmpty) {
                        canvas.clipRect(newClipRect)
                    }
                    layer.draw(drawScope, parentMatrix, childAlpha, remappedState)
                }
            }
        } finally {
            canvas.restore()
        }
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect
    ) {
        super.getBounds(drawScope, parentMatrix, applyParents, state, outBounds)

        getLayers(state).fastForEachReversed {
            rect.set(0f, 0f, 0f, 0f)
            it.getBounds(drawScope, boundsMatrix, true, state, rect)
            outBounds.union(rect)
        }
    }

    override fun setDynamicProperties(
        composition: DynamicCompositionProvider?,
        state: AnimationState
    ): DynamicLayerProvider? {
        val dynamic = super.setDynamicProperties(composition, state)

        getLayers(state).fastForEach {
            it.setDynamicProperties(composition, state)
        }

        return dynamic
    }

    override fun prepareExpressions(state: AnimationState) {
        super.prepareExpressions(state)
        timeRemapping?.prepareExpressions(state)
        getLayers(state).fastForEach { it.prepareExpressions(state) }
    }

    private fun getLayers(state: AnimationState): List<Layer> {
        loadedLayers?.let { return it }

        val layers = compose(state).filterIsInstance<BaseLayer>()

        layers.fastForEach {
            it.resolvingPath = this.resolvingPath?.resolve(it.nameOrDefault)
        }

        layers.fastForEach {
            it.effects += effects
        }

        val matteLayers = mutableSetOf<BaseLayer>()

        val layersWithIndex = layers
            .fastFilter { it.index != null }
            .associateBy { it.index }

        layers.forEachIndexed { i, it ->
            it.parent?.let { pId ->
                val p = layersWithIndex[pId]

                if (p != null) {
                    it.parentLayer = p
                }
            }

            if (it.matteMode?.isSupported() == true) {
                if (it.matteParent != null) {
                    val p = layersWithIndex[it.matteParent]

                    if (p != null) {
                        it.matteLayer = p
                        matteLayers.add(p)
                    }
                } else {
                    if (i > 0) {
                        it.matteLayer = layers[i - 1]
                        matteLayers.add(layers[i - 1])
                    }
                }
            }
        }

        this.loadedLayers = (layers - matteLayers).fastFilter { it.matteTarget != true }
        return this.loadedLayers!!
    }

    private fun getRemappedFrame(state: AnimationState): Float {

        val frame = timeRemapping?.interpolatedFloat(state)
            ?.times(state.composition.frameRate)
            ?.minus(state.composition.startFrame)
            ?: (state.frame - (startTime ?: inPoint ?: 0f))

        return if (timeRemapping == null && timeStretch.absoluteValue > Float.MIN_VALUE && !isContainerLayer) {
            frame / timeStretch
        } else frame
    }
}