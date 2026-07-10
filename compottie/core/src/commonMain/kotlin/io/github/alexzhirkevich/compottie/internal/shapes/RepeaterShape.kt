package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.RepeaterTransform
import io.github.alexzhirkevich.compottie.internal.animation.interpolatedNorm
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.ContentGroup
import io.github.alexzhirkevich.compottie.internal.content.ContentGroupImpl
import io.github.alexzhirkevich.compottie.internal.content.DrawingContent
import io.github.alexzhirkevich.compottie.internal.content.GreedyContent
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.platform.PathBuilder
import io.github.alexzhirkevich.compottie.internal.utils.fastSetFrom
import io.github.alexzhirkevich.compottie.internal.utils.preConcat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("rp")
internal class RepeaterShape(

    @SerialName("c")
    val copies: AnimatedNumber,

    @SerialName("o")
    val offset: AnimatedNumber = AnimatedNumber.Default(0f),

    @SerialName("tr")
    val transform: RepeaterTransform = RepeaterTransform(),

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false
) : Shape, GreedyContent, DrawingContent, PathContent {

    @Transient
    private var contentGroup: ContentGroup? = null

    @Transient
    private val path = Path()

    @Transient
    private val pathBuilder = PathBuilder()

    @Transient
    private val matrix = Matrix()

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    override fun draw(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState
    ) {
        contentGroup?.let { contentGroup ->
            val copies = copies.interpolatedFloat(state)
            val offset = offset.interpolatedFloat(state)
            val startOpacity = transform.startOpacity?.interpolatedNorm(state) ?: 1f
            val endOpacity = transform.endOpacity?.interpolatedNorm(state) ?: 1f

            for (i in copies.toInt() - 1 downTo 0) {
                matrix.fastSetFrom(parentMatrix)
                matrix.preConcat(transform.repeaterMatrix(state, i + offset))
                val newAlpha = parentAlpha * lerp(startOpacity, endOpacity, i / copies).coerceIn(0f, 1f)
                contentGroup.draw(drawScope, matrix, newAlpha, state)
            }
        }
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect
    ) {
        contentGroup?.getBounds(drawScope, parentMatrix, applyParents, state, outBounds)
    }

    override fun getPath(state: AnimationState): Path {
        path.rewind()
        val contentPath = contentGroup?.getPath(state) ?: return path

        val copies = copies.interpolatedFloat(state)
        val offset = offset.interpolatedFloat(state)

        for (i in copies.toInt() - 1 downTo 0) {
            matrix.fastSetFrom(transform.repeaterMatrix(state, i + offset))
            pathBuilder.addPath(contentPath, matrix)
        }
        pathBuilder.setTo(path)
        return path
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {
        contentGroup?.setContents(contentsBefore, contentsAfter)
    }

    override fun prepareExpressions(state: AnimationState) {
        copies.prepareExpressions(state)
        offset.prepareExpressions(state)

    }

    override fun absorbContent(contents: MutableList<Content>) {

        // This check prevents a repeater from getting added twice.
        // This can happen in the following situation:
        //    RECTANGLE
        //    REPEATER 1
        //    FILL
        //    REPEATER 2
        // In this case, the expected structure would be:
        //     REPEATER 2
        //        REPEATER 1
        //            RECTANGLE
        //        FILL
        // Without this check, REPEATER 1 will try and absorb contents once it is already inside of
        // REPEATER 2.
        if (contentGroup != null) {
            return
        }
        val thisIndex = contents.indexOf(this).takeIf { it > 0 } ?: return

        val contentsList = contents.take(thisIndex)

        repeat(thisIndex) {
            contents.removeAt(0)
        }

        contentGroup = ContentGroupImpl(
            name = name,
            hidden = { dynamicShape?.hidden.derive(hidden, it) },
            contents = contentsList,
            transform = null,
        )
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override fun deepCopy(): Shape {
        return RepeaterShape(
            copies = copies.copy(),
            offset = offset.copy(),
            transform = transform.deepCopy(),
            name = name,
            matchName = matchName,
            hidden = hidden
        )
    }
}