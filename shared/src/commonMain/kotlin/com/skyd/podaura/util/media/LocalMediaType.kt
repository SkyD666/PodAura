package com.skyd.podaura.util.media

import com.skyd.podaura.media.MediaTypes

internal enum class LocalMediaKind {
    Audio,
    Video,
    Playlist,
    Other;

    val isPlayable: Boolean
        get() = this == Audio || this == Video
}

internal fun detectLocalMediaKind(fileName: String, mimeType: String?): LocalMediaKind {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    val normalizedMimeType = mimeType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        .orEmpty()

    if (extension in MediaTypes.playlistExtensions || normalizedMimeType in MediaTypes.playlistMimeTypes) {
        return LocalMediaKind.Playlist
    }

    return when {
        normalizedMimeType.startsWith("audio/") -> LocalMediaKind.Audio
        normalizedMimeType.startsWith("video/") -> LocalMediaKind.Video
        normalizedMimeType in MediaTypes.applicationAudioMimeTypes -> LocalMediaKind.Audio
        normalizedMimeType in MediaTypes.applicationVideoMimeTypes -> LocalMediaKind.Video

        normalizedMimeType !in MediaTypes.genericMimeTypes -> LocalMediaKind.Other
        extension in MediaTypes.audioExtensions -> LocalMediaKind.Audio
        extension in MediaTypes.videoExtensions -> LocalMediaKind.Video
        else -> LocalMediaKind.Other
    }
}
