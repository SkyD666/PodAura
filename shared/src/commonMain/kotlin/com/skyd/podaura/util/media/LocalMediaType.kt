package com.skyd.podaura.util.media

internal enum class LocalMediaKind {
    Audio,
    Video,
    Playlist,
    Other;

    val isPlayable: Boolean
        get() = this == Audio || this == Video
}

private val audioExtensions = setOf(
    "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "alac", "aiff", "aif",
)
private val videoExtensions = setOf(
    "mp4", "avi", "mkv", "mov", "flv", "wmv", "webm", "mpg", "mpeg", "3gp", "rmvb", "ts",
)
private val hlsMimeTypes = setOf(
    "application/vnd.apple.mpegurl",
    "application/x-mpegurl",
    "audio/mpegurl",
    "audio/x-mpegurl",
)
private val applicationAudioMimeTypes = setOf(
    "application/ogg",
    "application/x-flac",
    "application/x-ogg",
)
private val applicationVideoMimeTypes = setOf(
    "application/mp4",
    "application/vnd.ms-asf",
    "application/vnd.rn-realmedia",
    "application/x-matroska",
)
private val genericMimeTypes = setOf(
    "",
    "*/*",
    "application/octet-stream",
    "application/unknown",
    "binary/octet-stream",
)

internal fun detectLocalMediaKind(fileName: String, mimeType: String?): LocalMediaKind {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    val normalizedMimeType = mimeType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        .orEmpty()

    if (extension == "m3u8" || normalizedMimeType in hlsMimeTypes) {
        return LocalMediaKind.Playlist
    }

    return when {
        normalizedMimeType.startsWith("audio/") -> LocalMediaKind.Audio
        normalizedMimeType.startsWith("video/") -> LocalMediaKind.Video
        normalizedMimeType in applicationAudioMimeTypes -> LocalMediaKind.Audio
        normalizedMimeType in applicationVideoMimeTypes -> LocalMediaKind.Video

        normalizedMimeType !in genericMimeTypes -> LocalMediaKind.Other
        extension in audioExtensions -> LocalMediaKind.Audio
        extension in videoExtensions -> LocalMediaKind.Video
        else -> LocalMediaKind.Other
    }
}
