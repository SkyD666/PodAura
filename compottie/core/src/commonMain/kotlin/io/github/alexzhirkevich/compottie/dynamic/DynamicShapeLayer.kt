package io.github.alexzhirkevich.compottie.dynamic

public interface DynamicShapeLayer : DynamicLayer {

    /**
     * Configure generic dynamic shape.
     *
     * @param path is a path to the shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun shape(
        vararg path: String,
        builder: DynamicShape.() -> Unit
    )

    /**
     * Shortcut useful if you want to configure multiple shapes in the same group
     *
     * @param path is a path to the group shape relative to the shape layer.
     * @param builder shape dynamic configuration
     *
     * Example:
     *
     * ```
     * group("Group 1") {
     *     ellipse("Ellipse 4") { }
     *     fill("Fill 1") { }
     * }
     * ```
     * Is alternative for
     * ```
     * ellipse("Group 1", "Ellipse 4") { }
     * fill("Group 1", "Fill 1") { }
     *```
     * */
    public fun group(vararg path: String, builder: DynamicShapeLayer.() -> Unit)

    /**
     * Configure dynamic stroke.
     *
     * @param path is a path to the stroke shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun stroke(
        vararg path: String,
        builder: DynamicStroke.() -> Unit
    )

    /**
     * Configure dynamic fill.
     *
     * @param path is a path to the fill shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun fill(
        vararg path: String,
        builder: DynamicFill.() -> Unit
    )

    /**
     * Configure dynamic ellipse.
     *
     * @param path is a path to the ellipse shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun ellipse(
        vararg path: String,
        builder: DynamicEllipse.() -> Unit
    )

    /**
     * Configure dynamic rect.
     *
     * @param path is a path to the rect shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun rect(
        vararg path: String,
        builder: DynamicRect.() -> Unit
    )

    /**
     * Configure dynamic rect.
     *
     * @param path is a path to the polystar shape relative to the shape layer.
     * Can contain '**' and '*' wildcards
     *
     * @param builder shape dynamic configuration
     * */
    public fun polystar(
        vararg path: String,
        builder: DynamicPolystar.() -> Unit
    )
}