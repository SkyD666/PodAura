package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.util.fastFirstOrNull
import kotlin.reflect.KClass

@PublishedApi
internal class DynamicShapeLayerProvider(
    private val basePath: String? = null,
    private val root: DynamicShapeLayerProvider? = null
) : DynamicLayerProvider(), DynamicShapeLayer {

    internal val shapes: MutableMap<String, DynamicShape> =
        root?.shapes ?: mutableMapOf()

    private val shapesByPattern: MutableList<Pair<List<String>, DynamicShape>> =
        root?.shapesByPattern ?: mutableListOf()

    override fun group(vararg path: String, builder: DynamicShapeLayer.() -> Unit) {
        DynamicShapeLayerProvider(
            basePath = layerPath(basePath, path.joinToString(LayerPathSeparator)),
            root = root ?: this
        ).apply(builder)
    }

    override fun shape(vararg path: String, builder: DynamicShape.() -> Unit) {
        this[path.toList()] = DynamicShapeProvider().apply(builder)
    }

    override fun fill(vararg path: String, builder: DynamicFill.() -> Unit) {
        this[path.toList()] = DynamicFillProvider().apply(builder)
    }

    override fun stroke(vararg path: String, builder: DynamicStroke.() -> Unit) {
        this[path.toList()] = DynamicStrokeProvider().apply(builder)
    }

    override fun ellipse(vararg path: String, builder: DynamicEllipse.() -> Unit) {
        this[path.toList()] = DynamicEllipseProvider().apply(builder)
    }

    override fun rect(vararg path: String, builder: DynamicRect.() -> Unit) {
        this[path.toList()] = DynamicRectProvider().apply(builder)
    }

    override fun polystar(vararg path: String, builder: DynamicPolystar.() -> Unit) {
        this[path.toList()] = DynamicPolystarProvider().apply(builder)
    }

    internal inline operator fun <reified S : DynamicShape> get(path: String): S? =
        getInternal(path, S::class) as S?

    private inline operator fun <reified T : DynamicShape> set(path: List<String>, instance: T) {
        val totalPath = layerPath(basePath, path.joinToString(LayerPathSeparator))

        if (path.containsWildcards()) {
            val split = totalPath.split(LayerPathSeparator).filter { it.isNotEmpty() }
            shapesByPattern.add(split to instance)
        } else {
            shapes[totalPath] = instance
        }
    }

    private fun <S : DynamicShape> getInternal(path: String, clazz: KClass<S>): DynamicShape? {

        shapes[path]?.let { return it }

        val pathParts = path
            .split(LayerPathSeparator)
            .filter(String::isNotEmpty)

        return shapesByPattern.fastFirstOrNull { (pattern, shape) ->
            pathMatches(path = pathParts, pattern = pattern) && clazz.isInstance(shape)
        }?.second
    }
}
