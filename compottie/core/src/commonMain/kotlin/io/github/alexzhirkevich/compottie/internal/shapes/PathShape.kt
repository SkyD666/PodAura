package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedShape
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.helpers.CompoundSimultaneousTrimPath
import io.github.alexzhirkevich.compottie.internal.helpers.CompoundTrimPath
import io.github.alexzhirkevich.compottie.internal.platform.set
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("sh")
internal class PathShape(
    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("closed")
    private val isClosedLegacy: Boolean? = null,

    @SerialName("ks")
    val shape: AnimatedShape
) : Shape, PathContent {

    init {
        // Until v 4.4.18 path objects had a boolean closed property and c was not present in the bezier data
        if (isClosedLegacy != null) {
            shape.setClosed(isClosedLegacy)
        }
    }

    @Transient
    private var trimPaths: CompoundTrimPath? = null

    private val path = Path()

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    override fun getPath(state: AnimationState): Path {
        path.rewind()
        if (dynamicShape?.hidden.derive(hidden, state)) {
            return path
        }
        path.set(shape.interpolated(state))
        path.fillType = PathFillType.EvenOdd

        trimPaths?.apply(path, state)
        return path
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {
        trimPaths = CompoundSimultaneousTrimPath(contentsBefore)
    }

    override fun prepareExpressions(state: AnimationState) {
        shape.prepareExpressions(state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)
        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "path" -> shape
            else -> super.get(property, runtime)
        }
    }

    override fun deepCopy(): Shape {
        return PathShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            shape = shape.copy()
        )
    }
}