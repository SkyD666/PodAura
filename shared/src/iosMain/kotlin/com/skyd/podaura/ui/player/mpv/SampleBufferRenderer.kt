package com.skyd.podaura.ui.player.mpv

import cnames.structs.__CFDictionary
import cnames.structs.mpv_handle
import cnames.structs.mpv_render_context
import co.touchlab.kermit.Logger
import com.skyd.podaura.libmpv.*
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import platform.AVFoundation.*
import platform.CoreFoundation.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.Foundation.NSRecursiveLock
import platform.posix.RTLD_DEFAULT
import platform.posix.dlsym
import kotlin.math.max
import kotlin.math.min

/**
 * libmpv composites video/subtitles on the GPU into IOSurface-backed Core Video buffers.
 * AVFoundation presents the same buffers, without CPU pixel conversion or GPU readback.
 * EAGL is used because the pinned libmpv render API exposes OpenGL, not Metal.
 */
internal class SampleBufferRenderer(
    handle: CPointer<mpv_handle>,
    onFailure: (Throwable) -> Unit,
) : AutoCloseable {
    // This lock is deliberately independent of MPV's client lock: the render thread must never
    // wait for a client API call. Only render_* functions are used by the render coroutine.
    private val lock = NSRecursiveLock()
    private var gl: COpaquePointer? = checkNotNull(podaura_gl_create())
    private val updates = Channel<Unit>(Channel.CONFLATED)
    private val callback = StableRef.create(updates)
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            onFailure(
                error
            )
        }
    )
    private var textureCache: CVOpenGLESTextureCacheRef? = null
    private var framebuffer = 0u
    private var format: CMVideoFormatDescriptionRef? = null
    private var allocationLimits: CFMutableDictionaryRef? = null
    private var context: CPointer<mpv_render_context>? = null
    private var target: PlatformSurfaceHolder? = null
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var pool: CVPixelBufferPoolRef? = null
    private var poolSize = 0 to 0
    private var closed = false
    private var redraw = false

    init {
        try {
            locked {
                memScoped {
                    textureCache = checkNotNull(podaura_gl_texture_cache(gl))
                    framebuffer = podaura_gl_create_fbo()
                    allocationLimits = newDictionary().also {
                        it.setNumber(kCVPixelBufferPoolAllocationThresholdKey, 3)
                    }
                    val init = alloc<mpv_opengl_init_params>()
                    init.get_proc_address =
                        staticCFunction { _: COpaquePointer?, name: CPointer<ByteVar>? ->
                            dlsym(RTLD_DEFAULT, name)
                        }
                    init.get_proc_address_ctx = null
                    val result = alloc<CPointerVar<mpv_render_context>>()
                    val params = allocArray<mpv_render_param>(3)
                    params[0].type = MPV_RENDER_PARAM_API_TYPE
                    params[0].data = "opengl".cstr.ptr
                    params[1].type = MPV_RENDER_PARAM_OPENGL_INIT_PARAMS
                    params[1].data = init.ptr
                    params[2].type = MPV_RENDER_PARAM_INVALID
                    params[2].data = null
                    check(mpv_render_context_create(result.ptr, handle, params) >= 0) {
                        "libmpv GPU sample-buffer renderer initialization failed"
                    }
                    context = checkNotNull(result.value)
                }
            }
            mpv_render_context_set_update_callback(context, staticCFunction { pointer ->
                pointer?.asStableRef<Channel<Unit>>()?.get()?.trySend(Unit)
                Unit
            }, callback.asCPointer())
            scope.launch {
                for (ignored in updates) locked {
                    val ctx = context ?: return@locked
                    if (target == null) return@locked
                    val changed =
                        mpv_render_context_update(ctx) and MPV_RENDER_UPDATE_FRAME.toULong() != 0uL
                    if (changed || redraw) {
                        redraw = false
                        render(ctx)
                    }
                }
            }
        } catch (error: Throwable) {
            close(); throw error
        }
    }

    fun attach(holder: PlatformSurfaceHolder): Unit = locked {
        target?.layer?.flushAndRemoveImage()
        target = holder
        redraw = true
        updates.trySend(Unit)
    }

    private fun detach() = locked {
        target?.layer?.flushAndRemoveImage()
        target = null
        // The view detaches on willResignActive, before iOS prohibits background GPU work.
        podaura_gl_finish()
        releasePool()
    }

    fun videoSize(width: Int, height: Int): Unit = locked {
        sourceWidth = width
        sourceHeight = height
        redraw = true
        updates.trySend(Unit)
    }

    private fun render(ctx: CPointer<mpv_render_context>) = memScoped {
        val holder = target
        if (holder == null || holder.width <= 0 || holder.height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            skipFrame(ctx)
            return@memScoped
        }
        // Avoid allocating more output pixels than needed. mpv applies aspect, rotation,
        // zoom and subtitles; the display layer performs final scaling.
        val scale = min(
            1.0,
            max(sourceWidth.toDouble() / holder.width, sourceHeight.toDouble() / holder.height)
        )
        val width = max(2, (holder.width * scale).toInt())
        val height = max(2, (holder.height * scale).toInt())
        if (poolSize != width to height) {
            releasePool()
            val attributes = newDictionary()
            val surfaceAttributes = newDictionary()
            try {
                attributes.setNumber(kCVPixelBufferWidthKey, width)
                attributes.setNumber(kCVPixelBufferHeightKey, height)
                attributes.setNumber(
                    kCVPixelBufferPixelFormatTypeKey,
                    kCVPixelFormatType_32BGRA.toInt()
                )
                CFDictionarySetValue(
                    attributes,
                    kCVPixelBufferOpenGLESCompatibilityKey,
                    kCFBooleanTrue
                )
                CFDictionarySetValue(
                    attributes,
                    kCVPixelBufferIOSurfacePropertiesKey,
                    surfaceAttributes
                )
                val output = alloc<CVPixelBufferPoolRefVar>()
                check(
                    CVPixelBufferPoolCreate(
                        null,
                        null,
                        attributes,
                        output.ptr
                    ) == kCVReturnSuccess
                )
                pool = output.value
            } finally {
                CFRelease(attributes); CFRelease(surfaceAttributes)
            }
            poolSize = width to height
        }
        val pixel = alloc<CVPixelBufferRefVar>()
        // Bound retained IOSurfaces if the display pipeline stops consuming frames.
        val allocated = CVPixelBufferPoolCreatePixelBufferWithAuxAttributes(
            null, pool, allocationLimits, pixel.ptr
        )
        if (allocated != kCVReturnSuccess) {
            skipFrame(ctx); return@memScoped
        }
        val buffer = checkNotNull(pixel.value)
        val texture = alloc<CVOpenGLESTextureRefVar> { value = null }
        try {
            val mapped = podaura_gl_bind_buffer(
                textureCache,
                buffer,
                width,
                height,
                framebuffer,
                texture.ptr
            )
            check(mapped == 0) { "GPU buffer mapping failed: $mapped ($width x $height)" }
            val fbo = alloc<mpv_opengl_fbo>()
            fbo.fbo = framebuffer.toInt()
            fbo.w = width
            fbo.h = height
            fbo.internal_format = 0
            // Core Video's first row maps to texture row zero, unlike an on-screen GL framebuffer.
            val params = allocArray<mpv_render_param>(2)
            params[0].type = MPV_RENDER_PARAM_OPENGL_FBO; params[0].data = fbo.ptr
            params[1].type = MPV_RENDER_PARAM_INVALID; params[1].data = null
            val result = mpv_render_context_render(ctx, params)
            if (result < 0) {
                Logger.w("libmpv GPU render failed: $result"); return@memScoped
            }
            // Device builds hand the IOSurface to AVFoundation without readback.
            podaura_gl_present_buffer(buffer, width, height, framebuffer)
            if (format == null) {
                val description = alloc<CMVideoFormatDescriptionRefVar>()
                check(
                    CMVideoFormatDescriptionCreateForImageBuffer(
                        null,
                        buffer,
                        description.ptr
                    ) == 0
                )
                format = checkNotNull(description.value)
            }
            val sample = alloc<CMSampleBufferRefVar>()
            val timing = alloc<CMSampleTimingInfo>()
            kCMTimeInvalid.readValue().place(timing.duration.ptr)
            CMClockGetTime(CMClockGetHostTimeClock()).place(timing.presentationTimeStamp.ptr)
            kCMTimeInvalid.readValue().place(timing.decodeTimeStamp.ptr)
            check(
                CMSampleBufferCreateReadyWithImageBuffer(
                    null,
                    buffer,
                    format,
                    timing.ptr,
                    sample.ptr
                ) == 0
            )
            try {
                val attachments = CMSampleBufferGetSampleAttachmentsArray(sample.value, true)!!
                val dictionary =
                    CFArrayGetValueAtIndex(attachments, 0)!!.reinterpret<__CFDictionary>()
                CFDictionarySetValue(
                    dictionary,
                    kCMSampleAttachmentKey_DisplayImmediately,
                    kCFBooleanTrue
                )
                // mpv_render_context_render already waited for audio-master presentation time.
                // Do not introduce a second clock that can drift after seek or speed changes.
                if (holder.layer.status == AVQueuedSampleBufferRenderingStatusFailed) holder.layer.flush()
                holder.layer.enqueueSampleBuffer(sample.value)
            } finally {
                sample.value?.let(::CFRelease)
            }
        } finally {
            podaura_gl_unbind_buffer(framebuffer)
            texture.value?.let(::CFRelease)
            textureCache?.let { CVOpenGLESTextureCacheFlush(it, 0u) }
            CVPixelBufferRelease(buffer)
        }
    }

    private fun skipFrame(ctx: CPointer<mpv_render_context>) = memScoped {
        val skip = alloc<IntVar> { value = 1 }
        val params = allocArray<mpv_render_param>(2)
        params[0].type = MPV_RENDER_PARAM_SKIP_RENDERING
        params[0].data = skip.ptr
        params[1].type = MPV_RENDER_PARAM_INVALID
        params[1].data = null
        mpv_render_context_render(ctx, params)
    }

    private fun newDictionary(): CFMutableDictionaryRef = checkNotNull(
        CFDictionaryCreateMutable(
            null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
        )
    )

    private fun CFMutableDictionaryRef.setNumber(key: CFStringRef?, number: Int) = memScoped {
        val value = alloc<IntVar> { this.value = number }
        val cfNumber = checkNotNull(CFNumberCreate(null, kCFNumberIntType, value.ptr))
        try {
            CFDictionarySetValue(this@setNumber, key, cfNumber)
        } finally {
            CFRelease(cfNumber)
        }
    }

    private fun releasePool() {
        format?.let(::CFRelease)
        format = null
        textureCache?.let { CVOpenGLESTextureCacheFlush(it, 0u) }
        pool?.let(::CVPixelBufferPoolRelease)
        pool = null
        poolSize = 0 to 0
    }

    override fun close() = locked {
        if (closed) return@locked
        closed = true
        context?.let {
            mpv_render_context_set_update_callback(it, null, null)
            mpv_render_context_free(it)
        }
        context = null
        callback.dispose()
        updates.close()
        scope.cancel()
        detach()
        podaura_gl_delete_fbo(framebuffer)
        framebuffer = 0u
        textureCache?.let(::CFRelease)
        textureCache = null
        allocationLimits?.let(::CFRelease)
        allocationLimits = null
        podaura_gl_release(gl)
        gl = null
    }

    private fun <T> locked(block: () -> T): T {
        lock.lock()
        val previous = podaura_gl_current()
        return try {
            if (gl != null) check(podaura_gl_make_current(gl))
            block()
        } finally {
            podaura_gl_make_current(previous)
            lock.unlock()
        }
    }
}
