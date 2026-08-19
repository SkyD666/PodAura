package com.skyd.podaura.model.preference

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.vinceglb.filekit.BookmarkData
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.releaseBookmark
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

private val persistedDirectoryMutex = Mutex()

internal suspend fun persistDirectoryLocation(
    directory: PlatformFile,
    locationKey: Preferences.Key<String>,
    bookmarkKey: Preferences.Key<String>,
    beforePersist: suspend (PlatformFile) -> Unit = {},
): String = persistedDirectoryMutex.withLock {
    require(directory.isDirectory()) { "The selected location is not a directory" }
    val previousBookmark = dataStore.data.first()[bookmarkKey]
    val bookmarkValue = directory.bookmarkData().bytes.toHexString()
    val probeName = ".podaura-access-${Uuid.random()}.tmp"
    val probe = PlatformFile(directory, probeName)

    try {
        probe.sink().use { }
        (directory.list().firstOrNull { it.name == probeName } ?: probe)
            .delete(mustExist = false)
        beforePersist(directory)
        dataStore.edit { preferences ->
            preferences[locationKey] = directory.path
            preferences[bookmarkKey] = bookmarkValue
        }
    } catch (error: Throwable) {
        if (previousBookmark != bookmarkValue) {
            releaseBookmarkIfUnused(bookmarkValue)
        }
        throw error
    } finally {
        runCatching {
            (directory.list().firstOrNull { it.name == probeName } ?: probe)
                .delete(mustExist = false)
        }
    }

    releaseReplacedBookmark(previousBookmark, bookmarkValue)
    return directory.path
}

internal suspend fun resetDirectoryLocation(
    locationKey: Preferences.Key<String>,
    bookmarkKey: Preferences.Key<String>,
    default: String,
): Unit = persistedDirectoryMutex.withLock {
    val previousBookmark = dataStore.data.first()[bookmarkKey]
    dataStore.edit { preferences ->
        preferences[locationKey] = default
        preferences.remove(bookmarkKey)
    }
    releaseBookmarkIfUnused(previousBookmark)
}

internal suspend fun restoreDirectoryLocation(
    locationKey: Preferences.Key<String>?,
    bookmarkKey: Preferences.Key<String>,
    default: String,
): PlatformFile {
    if (locationKey == null) return PlatformFile(default)
    val preferences = dataStore.data.first()
    val bookmarkFile = preferences[bookmarkKey]
        ?.hexToByteArrayOrNull()
        ?.let { bytes ->
            runCatching { PlatformFile.fromBookmarkData(BookmarkData(bytes)) }.getOrNull()
        }
    return bookmarkFile ?: PlatformFile(preferences[locationKey] ?: default)
}

private suspend fun releaseReplacedBookmark(previous: String?, current: String) {
    if (previous != null && previous != current) releaseBookmarkIfUnused(previous)
}

private suspend fun releaseBookmarkIfUnused(value: String?) {
    if (value == null) return
    val stillInUse = dataStore.data.first().asMap().values.any { it == value }
    if (!stillInUse) releaseBookmark(value)
}

private fun releaseBookmark(value: String?) {
    value?.hexToByteArrayOrNull()?.let { bytes ->
        runCatching { PlatformFile.fromBookmarkData(BookmarkData(bytes)).releaseBookmark() }
    }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
    byte.toUByte().toString(radix = 16).padStart(2, '0')
}

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) return null
    return runCatching {
        ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(radix = 16).toByte()
        }
    }.getOrNull()
}
