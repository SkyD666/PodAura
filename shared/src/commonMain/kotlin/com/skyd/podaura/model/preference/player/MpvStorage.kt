package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.persistDirectoryLocation
import com.skyd.podaura.model.preference.resetDirectoryLocation
import com.skyd.podaura.model.preference.restoreDirectoryLocation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MpvCacheSelectionMode {
    DirectoryPicker,
    ManagedLocations,
    Unsupported,
}

enum class MpvCacheLocationKind {
    Internal,
    External,
}

data class MpvCacheLocation(
    val id: String,
    val path: String,
    val kind: MpvCacheLocationKind,
    val volumeName: String? = null,
)

data class MpvRuntimeDirectories(
    val configDirectory: String,
    val cacheDirectory: String,
)

private val mpvConfigDirBookmarkKey = stringPreferencesKey("mpvConfigDirBookmark")
private val mpvCacheDirBookmarkKey = stringPreferencesKey("mpvCacheDirBookmark")
private val configMutex = Mutex()
private val log = Logger.withTag("MpvStorage")

val mpvCacheSelectionMode: MpvCacheSelectionMode
    get() = platformMpvCacheSelectionMode

fun availableMpvCacheLocations(): List<MpvCacheLocation> = platformMpvCacheLocations()

suspend fun selectMpvConfigDirectory(directory: PlatformFile): String = configMutex.withLock {
    val locationKey = checkNotNull(MpvConfigDirPreference.key) {
        "Changing the MPV config directory is not supported on this platform"
    }
    persistDirectoryLocation(
        directory = directory,
        locationKey = locationKey,
        bookmarkKey = mpvConfigDirBookmarkKey,
        beforePersist = ::platformSyncMpvConfigDirectory,
    )
}

suspend fun syncMpvConfigDirectory() = configMutex.withLock {
    platformSyncMpvConfigDirectory(restoredMpvConfigDirectory())
}

suspend fun resetMpvConfigDirectory() = configMutex.withLock {
    val locationKey = MpvConfigDirPreference.key ?: return@withLock
    resetDirectoryLocation(
        locationKey = locationKey,
        bookmarkKey = mpvConfigDirBookmarkKey,
        default = MpvConfigDirPreference.default,
    )
    platformSyncMpvConfigDirectory(PlatformFile(MpvConfigDirPreference.default))
}

suspend fun selectMpvCacheDirectory(directory: PlatformFile): String {
    check(mpvCacheSelectionMode == MpvCacheSelectionMode.DirectoryPicker) {
        "Picking an arbitrary MPV cache directory is not supported on this platform"
    }
    return persistMpvCacheDirectory(directory)
}

suspend fun selectMpvCacheLocation(location: MpvCacheLocation): String {
    check(mpvCacheSelectionMode == MpvCacheSelectionMode.ManagedLocations) {
        "Managed MPV cache locations are not supported on this platform"
    }
    val available = availableMpvCacheLocations().firstOrNull {
        it.id == location.id && it.path == location.path
    } ?: error("The selected MPV cache location is not available")
    val directory = PlatformFile(available.path).apply { createDirectories() }
    return persistMpvCacheDirectory(directory)
}

suspend fun resetMpvCacheDirectory() {
    val locationKey = MpvCacheDirPreference.key ?: return
    resetDirectoryLocation(
        locationKey = locationKey,
        bookmarkKey = mpvCacheDirBookmarkKey,
        default = MpvCacheDirPreference.default,
    )
    platformResolveMpvCacheDirectory(PlatformFile(MpvCacheDirPreference.default))
}

suspend fun prepareMpvRuntimeDirectories(): MpvRuntimeDirectories {
    val configSource = restoredMpvConfigDirectory()
    var runtimeConfig = platformMpvRuntimeConfigDirectory(configSource)
    configMutex.withLock {
        runCatching { platformSyncMpvConfigDirectory(configSource) }
            .onFailure { error ->
                log.w(throwable = error) { "Failed to sync the selected MPV config directory" }
                if (!runtimeConfig.exists()) {
                    val defaultSource = PlatformFile(MpvConfigDirPreference.default)
                    runCatching {
                        platformSyncMpvConfigDirectory(defaultSource)
                        runtimeConfig = platformMpvRuntimeConfigDirectory(defaultSource)
                    }.onFailure { fallbackError ->
                        error.addSuppressed(fallbackError)
                    }
                }
            }
    }

    val configuredCache = restoreDirectoryLocation(
        locationKey = MpvCacheDirPreference.key,
        bookmarkKey = mpvCacheDirBookmarkKey,
        default = MpvCacheDirPreference.default,
    )
    val runtimeCache = platformResolveMpvCacheDirectory(configuredCache)
    val cacheHasBookmark = dataStore.data.first()[mpvCacheDirBookmarkKey] != null
    // Keep a managed removable-volume choice while it is offline; only migrate legacy raw paths.
    if (configuredCache.path != runtimeCache.path && !cacheHasBookmark) {
        MpvCacheDirPreference.key?.let { key ->
            dataStore.edit { preferences -> preferences[key] = runtimeCache.path }
        }
    }

    return MpvRuntimeDirectories(
        configDirectory = runtimeConfig.path,
        cacheDirectory = runtimeCache.path,
    )
}

