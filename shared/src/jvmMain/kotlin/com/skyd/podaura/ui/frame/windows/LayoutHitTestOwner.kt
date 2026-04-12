@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.frame.windows

import androidx.compose.foundation.AbstractClickableNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.CopiedList
import androidx.compose.ui.scene.LocalComposeSceneContext
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEachReversed

interface LayoutHitTestOwner {
    fun hitTest(x: Float, y: Float): Boolean = false
}

@Composable
fun rememberLayoutHitTestOwner(): LayoutHitTestOwner {
    val scene = LocalComposeSceneContext.current as? ComposeScene
        ?: error("No compose scene")
    return remember(scene) {
        when (scene::class.java.canonicalName) {
            "androidx.compose.ui.scene.CanvasLayersComposeSceneImpl" -> {
                CanvasLayersLayoutHitTestOwner(scene)
            }

            "androidx.compose.ui.scene.PlatformLayersComposeSceneImpl" -> {
                PlatformLayersLayoutHitTestOwner(scene)
            }

            else -> error("Unsupported compose scene")
        }
    }
}

/**
 * Reflect implementation for Compose 1.8
 */
private abstract class ReflectLayoutHitTestOwner : LayoutHitTestOwner {

    val classLoader = ComposeScene::class.java.classLoader!!

    fun RootNodeOwner.layoutNodeHitTest(
        x: Float,
        y: Float,
    ): Boolean {
        // result type is List<Modifier.Node> (compose 1.8)
        val result = HitTestResult()
        owner.root.hitTest(Offset(x, y), result, PointerType.Mouse, true)
        // pointer input modifier node detection for Material 3 components
        for (index in result.lastIndex downTo result.lastIndex - 1) {
            val node = result.getOrNull(index) ?: return false
            // SelectableNode, ClickableNode, CombinedClickableNode, ToggleableNode, TriStateToggleableNode
            if (node is AbstractClickableNode) {
                return true
            }
            val nodeClassName = node.javaClass.name
            return excludeNodeNames.any { nodeClassName.contains(it) }
        }
        return false
    }

    private val excludeNodeNames = listOf(
        "ScrollableNode",
        "HoverableNode",
    )
}

private class PlatformLayersLayoutHitTestOwner(
    scene: ComposeScene,
) : ReflectLayoutHitTestOwner() {

    private val sceneClass =
        classLoader.loadClass("androidx.compose.ui.scene.PlatformLayersComposeSceneImpl")

    private val mainOwnerRef =
        sceneClass.getDeclaredMethod("getMainOwner").let {
            it.trySetAccessible()
            it.invoke(scene) as RootNodeOwner
        }

    override fun hitTest(
        x: Float,
        y: Float,
    ): Boolean = mainOwnerRef.layoutNodeHitTest(x, y)
}

private class CanvasLayersLayoutHitTestOwner(
    private val scene: ComposeScene,
) : ReflectLayoutHitTestOwner() {

    private val sceneClass =
        classLoader.loadClass("androidx.compose.ui.scene.CanvasLayersComposeSceneImpl")
    private val layerClass =
        sceneClass.declaredClasses.first {
            it.name == $$"androidx.compose.ui.scene.CanvasLayersComposeSceneImpl$AttachedComposeSceneLayer"
        }

    private val mainOwnerRef =
        sceneClass.getDeclaredField("mainOwner").let {
            it.trySetAccessible()
            it.get(scene) as RootNodeOwner
        }

    private val layersCopyCacheRef =
        sceneClass.getDeclaredField("_layersCopyCache").let {
            it.trySetAccessible()
            it.get(scene) as CopiedList<*>
        }

    private val focusedLayerField =
        sceneClass.getDeclaredField("focusedLayer")
            .apply { trySetAccessible() }

    private val layerOwnerField =
        layerClass.getDeclaredField("owner")
            .apply { trySetAccessible() }

    /*
    private val layerIsInBoundMethod =
        layerClass
            .declaredMethods
            .first { it.name.startsWith("isInBounds") }
            .apply { trySetAccessible() }
     */

    override fun hitTest(
        x: Float,
        y: Float,
    ): Boolean {
        layersCopyCacheRef.withCopy {
            it.fastForEachReversed { layer ->
                // if (layerIsInBoundMethod.invoke(layer, packFloats(x, y)) == true) {
                if (
                    (layer as ComposeSceneLayer?)?.boundsInWindow
                        ?.contains(Offset(x, y).round()) == true
                ) {
                    return (layerOwnerField.get(layer) as RootNodeOwner).layoutNodeHitTest(x, y)
                } else if (layer == focusedLayerField.get(scene)) {
                    return false
                }
            }
        }
        return mainOwnerRef.layoutNodeHitTest(x, y)
    }
}
