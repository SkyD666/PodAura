@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.openani.mediamp.mpv.internal.MpvSurfaceDrawResolver
import org.openani.mediamp.mpv.utils.SkiaRenderDeviceInterop
import org.openani.mediamp.mpv.utils.findSkiaLayer
import kotlin.time.Duration.Companion.milliseconds

/**
 * The mediamp surface implementation adapted to the raw [MPV] handle. Keeping the raw handle is
 * important: mediamp's high-level player initializes mpv in its constructor, before PodAura can
 * apply config-dir and the user's initialization options.
 */
@Composable
internal fun MPVSurface(
    player: MPV,
    modifier: Modifier,
) {
    val logger = remember { Logger.withTag("MPVSurface") }
    val window = LocalAwtWindow.current
    val interop: SkiaRenderDeviceInterop? = remember(window, player) {
        if (window == null) {
            logger.e { "LocalWindow.current is null; cannot locate SkiaLayer" }
            return@remember null
        }
        val layer = window.findSkiaLayer()
        if (layer == null) {
            logger.e { "No SkiaLayer found in player window" }
            return@remember null
        }
        try {
            val value = player.createSkiaInterop(layer)
            value as? SkiaRenderDeviceInterop
                ?: error("Unsupported Skia interop: ${value?.javaClass}")
        } catch (throwable: Throwable) {
            logger.e(throwable) { "Skia device interop initialization failed" }
            null
        }
    }
    val drawResolver: MpvSurfaceDrawResolver? = remember(player, interop) {
        interop?.let { player.renderContextLifecycle?.createDrawResolver(it) }
    }
    val frameTick = remember(player) { mutableLongStateOf(0L) }
    val canvasSize = remember(player) { mutableStateOf(IntSize.Zero) }
    var renderContextReady by remember(player) { mutableStateOf(false) }
    val loggedStates = remember(player) { mutableSetOf<String>() }
    fun logOnce(state: String) {
        if (loggedStates.add(state)) logger.d { state }
    }

    DisposableEffect(player) {
        renderContextReady = player.renderContextLifecycle?.createEagerly() ?: false
        if (!renderContextReady &&
            player.renderContextLifecycle?.deferredReadiness != true
        ) {
            logger.e { "Failed to create eager mpv render context" }
        }
        if (renderContextReady) {
            player.setRenderUpdateListener { frameTick.longValue++ }
        }
        onDispose {
            player.setRenderUpdateListener(null)
            player.releaseSurface()
            renderContextReady = false
        }
    }

    LaunchedEffect(player, interop) {
        val deviceInterop = interop ?: return@LaunchedEffect
        val deferredReadiness = player.renderContextLifecycle?.deferredReadiness == true
        var configured = false
        snapshotFlow { canvasSize.value }
            .filter { it.width > 0 && it.height > 0 }
            .collectLatest { size ->
                if (deferredReadiness) {
                    snapshotFlow { renderContextReady }.first { it }
                }
                if (configured) delay(150.milliseconds)
                while (true) {
                    val devicePtr = runCatching { deviceInterop.renderDevicePtr }.getOrNull()
                    if (devicePtr != null &&
                        player.requestSurface(size.width, size.height, devicePtr)
                    ) {
                        configured = true
                        break
                    }
                    if (deferredReadiness) break
                    delay(50.milliseconds)
                }
            }
    }

    Canvas(modifier.onSizeChanged { canvasSize.value = it }) {
        frameTick.longValue
        val resolver = drawResolver
        if (resolver == null) {
            logOnce("No surface draw resolver; video stays black")
            return@Canvas
        }
        val drawPass = resolver.resolveDrawPass(renderContextReady)
        if (drawPass != null && !renderContextReady) {
            // On Linux this is the first draw for which the GLX environment is attached.
            // The lifecycle invokes MPV's readiness callback while resolving this pass, which
            // flushes all queued loadfile/playlist commands.
            player.setRenderUpdateListener { frameTick.longValue++ }
            renderContextReady = true
        }
        if (drawPass == null) {
            logOnce("Render context is not ready")
            return@Canvas
        }
        val directContext = drawPass.directContext
        if (directContext == null) {
            logOnce("Skia DirectContext is not initialized")
            return@Canvas
        }
        val width = size.width.toInt()
        val height = size.height.toInt()
        if (width <= 0 || height <= 0) return@Canvas
        val renderDevicePtr: Long? = drawPass.renderDevicePtr.invoke()
        if (renderDevicePtr != null) {
            player.refreshDeviceIfChanged(renderDevicePtr)
        }

        val frame = player.currentFrameImage(directContext)
        if (frame == null) {
            logOnce("Surface ring has no rendered frame")
        } else {
            logOnce("Rendering video through ${resolver.rendererName}")
            val scale = minOf(
                size.width / frame.width.toFloat(),
                size.height / frame.height.toFloat(),
            )
            val dstWidth = frame.width * scale
            val dstHeight = frame.height * scale
            val dx = (size.width - dstWidth) / 2f
            val dy = (size.height - dstHeight) / 2f
            drawIntoCanvas { canvas ->
                canvas.skiaCanvas.drawImageRect(
                    image = frame,
                    src = Rect.makeWH(frame.width.toFloat(), frame.height.toFloat()),
                    dst = Rect.makeXYWH(dx, dy, dstWidth, dstHeight),
                    samplingMode = SamplingMode.LINEAR,
                    paint = null,
                    strict = true,
                )
            }
        }
    }
}
