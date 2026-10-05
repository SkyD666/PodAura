package com.skyd.podaura.ui.component

import platform.Foundation.NSThread

/**
 * Commits NSView changes between
 * Compose layout and drawing. This window owns the renderer, so no main-queue fallback is needed.
 */
internal class MacosViewUpdates(private val requestFrame: () -> Unit) {
    private val pending = ArrayDeque<() -> Unit>()
    private var flushing = false
    private var disposed = false

    fun schedule(action: () -> Unit) {
        check(NSThread.isMainThread)
        if (disposed) return
        pending.addLast(action)
        requestFrame()
    }

    fun flush() {
        check(NSThread.isMainThread)
        if (disposed || flushing) return
        flushing = true
        try {
            var count = 0
            while (pending.isNotEmpty()) {
                check(++count <= 1024) { "AppKit view updates did not converge" }
                pending.removeFirst().invoke()
            }
        } finally {
            flushing = false
        }
    }

    fun dispose() {
        if (disposed) return
        flush()
        disposed = true
        pending.clear()
    }
}
