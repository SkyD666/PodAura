package io.github.alexzhirkevich.compottie.statemachine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SMInteraction {

    val actions: List<SMAction>

    interface Pointer : SMInteraction {
        val layerName: String?
    }

    @Serializable
    @SerialName("PointerUp")
    class PointerUp(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("PointerDown")
    class PointerDown(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("PointerEnter")
    class PointerEnter(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("PointerMove")
    class PointerMove(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("PointerExit")
    class PointerExit(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("Click")
    class Click(
        override val actions: List<SMAction>,
        override val layerName: String? = null,
    ) : Pointer

    @Serializable
    @SerialName("OnComplete")
    class OnComplete(
        override val actions: List<SMAction>,
        val stateName: String,
    ) : SMInteraction

    @Serializable
    @SerialName("OnLoopComplete")
    class OnLoopComplete(
        override val actions: List<SMAction>,
        val stateName: String,
    ) : SMInteraction
}
