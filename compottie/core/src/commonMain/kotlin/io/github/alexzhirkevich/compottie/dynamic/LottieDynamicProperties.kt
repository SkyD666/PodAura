package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Create and remember [createLottieDynamicProperties].
 *
 * Dynamic properties are used to style your animation. For example, change color according to app theme
 * or change size/position of specific layer as a reaction for a click.
 * */
@Composable
@ExperimentalCompottieApi
public fun rememberLottieDynamicProperties(
    vararg keys: Any?,
    builder: LottieDynamicProperties.() -> Unit
): LottieDynamicProperties = remember(*keys) {
    createLottieDynamicProperties(builder)
}


/**
 * Create and remember [LottieDynamicProperties].
 *
 * Dynamic properties are used to style your animation. For example, change color according to app theme
 * or change size/position of specific layer as a reaction for a click.
 *
 * Use [rememberLottieDynamicProperties] to create it from the composition
 * */
@OptIn(ExperimentalContracts::class)
public fun createLottieDynamicProperties(
    builder: LottieDynamicProperties.() -> Unit
): LottieDynamicProperties {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return DynamicCompositionProvider().apply(builder)
}

/**
 * Lottie dynamic properties builder.
 *
 * Usually created with [rememberLottieDynamicProperties] (from Compose) or
 * [createLottieDynamicProperties] (outside of Compose)
 * */
public sealed interface LottieDynamicProperties {

    /**
     * Layer dynamic properties builder.
     *
     * Path is a chain of layers names up to the required layer.
     * All layers in the chain must have a name.
     *
     * Example:
     *
     * ```
     * layer("Precomposition 1", "Layer3") {
     *     transform {
     *         scale {
     *             it * (1 - this.progress)
     *         }
     *     }
     * }
     * ```
     * */
    public fun layer(vararg path: String, builder: DynamicLayer.() -> Unit)

    /**
     * Shape layer dynamic properties builder.
     *
     * Path is a chain of layers names up to the required layer.
     * All layers in the chain must have a name.
     *
     * Example:
     *
     * ```
     * shapeLayer("Precomposition 1", "ShapeLayer2") {
     *     transform {
     *        //...
     *     }
     *
     *     fill("Group4", "Fill 1") {
     *         color {
     *             if (isDark) Color.White else Color.Black
     *         }
     *     }
     * }
     * ```
     * */
    public fun shapeLayer(vararg path: String, builder: DynamicShapeLayer.() -> Unit)

    /**
     * Image layer dynamic properties builder.
     *
     * Path is a chain of layers names up to the required layer.
     * All layers in the chain must have a name.
     * */
    public fun imageLayer(vararg path: String, builder: DynamicImageLayer.() -> Unit)

    /**
     * Text layer dynamic properties builder.
     *
     * Path is a chain of layers names up to the required layer.
     * All layers in the chain must have a name.
     * */
    public fun textLayer(vararg path: String, builder: DynamicTextLayer.() -> Unit)
}




