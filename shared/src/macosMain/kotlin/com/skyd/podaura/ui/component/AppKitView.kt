/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.viewinterop.InteropContainer
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewHolder
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TrackInteropPlacementModifierNode
import androidx.compose.ui.viewinterop.TypedInteropViewHolder
import androidx.compose.ui.viewinterop.countInteropComponentsBelow
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.AppKit.NSView
import platform.AppKit.NSWindowBelow
import platform.AppKit.fittingSize
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSMakeRect
import platform.QuartzCore.CATransaction
import kotlin.math.roundToInt

/** Non-interactive native content beneath Compose; Compose owns input and accessibility. */
@Composable
internal fun <T : NSView> AppKitView(
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = {},
    onRelease: (T) -> Unit = {},
) {
    val container = LocalInteropContainer.current as AppKitInteropContainer
    InteropView(
        factory = { key -> AppKitViewHolder(factory, container, key) },
        modifier = modifier,
        update = update,
        onRelease = onRelease,
    )
}

internal class AppKitInteropContainer(
    override val root: NSView,
    private val updates: MacosViewUpdates,
) : InteropContainer {
    override var rootModifier: TrackInteropPlacementModifierNode? = null
    override val snapshotObserver = SnapshotStateObserver { scheduleUpdate(it) }
    private val holders = mutableMapOf<Any, InteropViewHolder>()

    init {
        snapshotObserver.start()
    }

    override fun contains(holder: InteropViewHolder) = holders[holder.interopView] === holder
    override fun holderOfView(view: Any) = holders[view]
    override fun scheduleUpdate(action: () -> Unit) = updates.schedule(action)

    override fun place(holder: InteropViewHolder) {
        val inserted = holders.put(holder.interopView, holder) == null
        val index = countInteropComponentsBelow(holder)
        scheduleUpdate {
            if (inserted) holder.insertInteropView(root, index)
            else holder.changeInteropViewIndex(root, index)
        }
    }

    override fun unplace(holder: InteropViewHolder) {
        if (holders.remove(holder.interopView) != null) {
            scheduleUpdate { holder.removeInteropView(root) }
        }
    }

    fun dispose() {
        updates.flush()
        holders.values.forEach { it.removeInteropView(root) }
        holders.clear()
        snapshotObserver.stop()
        snapshotObserver.clear()
    }
}

internal class AppKitInteropRootView : NSView(NSMakeRect(0.0, 0.0, 0.0, 0.0)) {
    override fun isFlipped() = true
    override fun hitTest(point: CValue<CGPoint>): NSView? = null
}

private class AppKitViewHolder<T : NSView>(
    factory: () -> T,
    container: AppKitInteropContainer,
    key: CompositeKeyHashCode,
    private val wrapper: NSView = AppKitInteropRootView(),
) : TypedInteropViewHolder<T>(factory, container, wrapper, key) {
    init {
        wrapper.wantsLayer = true
        wrapper.layer?.masksToBounds = true
        wrapper.setAccessibilityElement(false)
        wrapper.setAccessibilityChildren(emptyList<Any>())
        wrapper.addSubview(interopView)
        platformModifier = Modifier.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        }
    }

    override val measurePolicy = MeasurePolicy { _, constraints ->
        val fitting = interopView.fittingSize()
        layout(
            constraints.constrainWidth(fitting.useContents { (width * density).roundToInt() }),
            constraints.constrainHeight(fitting.useContents { (height * density).roundToInt() }),
        ) {}
    }

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val root = layoutCoordinates.findRootCoordinates()
        val full = root.localBoundingBoxOf(layoutCoordinates, clipBounds = false).roundToIntRect()
        val clip = root.localBoundingBoxOf(layoutCoordinates, clipBounds = true).roundToIntRect()
        val scale = density.density.toDouble()
        container.scheduleUpdate {
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            try {
                wrapper.hidden = clip.isEmpty
                wrapper.setFrame(
                    NSMakeRect(
                        clip.left / scale, clip.top / scale,
                        clip.width / scale, clip.height / scale
                    )
                )
                // Keep the full view size when scrolling clips part of the interop node.
                interopView.setFrame(
                    NSMakeRect(
                        (full.left - clip.left) / scale,
                        (full.top - clip.top) / scale, full.width / scale, full.height / scale
                    )
                )
                interopView.layer?.contentsScale = scale
            } finally {
                CATransaction.commit()
            }
        }
    }

    override fun insertInteropView(root: Any, index: Int) {
        changeInteropViewIndex(root, index)
        super.insertInteropView(root, index)
    }

    override fun changeInteropViewIndex(root: Any, index: Int) {
        val target = root as NSView
        val above =
            target.subviews.filterIsInstance<NSView>().filter { it != wrapper }.getOrNull(index)
        if (above == null) target.addSubview(wrapper)
        else target.addSubview(wrapper, positioned = NSWindowBelow, relativeTo = above)
    }

    override fun removeInteropView(root: Any) {
        wrapper.removeFromSuperview()
        super.removeInteropView(root)
    }

    override fun dispatchToView(pointerEvent: PointerEvent) = Unit
}
