package com.skyd.podaura.ui.player

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults

private const val BOOKMARK_PREFIX = "podaura.mediaBookmark."

actual fun resolveExternalMedia(file: PlatformFile): ExternalMedia = memScoped {
    val source = file.path
    val preferences = NSUserDefaults.standardUserDefaults
    val saved = preferences.dataForKey(BOOKMARK_PREFIX + source)
    val error = alloc<ObjCObjectVar<NSError?>>()
    val stale = alloc<BooleanVar>()
    // A freshly selected URL can renew a revoked bookmark. Prefer its granted access.
    var url = file.nsUrl
    var accessed = url.startAccessingSecurityScopedResource()
    if (!accessed && saved != null) {
        url = NSURL.URLByResolvingBookmarkData(saved, 0u, null, stale.ptr, error.ptr)
            ?: error("File access expired. Select the file again: ${error.value?.localizedDescription.orEmpty()}")
        accessed = url.startAccessingSecurityScopedResource()
    }
    try {
        val localPath = url.path.orEmpty()
        val inContainer = localPath.startsWith(NSHomeDirectory().trimEnd('/') + "/") ||
                localPath.startsWith(NSTemporaryDirectory().trimEnd('/') + "/")
        var playable: String? =
            if (inContainer && NSFileManager.defaultManager.isReadableFileAtPath(localPath)) localPath else null
        if (!inContainer) {
            NSFileCoordinator(null).coordinateReadingItemAtURL(url, 0u, error.ptr) { coordinated ->
                playable =
                    coordinated?.path?.takeIf { NSFileManager.defaultManager.isReadableFileAtPath(it) }
            }
        }
        val path = playable
            ?: error("Cannot read this file. Select it again: ${error.value?.localizedDescription.orEmpty()}")
        val bookmark = url.bookmarkDataWithOptions(0u, null, null, error.ptr)
            ?: error("Cannot retain file access: ${error.value?.localizedDescription.orEmpty()}")
        preferences.setObject(bookmark, BOOKMARK_PREFIX + source)
        ExternalMedia(source, path) { if (accessed) url.stopAccessingSecurityScopedResource() }
    } catch (failure: Throwable) {
        if (accessed) url.stopAccessingSecurityScopedResource()
        throw failure
    }
}

/** History and saved playlists retain source identity while restoring the provider's current URL. */
internal actual suspend fun preparePlatformPlayback(data: PlayerLaunchData): PlayerLaunchData {
    val external = data.playlist.filter {
        NSUserDefaults.standardUserDefaults.dataForKey(BOOKMARK_PREFIX + it.playlistMediaBean.stableUrl) != null
    }
    if (external.isEmpty()) return data
    val batch = resolveExternalMediaBatch(
        external,
        { it.playlistMediaBean.stableUrl },
        { resolveExternalMedia(PlatformFile(it.playlistMediaBean.stableUrl)) })
    try {
        val resolved = batch.media.associateBy { it.source }
        val failed = batch.failures.mapTo(mutableSetOf()) { it.source }
        val playlist = data.playlist.mapNotNull { item ->
            val source = item.playlistMediaBean.stableUrl
            if (source in failed) null else resolved[source]?.let { media ->
                val original = item.playlistMediaBean
                item.copy(playlistMediaBean = original.copy(url = media.playbackUrl).apply {
                    sourceUrl = source
                    historyUrl = original.historyUrl
                    title = original.title
                    artist = original.artist
                    duration = original.duration
                    thumbnail = original.thumbnail
                })
            } ?: item
        }
        val start = data.playlist.firstOrNull { it.playlistMediaBean.url == data.startPath }
            ?.playlistMediaBean?.stableUrl
        return data.copy(
            playlist = playlist,
            startPath = resolved[start]?.playbackUrl ?: data.startPath,
            externalBatch = batch
        )
    } catch (error: Throwable) {
        batch.release(); throw error
    }
}
