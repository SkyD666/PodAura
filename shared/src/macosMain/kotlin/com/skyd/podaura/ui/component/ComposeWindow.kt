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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.MacosCursor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.MacosTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.hasInvalidations
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
import platform.AppKit.NSApplication
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSCursor
import platform.AppKit.NSEvent
import platform.AppKit.NSEventModifierFlagCapsLock
import platform.AppKit.NSEventModifierFlagCommand
import platform.AppKit.NSEventModifierFlagControl
import platform.AppKit.NSEventModifierFlagFunction
import platform.AppKit.NSEventModifierFlagOption
import platform.AppKit.NSEventModifierFlagShift
import platform.AppKit.NSEventPhaseBegan
import platform.AppKit.NSEventPhaseCancelled
import platform.AppKit.NSEventPhaseEnded
import platform.AppKit.NSEventPhaseNone
import platform.AppKit.NSTrackingActiveAlways
import platform.AppKit.NSTrackingActiveInKeyWindow
import platform.AppKit.NSTrackingArea
import platform.AppKit.NSTrackingAssumeInside
import platform.AppKit.NSTrackingInVisibleRect
import platform.AppKit.NSTrackingMouseEnteredAndExited
import platform.AppKit.NSTrackingMouseMoved
import platform.AppKit.NSView
import platform.AppKit.NSViewHeightSizable
import platform.AppKit.NSViewWidthSizable
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

internal val LocalMacosVideoContainer = staticCompositionLocalOf<NSView> {
    error("No AppKit window host")
}

interface WindowScope {
    /**
     * [NSWindow] that was created inside [Window]
     */
    val window: NSWindow
}

fun Window(
    title: String = "ComposeWindow",
    size: DpSize = DpSize(800.dp, 600.dp),
    onClose: () -> Unit = {},
    transparent: Boolean = false,
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable WindowScope.() -> Unit,
): ComposeWindow = ComposeWindow(
    title = title,
    size = size,
    content = content,
    onClose = onClose,
    transparent = transparent,
    onKeyEvent = onKeyEvent,
)

