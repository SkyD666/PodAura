package io.github.alexzhirkevich.compottie.internal.shapes

import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.helpers.TrimPathType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.contracts.contract

@Serializable
@SerialName("tm")
internal class TrimPathShape(

    @SerialName("n")
    override val name: String? = null,

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("s")
    val start: AnimatedNumber,

    @SerialName("e")
    val end: AnimatedNumber,

    @SerialName("o")
    val offset: AnimatedNumber,

    @SerialName("m")
    val trimPathType: TrimPathType = TrimPathType.Simultaneously
) : Shape {

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null
    fun isHidden(state: AnimationState): Boolean {
        return dynamicShape?.hidden.derive(hidden, state)
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {

    }

    override fun prepareExpressions(state: AnimationState) {
        start.prepareExpressions(state)
        end.prepareExpressions(state)
        offset.prepareExpressions(state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)
        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override fun deepCopy(): Shape {
        return TrimPathShape(
            name = name,
            matchName = matchName,
            hidden = hidden,
            start = start.copy(),
            end = end.copy(),
            offset = offset.copy(),
            trimPathType = trimPathType
        )
    }
}


internal fun Content.isSimultaneousTrimPath(): Boolean {
    contract {
        returns(true) implies (this@isSimultaneousTrimPath is TrimPathShape)
    }

    return this is TrimPathShape && trimPathType == TrimPathType.Simultaneously
}

internal fun Content.isIndividualTrimPath(): Boolean {
    contract {
        returns(true) implies (this@isIndividualTrimPath is TrimPathShape)
    }

    return this is TrimPathShape && trimPathType == TrimPathType.Individually
}
