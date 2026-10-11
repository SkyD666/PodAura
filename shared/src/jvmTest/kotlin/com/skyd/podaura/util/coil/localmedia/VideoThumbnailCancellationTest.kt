package com.skyd.podaura.util.coil.localmedia

import com.sun.jna.Pointer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VideoThumbnailCancellationTest {
    @Test
    fun cancellingQueuedAndActiveRequestsSkipsDecodingAndReleasesTheHandle() = runBlocking<Unit> {
        val polling = CompletableDeferred<Unit>()
        val releasePoll = CountDownLatch(1)
        val created = AtomicInteger()
        val destroyed = AtomicInteger()
        val api = Proxy.newProxyInstance(
            ThumbnailMpv::class.java.classLoader,
            arrayOf(ThumbnailMpv::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "mpv_create" -> Pointer(created.incrementAndGet().toLong())
                "mpv_set_option_string", "mpv_initialize", "mpv_command" -> 0
                "mpv_wait_event" -> {
                    polling.complete(Unit)
                    check(releasePoll.await(5, TimeUnit.SECONDS))
                    null
                }
                "mpv_terminate_destroy" -> { destroyed.incrementAndGet(); null }
                else -> error("Unexpected native call: ${method.name}")
            }
        } as ThumbnailMpv
        val active = launch(Dispatchers.IO) {
            assertFailsWith<CancellationException> {
                getLocalVideoThumbnailData("active.mkv") { api }
            }
        }
        try {
            withTimeout(5_000) { polling.await() }
            val queued = launch(start = CoroutineStart.UNDISPATCHED) {
                getLocalVideoThumbnailData("queued.mkv") {
                    error("Cancelled queued requests must not initialize the decoder")
                }
            }
            // This must finish while the active request still holds the decoder lock.
            withTimeout(5_000) { queued.cancelAndJoin() }
            active.cancel()
        } finally {
            releasePoll.countDown()
            active.cancelAndJoin()
        }
        assertTrue(active.isCancelled)
        assertEquals(1, created.get(), "Cancellation must not start the frame-zero retry")
        assertEquals(1, destroyed.get())
        // Cancellation must also release the mutex for subsequent requests.
        withTimeout(5_000) {
            assertFailsWith<IllegalStateException> {
                getLocalVideoThumbnailData("next.mkv") { error("Lock acquired") }
            }
        }
    }

    @Test
    fun cancelledExtractionPropagatesCancellationInsteadOfMissingArtwork() = runBlocking {
        launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            assertFailsWith<CancellationException> {
                getLocalMediaThumbnailData("cancelled.mkv")
            }
        }.join()
    }
}
