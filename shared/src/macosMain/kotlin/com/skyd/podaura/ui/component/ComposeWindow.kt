/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.MacosCursor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.MacosTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSCursor
import platform.AppKit.NSEvent
import platform.AppKit.NSTrackingActiveAlways
import platform.AppKit.NSTrackingActiveInKeyWindow
import platform.AppKit.NSTrackingArea
import platform.AppKit.NSTrackingAssumeInside
import platform.AppKit.NSTrackingInVisibleRect
import platform.AppKit.NSTrackingMouseEnteredAndExited
import platform.AppKit.NSTrackingMouseMoved
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.AppKit.NSWindowStyleMaskMiniaturizable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.NSWindowTitleHidden
import platform.Foundation.NSEdgeInsets
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.darwin.NSObject

interface WindowScope {
    /**
     * [NSWindow] that was created inside [Window]
     */
    val window: NSWindow
}

fun Window(
    title: String = "ComposeWindow",
    size: DpSize = DpSize(800.dp, 600.dp),
    content: @Composable WindowScope.() -> Unit,
) {
    ComposeWindow(
        title = title,
        size = size,
        content = content,
    )
}

private class ComposeWindow(
    title: String,
    size: DpSize,
    content: @Composable WindowScope.() -> Unit,
) : WindowScope {
    private var isDisposed = false
    private val macosTextInputService = MacosTextInputService()
    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    private val archComponentsOwner = DefaultArchitectureComponentsOwner()

    // TODO: It must be shared between Compose instances.
    //  It's supposed to be stored in platform's root view or window.
    private val frameRecomposer = FrameRecomposer(Dispatchers.Main) { skiaLayer.needRender() }

    // TODO: It cannot be used in case of shared [FrameRecomposer], replace this helper with calling
    //  - [frameRecomposer.performFrame] once per frame (across all instances) before platform views layout phase
    //  - [scene.measureAndLayout] during platform views layout phase. Note that it should be triggered
    //    by platform view invalidation (which is triggered by [scene.invalidateLayout] OR by regular platform invalidation)
    //  - [scene.draw] during drawing phase of platform views (which is triggered by [scene.invalidateDraw]).
    //    Note that in case of custom GPU surface/V-Sync handling, it needs to be handled differently.
    private val sceneRenderingScope = SingleComposeSceneRenderingScope { skiaLayer.needRender() }

    private val platformContext: PlatformContext =
        object : PlatformContext by PlatformContext.Empty() {
            override val windowInfo get() = _windowInfo
            override val architectureComponentsOwner get() = archComponentsOwner
            override val textInputService get() = macosTextInputService
            override fun setPointerIcon(pointerIcon: PointerIcon) {
                val cursor = (pointerIcon as? MacosCursor)?.cursor ?: NSCursor.arrowCursor
                cursor.set()
            }
        }
    private val skiaLayer = SkiaLayer()
    private val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        platformContext = platformContext,
        // TODO: Route these to distinct AppKit invalidation paths: layout work should use
        // native layout scheduling, while draw work should only mark display dirty.
        invalidateLayout = sceneRenderingScope::onSceneInvalidation,
        invalidateDraw = sceneRenderingScope::onSceneInvalidation,
    )
    private val renderDelegate = SkikoRenderDelegate { canvas, width, height, nanoTime ->
        val sizeInPx = IntSize(width, height)
        _windowInfo.containerSize = sizeInPx
        _windowInfo.containerDpSize = sizeInPx.toSize().toDpSize(scene.density)
        scene.size = sizeInPx // TODO: Move it out from onRender to avoid extra invalidation
        with(sceneRenderingScope) {
            scene.render(frameRecomposer, canvas.asComposeCanvas(), nanoTime)
        }
    }

    private val windowStyle =
        NSWindowStyleMaskTitled or
                NSWindowStyleMaskMiniaturizable or
                NSWindowStyleMaskClosable or
                NSWindowStyleMaskResizable or
                NSWindowStyleMaskFullSizeContentView

    private val windowDelegate = object : NSObject(), NSWindowDelegateProtocol {
        override fun windowWillClose(notification: NSNotification) = dispose()
    }

    override val window = object : NSWindow(
        contentRect = NSMakeRect(
            x = 0.0,
            y = 0.0,
            w = size.width.value.toDouble(),
            h = size.height.value.toDouble()
        ),
        styleMask = windowStyle,
        backing = NSBackingStoreBuffered,
        defer = true
    ) {
        override fun canBecomeKeyWindow() = true
        override fun canBecomeMainWindow() = true
    }

    private val view = object : NSView(window.frame) {
        private var trackingArea: NSTrackingArea? = null
        override fun wantsUpdateLayer() = true
        override fun acceptsFirstResponder() = true
        override fun viewWillMoveToWindow(newWindow: NSWindow?) {
            updateTrackingAreas()
        }

        override fun updateTrackingAreas() {
            trackingArea?.let { removeTrackingArea(it) }
            trackingArea = NSTrackingArea(
                rect = bounds,
                options = NSTrackingActiveAlways or
                        NSTrackingMouseEnteredAndExited or
                        NSTrackingMouseMoved or
                        NSTrackingActiveInKeyWindow or
                        NSTrackingAssumeInside or
                        NSTrackingInVisibleRect,
                owner = this, userInfo = null
            )
            addTrackingArea(trackingArea!!)
        }

        override fun mouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton.Primary)
        }

        override fun mouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton.Primary)
        }

        override fun rightMouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton.Secondary)
        }

        override fun rightMouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton.Secondary)
        }

        override fun otherMouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton(event.buttonNumber.toInt()))
        }

        override fun otherMouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton(event.buttonNumber.toInt()))
        }

        override fun mouseMoved(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }

        override fun mouseDragged(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }

        override fun scrollWheel(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Scroll)
        }

        override fun keyDown(event: NSEvent) {
            val consumed = onKeyboardEvent(event.toComposeEvent())
            if (!consumed) {
                // Pass only unconsumed event to system handler.
                // It will trigger the system's "beep" sound for unconsumed events.
                super.keyDown(event)
            }
        }

        override fun keyUp(event: NSEvent) {
            onKeyboardEvent(event.toComposeEvent())
        }
    }

    private val density: Density
        get() = Density(window.backingScaleFactor.toFloat())

    private val windowInsets = object : PlatformWindowInsets {
        override val systemBars: PlatformInsets
            get() = view.safeAreaInsets.toPlatformInsets(density)
    }

    init {
        window.delegate = windowDelegate
        window.titlebarAppearsTransparent = true
        window.titleVisibility = NSWindowTitleHidden

        window.title = title
        window.contentView = view

        skiaLayer.renderDelegate = renderDelegate
        skiaLayer.attachTo(view) // Should be called after attaching to window

        // TODO: Expose some API to control showing outside
        window.center()
        window.makeKeyAndOrderFront(null)

        scene.density = density
        scene.setContent {
            CompositionLocalProvider(
                LocalPlatformWindowInsets provides windowInsets,
                content = { content() }
            )
        }

        archComponentsOwner.enableSavedStateHandles()
        // TODO: Properly handle lifecycle events
        archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun dispose() {
        check(!isDisposed) { "ComposeWindow is already disposed" }
        archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        archComponentsOwner.viewModelStore.clear()
        skiaLayer.detach()
        scene.close()
        frameRecomposer.close()
        isDisposed = true
    }

    private fun onKeyboardEvent(event: KeyEvent): Boolean {
        if (isDisposed) return false
        return scene.sendKeyEvent(event)
    }

    private fun onMouseEvent(
        event: NSEvent,
        eventType: PointerEventType,
        button: PointerButton? = null,
    ) {
        if (isDisposed) return
        scene.sendPointerEvent(
            eventType = eventType,
            position = event.offset.toOffset(scene.density),
            scrollDelta = Offset(x = event.deltaX.toFloat(), y = event.deltaY.toFloat()),
            nativeEvent = event,
            button = button,
        )
    }

    private val NSEvent.offset: DpOffset
        get() {
            val position = locationInWindow.useContents {
                DpOffset(x = x.dp, y = y.dp)
            }
            val height = view.frame.useContents { size.height.dp }
            return DpOffset(
                x = position.x,
                y = height - position.y,
            )
        }

    // Copied from https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/iosMain/kotlin/androidx/compose/ui/unit/Conversions.ios.kt
    private fun CValue<NSEdgeInsets>.toPlatformInsets(density: Density) = useContents {
        density.PlatformInsets(
            left = left.dp,
            top = top.dp,
            right = right.dp,
            bottom = bottom.dp
        )
    }
}
