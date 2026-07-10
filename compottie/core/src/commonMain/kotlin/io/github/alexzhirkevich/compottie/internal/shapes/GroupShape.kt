package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.content.ContentGroup
import io.github.alexzhirkevich.compottie.internal.content.ContentGroupImpl
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.utils.firstInstanceOf
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("gr")
internal class GroupShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("it")
    val items: List<Shape> = emptyList(),

    ) : Shape, ContentGroup by ContentGroupImpl(
    name = name,
    hidden = null, // overrided
    contents = items,
    transform = items.firstInstanceOf()
) {

    internal val shapesByName by lazy {
        items.associateBy(Shape::name)
    }

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    override fun hidden(state: AnimationState): Boolean {
        return dynamicShape?.hidden.derive(hidden, state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        val path = layerPath(basePath, nameOrDefault)
        dynamicShape = properties?.get(path)
        items.forEach {
            it.setDynamicProperties(path, properties)
        }
    }

    override fun prepareExpressions(state: AnimationState) {
        items.fastForEach { it.prepareExpressions(state) }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "content" -> Callable {
                val name = it[0]?.toKotlin(runtime)?.toString() ?: return@Callable null
                shapesByName[name]
            }

            else -> super.get(property, runtime)
        }
    }

    override fun deepCopy(): Shape {
        return GroupShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            items = items.map(Shape::deepCopy)
        )
    }
}

