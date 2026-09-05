package com.skyd.podaura.ui.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalMediaTest {
    @Test
    fun preservesOrderAndReportsFailuresWithoutDiscardingGoodFiles() = runTest {
        val batch = resolveExternalMediaBatch(listOf("2.mp3", "bad.mp3", "1.mp3"), { it }) {
            if (it == "bad.mp3") error("Permission denied")
            ExternalMedia(it, "resolved:$it")
        }
        assertEquals(listOf("2.mp3", "1.mp3"), batch.media.map { it.source })
        assertEquals(listOf(ExternalMediaFailure("bad.mp3", "Permission denied")), batch.failures)
        batch.release()
    }

    @Test
    fun allInvalidProducesNoReplacementMedia() = runTest {
        val batch = resolveExternalMediaBatch(listOf("missing"), { it }) { error("Missing file") }
        assertTrue(batch.media.isEmpty())
        assertEquals(1, batch.failures.size)
    }

    @Test
    fun cancellationClosesPreviouslyResolvedAccess() = runTest {
        var closed = 0
        assertFailsWith<CancellationException> {
            resolveExternalMediaBatch(listOf("first", "cancel"), { it }) {
                if (it == "cancel") throw CancellationException()
                ExternalMedia(it, "fd://10") { closed++ }
            }
        }
        assertEquals(1, closed)
    }

    @Test
    fun playerKeepsAccessAfterRequestIsReplacedAndClosesExactlyOnce() {
        var closed = 0
        val media = ExternalMedia("content://media/1", "fd://10") { closed++ }
        val batch = ExternalMediaBatch(listOf(media), emptyList())
        assertTrue(batch.retain())
        batch.release()
        assertEquals(0, closed)
        batch.release()
        assertEquals(1, closed)
        assertFalse(batch.retain())
        assertEquals("content://media/1", media.source)
    }

    @Test
    fun failedRetainRollsBackAccessAlreadyRetained() {
        var closed = 0
        val first = ExternalMedia("first", "fd://1") { closed++ }
        val expired = ExternalMedia("second", "fd://2")
        expired.release()
        assertFalse(ExternalMediaBatch(listOf(first, expired), emptyList()).retain())
        first.release()
        assertEquals(1, closed)
    }
}
