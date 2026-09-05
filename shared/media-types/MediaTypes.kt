package com.skyd.podaura.media

/** Dependency-free catalog compiled by both commonMain and buildSrc. */
object MediaTypes {
    val audioExtensions: Set<String> = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "alac", "aiff", "aif",
    )
    val videoExtensions: Set<String> = setOf(
        "mp4", "avi", "mkv", "mov", "flv", "wmv", "webm", "mpg", "mpeg", "3gp", "rmvb", "ts",
    )
    val playableExtensions: Set<String> = audioExtensions + videoExtensions
    val playlistExtensions: Set<String> = setOf("m3u8")
    val playlistMimeTypes: Set<String> = setOf(
        "application/vnd.apple.mpegurl", "application/x-mpegurl", "audio/mpegurl", "audio/x-mpegurl",
    )
    val applicationAudioMimeTypes: Set<String> = setOf(
        "application/ogg", "application/x-flac", "application/x-ogg",
    )
    val applicationVideoMimeTypes: Set<String> = setOf(
        "application/mp4", "application/vnd.ms-asf", "application/vnd.rn-realmedia", "application/x-matroska",
    )
    val genericMimeTypes: Set<String> = setOf(
        "", "*/*", "application/octet-stream", "application/unknown", "binary/octet-stream",
    )
}