class ComposeWindow(
    title: String,
    size: DpSize,
    private val onClose: () -> Unit = {},
    private val transparent: Boolean = false,
    private val onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable WindowScope.() -> Unit,
) : WindowScope {
    private var isDisposed = false
    private var isDispatchingScene = false

    private inline fun <T> dispatchScene(block: () -> T): T? {
        if (isDisposed || isDispatchingScene) return null
        isDispatchingScene = true
        return try {
            block()
        } finally {
            isDispatchingScene = false
        }
    }

    private val macosTextInputService = MacosTextInputService()
    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    private val archComponentsOwner = DefaultArchitectureComponentsOwner()

    private val frameRecomposer = FrameRecomposer(Dispatchers.Main, ::scheduleFrame)
    private val nativeViewUpdates = MacosViewUpdates(::scheduleFrame)

    private fun scheduleFrame() {
        if (!isDisposed) skiaLayer.needRender()
    }

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
        invalidateLayout = ::scheduleFrame,
        invalidateDraw = ::scheduleFrame,
    )
    private val renderDelegate = SkikoRenderDelegate { canvas, width, height, nanoTime ->
        // FileKit's runModal pumps AppKit while Compose is still dispatching its click.
        // Flushing that scene again resumes the same coroutine twice.
        if (isDispatchingScene || NSApplication.sharedApplication().modalWindow != null) return@SkikoRenderDelegate
        dispatchScene {
            if (transparent) canvas.clear(org.jetbrains.skia.Color.TRANSPARENT)
            scene.density = density
            val sizeInPx = IntSize(width, height)
            _windowInfo.containerSize = sizeInPx
            _windowInfo.containerDpSize = sizeInPx.toSize().toDpSize(scene.density)
            scene.size = sizeInPx // TODO: Move it out from onRender to avoid extra invalidation
            // AppKit interop must commit after placement and before the matching clear regions draw.
            frameRecomposer.performFrame(nanoTime)
            if (isDisposed) return@dispatchScene
            scene.measureAndLayout()
            nativeViewUpdates.flush()
            scene.draw(canvas.asComposeCanvas())
        }
        if (!isDisposed && (frameRecomposer.hasPendingWork() || scene.hasInvalidations())) {
            scheduleFrame()
        }
    }

    private val windowStyle =
        NSWindowStyleMaskTitled or
                NSWindowStyleMaskMiniaturizable or
                NSWindowStyleMaskClosable or
                NSWindowStyleMaskResizable or
                NSWindowStyleMaskFullSizeContentView

    private val windowDelegate = object : NSObject(), NSWindowDelegateProtocol {
        override fun windowWillClose(notification: NSNotification) {
            dispose()
            onClose()
        }

        override fun windowDidBecomeKey(notification: NSNotification) {
            _windowInfo.isWindowFocused = true
        }

        override fun windowDidResignKey(notification: NSNotification) {
            _windowInfo.isWindowFocused = false
        }

        override fun windowDidResize(notification: NSNotification) {
            scene.invalidatePositionInWindow()
            scheduleFrame()
        }

        override fun windowDidChangeBackingProperties(notification: NSNotification) {
            scene.density = density
            scene.invalidatePositionInWindow()
            scheduleFrame()
        }
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

    private val container =
        NSView(NSMakeRect(0.0, 0.0, size.width.value.toDouble(), size.height.value.toDouble()))
    private val view = object : NSView(container.bounds) {
        private var trackingArea: NSTrackingArea? = null
        override fun wantsUpdateLayer() = true
        override fun acceptsFirstResponder() = true
        override fun viewWillMoveToWindow(newWindow: NSWindow?) {
            super.viewWillMoveToWindow(newWindow)
            updateTrackingAreas()
        }

        override fun updateTrackingAreas() {
            super.updateTrackingAreas()
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

        override fun rightMouseDragged(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }

        override fun otherMouseDragged(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }

        override fun mouseEntered(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Enter)
        }

        override fun mouseExited(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Exit)
        }

        override fun scrollWheel(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Scroll)
        }

        override fun magnifyWithEvent(event: NSEvent) {
            onMagnificationEvent(event)
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
        window.releasedWhenClosed = false
        window.contentView = container
        container.wantsLayer = true
        view.autoresizingMask = NSViewWidthSizable or NSViewHeightSizable
        container.addSubview(view)

        skiaLayer.renderDelegate = renderDelegate
        skiaLayer.attachTo(view) // Should be called after attaching to window

        // TODO: Expose some API to control showing outside
        window.center()
        window.makeKeyAndOrderFront(null)
        window.makeFirstResponder(view)

        scene.density = density
        scene.setContent {
            CompositionLocalProvider(
                LocalPlatformWindowInsets provides windowInsets,
                LocalMacosVideoContainer provides container,
                LocalMacosViewUpdates provides nativeViewUpdates,
                content = { content() }
            )
        }

        archComponentsOwner.enableSavedStateHandles()
        // TODO: Properly handle lifecycle events
        archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        archComponentsOwner.viewModelStore.clear()
        scene.close()
        nativeViewUpdates.dispose()
        skiaLayer.detach()
        frameRecomposer.close()
        window.delegate = null
    }

    fun close() {
        window.performClose(null)
    }

    fun activate() {
        window.deminiaturize(null)
        window.makeKeyAndOrderFront(null)
        window.makeFirstResponder(view)
    }

    private fun onKeyboardEvent(event: KeyEvent): Boolean {
        if (isDisposed) return false
        return dispatchScene { scene.sendKeyEvent(event) || onKeyEvent(event) } ?: false
    }

    private fun onMouseEvent(
        event: NSEvent,
        eventType: PointerEventType,
        button: PointerButton? = null,
    ) {
        if (isDisposed) return
        dispatchScene {
            scene.sendPointerEvent(
                eventType = eventType,
                position = event.offset.toOffset(scene.density),
                scrollDelta = Offset(x = event.deltaX.toFloat(), y = event.deltaY.toFloat()),
                keyboardModifiers = event.pointerKeyboardModifiers,
                nativeEvent = event,
                button = button,
            )
        }
    }

    private var lastMagnificationPosition: Offset? = null

    private fun onMagnificationEvent(event: NSEvent) {
        if (isDisposed) return
        val position = event.offset.toOffset(scene.density)
        val began = event.phase and NSEventPhaseBegan != 0uL
        val ended = event.phase and (NSEventPhaseEnded or NSEventPhaseCancelled) != 0uL
        val unphased = event.phase == NSEventPhaseNone

        if (began || lastMagnificationPosition == null) {
            lastMagnificationPosition = position
            sendMagnificationEvent(
                event = event,
                eventType = PointerEventType.ScaleStart,
                position = position,
            )
        }

        val previousPosition = lastMagnificationPosition ?: position
        val panGestureOffset = previousPosition - position
        val scaleFactor = (1f + event.magnification.toFloat()).coerceAtLeast(0.01f)
        if (scaleFactor != 1f || panGestureOffset != Offset.Zero) {
            sendMagnificationEvent(
                event = event,
                eventType = PointerEventType.ScaleChange,
                position = position,
                scaleGestureFactor = scaleFactor,
                panGestureOffset = panGestureOffset,
            )
        }
        lastMagnificationPosition = position

        if (ended || unphased) {
            sendMagnificationEvent(
                event = event,
                eventType = PointerEventType.ScaleEnd,
                position = position,
            )
            lastMagnificationPosition = null
        }
    }

    private fun sendMagnificationEvent(
        event: NSEvent,
        eventType: PointerEventType,
        position: Offset,
        scaleGestureFactor: Float = 1f,
        panGestureOffset: Offset = Offset.Zero,
    ) {
        dispatchScene {
            scene.sendPointerEvent(
                eventType = eventType,
                position = position,
                keyboardModifiers = event.pointerKeyboardModifiers,
                nativeEvent = event,
                scaleGestureFactor = scaleGestureFactor,
                panGestureOffset = panGestureOffset,
            )
        }
    }

    private val NSEvent.pointerKeyboardModifiers: PointerKeyboardModifiers
        get() = PointerKeyboardModifiers(
            isCtrlPressed = modifierFlags and NSEventModifierFlagControl != 0uL,
            isMetaPressed = modifierFlags and NSEventModifierFlagCommand != 0uL,
            isAltPressed = modifierFlags and NSEventModifierFlagOption != 0uL,
            isShiftPressed = modifierFlags and NSEventModifierFlagShift != 0uL,
            isFunctionPressed = modifierFlags and NSEventModifierFlagFunction != 0uL,
            isCapsLockOn = modifierFlags and NSEventModifierFlagCapsLock != 0uL,
        )

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
