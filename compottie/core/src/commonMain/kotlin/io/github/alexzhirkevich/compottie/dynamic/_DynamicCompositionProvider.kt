package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.util.fastFirstOrNull
import io.github.alexzhirkevich.compottie.internal.layers.ResolvingPath


@PublishedApi
internal class DynamicCompositionProvider : LottieDynamicProperties {

    private val layers = mutableMapOf<String, DynamicLayerProvider>()
    private val layersByPattern = mutableListOf<Pair<List<String>, DynamicLayerProvider>>()

    override fun shapeLayer(vararg path: String, builder: DynamicShapeLayer.() -> Unit) {
        appendLayer(path, DynamicShapeLayerProvider().apply(builder))
    }

    override fun imageLayer(vararg path: String, builder: DynamicImageLayer.() -> Unit) {
        appendLayer(path, DynamicImageLayerProvider().apply(builder))
    }

    override fun textLayer(vararg path: String, builder: DynamicTextLayer.() -> Unit) {
        appendLayer(path, DynamicTextLayerProvider().apply(builder))
    }

    override fun layer(vararg path: String, builder: DynamicLayer.() -> Unit) {
        appendLayer(path, DynamicLayerProvider().apply(builder))
    }

    private fun <T : DynamicLayerProvider> appendLayer(path: Array<out String>, instance: T) {
        val list = path.toList()
        if (list.containsWildcards()) {
            layersByPattern.add(list to instance)
        } else {
            layers[list.joinToString(LayerPathSeparator, LayerPathSeparator)] = instance
        }
    }

    operator fun get(path: ResolvingPath): DynamicLayerProvider? {
        val exactLayer = layers[path.path]
        // Prioritize an exact match over a pattern match
        if (exactLayer != null) return exactLayer

        val pathParts = path.path
            .split(LayerPathSeparator)
            .filter(String::isNotEmpty)

        return layersByPattern.fastFirstOrNull { (pattern, _) ->
            pathMatches(path = pathParts, pattern = pattern)
        }?.second
    }
}

@PublishedApi
internal const val LayerPathSeparator: String = "/"

internal fun layerPath(base: String?, name: String): String = listOfNotNull(base, name)
    .joinToString(LayerPathSeparator)
