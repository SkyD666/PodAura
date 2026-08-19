package com.skyd.podaura.model.preference.data.medialib

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.podaura.model.preference.persistDirectoryLocation
import com.skyd.podaura.model.preference.resetDirectoryLocation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

private val mediaLibBookmarkKey = stringPreferencesKey("mediaLibLocationBookmark")

suspend fun persistMediaLibraryLocation(directory: PlatformFile): String {
    val locationKey = checkNotNull(MediaLibLocationPreference.key) {
        "Changing the media library location is not supported on this platform"
    }
    return persistDirectoryLocation(
        directory = directory,
        locationKey = locationKey,
        bookmarkKey = mediaLibBookmarkKey,
    )
}

suspend fun resetMediaLibraryLocation() {
    val locationKey = MediaLibLocationPreference.key ?: return
    resetDirectoryLocation(
        locationKey = locationKey,
        bookmarkKey = mediaLibBookmarkKey,
        default = MediaLibLocationPreference.default,
    )
}

fun mediaLibraryLocationDisplayName(location: String): String = runCatching {
    PlatformFile(location).name.takeIf { it.isNotBlank() }
}.getOrNull() ?: location
