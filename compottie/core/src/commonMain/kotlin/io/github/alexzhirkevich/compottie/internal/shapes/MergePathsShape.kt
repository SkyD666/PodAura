package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.GreedyContent
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.platform.set
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmInline


@Serializable
@JvmInline
internal value class MergeMode(val mode: Byte) {
    companion object {
        val Normal = MergeMode(1)
        val Add = MergeMode(2)
        val Subtract = MergeMode(3)
        val Intersect = MergeMode(4)
        val ExcludeIntersections = MergeMode(5)
    }
}

@Serializable
@SerialName("mm")
internal class MergePathsShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("mm")
    val mode: MergeMode = MergeMode.Normal
) : Shape, GreedyContent, PathContent {

    @Transient
    private val path = Path()

    @Transient
    private val remainderPath = Path()

    @Transient
    private val firstPath = Path()

    @Transient
    private var pathContents = emptyList<PathContent>()

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    override fun getPath(state: AnimationState): Path {

        path.rewind()

        val hidden = dynamicShape?.hidden.derive(hidden, state)

        if (hidden || mode == MergeMode.Normal || !state.enableMergePaths) {
            pathContents.fastForEach {
                path.addPath(it.getPath(state))
            }
        } else {
            when (mode) {
                MergeMode.Add -> opFirstPathWithRest(PathOperation.Union, state)
                MergeMode.Subtract -> opFirstPathWithRest(PathOperation.Difference, state)
                MergeMode.Intersect -> opFirstPathWithRest(PathOperation.Intersect, state)
                MergeMode.ExcludeIntersections -> opFirstPathWithRest(PathOperation.Xor, state)
            }
        }

        return path
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override fun deepCopy(): Shape {
        return MergePathsShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            mode = mode
        )
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {
        pathContents.fastForEach {
            it.setContents(contentsBefore, contentsAfter)
        }
    }

    override fun prepareExpressions(state: AnimationState) {

    }

    override fun absorbContent(contents: MutableList<Content>) {

        val thisIndex = contents.indexOf(this).takeIf { it > 0 } ?: return

        pathContents = contents
            .take(thisIndex)
            .filterIsInstance<PathContent>()
            .reversed()

        if (pathContents.size < 2) {
            contents.removeAt(thisIndex)
        } else {
            contents.removeAll(pathContents)
        }
    }

    private fun opFirstPathWithRest(op: PathOperation, state: AnimationState) {

        remainderPath.rewind()
        firstPath.rewind()
        path.rewind()

        if (pathContents.isEmpty())
            return

        for (i in pathContents.size - 1 downTo 1) {
            remainderPath.addPath(pathContents[i].getPath(state))
        }

        firstPath.set(pathContents[0].getPath(state))

        path.op(firstPath, remainderPath, op)
    }
}
