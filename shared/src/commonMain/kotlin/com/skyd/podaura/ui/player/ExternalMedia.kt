package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.concurrent.atomics.AtomicInt

/** Platform adapters retain access until both the request and the player release it. */
class ExternalMedia(
    val source: String,
    val playbackUrl: String,
    private val releaseAccess: () -> Unit = {},
) {
    private val references = AtomicInt(1)

    fun retain(): Boolean {
        while (true) {
            val count = references.load()
            if (count == 0) return false
            if (references.compareAndSet(count, count + 1)) return true
        }
    }

    fun release() {
        while (true) {
            val count = references.load()
            check(count > 0) { "External media access released twice" }
            if (references.compareAndSet(count, count - 1)) {
                if (count == 1) runCatching(releaseAccess)
                return
            }
        }
    }
}

data class ExternalMediaFailure(val source: String, val reason: String)

internal fun externalPlaybackError(code: Int): String = when (code) {
    -13 -> "Unable to load media"
    -14 -> "Unable to initialize audio output"
    -15 -> "Unable to initialize video output"
    -16 -> "No playable audio or video streams"
    -17 -> "Unrecognized or damaged media format"
    -18 -> "Playback is not supported by this system"
    else -> "Playback error ($code)"
}

data class ExternalMediaBatch(
    val media: List<ExternalMedia>,
    val failures: List<ExternalMediaFailure>,
) {
    fun retain(): Boolean {
        val retained = mutableListOf<ExternalMedia>()
        for (item in media) {
            if (!item.retain()) {
                retained.forEach { it.release() }
                return false
            }
            retained += item
        }
        return true
    }

    fun release() = media.forEach { it.release() }
}

/** OS events supply ordered files; resolution and access ownership remain platform-specific. */
expect fun resolveExternalMedia(file: PlatformFile): ExternalMedia

internal suspend fun <T> resolveExternalMediaBatch(
    files: List<T>,
    source: (T) -> String,
    resolve: (T) -> ExternalMedia,
): ExternalMediaBatch {
    val media = mutableListOf<ExternalMedia>()
    val failures = mutableListOf<ExternalMediaFailure>()
    try {
        for (file in files) {
            currentCoroutineContext().ensureActive()
            try {
                media += resolve(file)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures += ExternalMediaFailure(source(file), error.message ?: error.toString())
            }
        }
        currentCoroutineContext().ensureActive()
        return ExternalMediaBatch(media, failures)
    } catch (error: Throwable) {
        media.forEach { it.release() }
        throw error
    }
}
