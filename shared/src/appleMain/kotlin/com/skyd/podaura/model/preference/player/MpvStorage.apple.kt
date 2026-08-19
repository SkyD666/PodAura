package com.skyd.podaura.model.preference.player

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories

internal actual val platformMpvCacheSelectionMode: MpvCacheSelectionMode =
    MpvCacheSelectionMode.DirectoryPicker

internal actual fun platformMpvCacheLocations(): List<MpvCacheLocation> = emptyList()

internal actual suspend fun platformSyncMpvConfigDirectory(source: PlatformFile) {
    source.createDirectories()
}

internal actual fun platformMpvRuntimeConfigDirectory(source: PlatformFile): PlatformFile = source

internal actual fun platformResolveMpvCacheDirectory(configured: PlatformFile): PlatformFile =
    configured.apply { createDirectories() }