suspend fun readMpvConfigFile(fileName: String): String = configMutex.withLock {
    requireSimpleFileName(fileName)
    val source = restoredMpvConfigDirectory()
    val sourceFile = PlatformFile(source, fileName)
    if (sourceFile.exists()) return@withLock sourceFile.readString()

    val runtimeFile = PlatformFile(platformMpvRuntimeConfigDirectory(source), fileName)
    if (runtimeFile.exists()) runtimeFile.readString() else ""
}

suspend fun writeMpvConfigFile(fileName: String, value: String) = configMutex.withLock {
    requireSimpleFileName(fileName)
    val source = restoredMpvConfigDirectory()
    val sourceFile = PlatformFile(source, fileName)
    sourceFile.writeString(value)

    val runtime = platformMpvRuntimeConfigDirectory(source)
    if (runtime.path != source.path) {
        runtime.createDirectories()
        PlatformFile(runtime, fileName).writeString(value)
    }
}

fun mpvDirectoryDisplayName(location: String): String = runCatching {
    PlatformFile(location).name.takeIf { it.isNotBlank() }
}.getOrNull() ?: location

private suspend fun persistMpvCacheDirectory(directory: PlatformFile): String {
    val locationKey = checkNotNull(MpvCacheDirPreference.key) {
        "Changing the MPV cache directory is not supported on this platform"
    }
    return persistDirectoryLocation(
        directory = directory,
        locationKey = locationKey,
        bookmarkKey = mpvCacheDirBookmarkKey,
    )
}

private suspend fun restoredMpvConfigDirectory(): PlatformFile = restoreDirectoryLocation(
    locationKey = MpvConfigDirPreference.key,
    bookmarkKey = mpvConfigDirBookmarkKey,
    default = MpvConfigDirPreference.default,
)

private fun requireSimpleFileName(fileName: String) {
    require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName) {
        "A simple file name is required"
    }
}

internal suspend fun mirrorMpvConfigDirectory(
    source: PlatformFile,
    runtime: PlatformFile,
) {
    require(source.isDirectory()) { "The MPV config source is not a directory" }
    if (source.path == runtime.path) return

    val parent = checkNotNull(runtime.parent()) { "The MPV runtime directory has no parent" }
    parent.createDirectories()
    val staging = PlatformFile(parent, "${runtime.name}.importing")
    val backup = PlatformFile(parent, "${runtime.name}.backup")
    staging.deleteDirectoryTree()
    backup.deleteDirectoryTree()
    staging.createDirectories()

    try {
        copyDirectoryContents(source = source, destination = staging)
        val hadRuntime = runtime.exists()
        if (hadRuntime) runtime.atomicMove(backup)
        try {
            staging.atomicMove(runtime)
        } catch (error: Throwable) {
            if (hadRuntime) {
                runtime.deleteDirectoryTree()
                if (backup.exists()) backup.atomicMove(runtime)
            }
            throw error
        }
        backup.deleteDirectoryTree()
    } finally {
        staging.deleteDirectoryTree()
        if (runtime.exists()) backup.deleteDirectoryTree()
    }
}

private suspend fun copyDirectoryContents(source: PlatformFile, destination: PlatformFile) {
    source.list().forEach { child ->
        val target = PlatformFile(destination, child.name)
        if (child.isDirectory()) {
            target.createDirectories()
            copyDirectoryContents(source = child, destination = target)
        } else {
            child.copyTo(target)
        }
    }
}

private suspend fun PlatformFile.deleteDirectoryTree() {
    if (!exists()) return
    if (isDirectory()) list().forEach { it.deleteDirectoryTree() }
    delete(mustExist = false)
}

internal expect val platformMpvCacheSelectionMode: MpvCacheSelectionMode

internal expect fun platformMpvCacheLocations(): List<MpvCacheLocation>

internal expect suspend fun platformSyncMpvConfigDirectory(source: PlatformFile)

internal expect fun platformMpvRuntimeConfigDirectory(source: PlatformFile): PlatformFile

internal expect fun platformResolveMpvCacheDirectory(configured: PlatformFile): PlatformFile
