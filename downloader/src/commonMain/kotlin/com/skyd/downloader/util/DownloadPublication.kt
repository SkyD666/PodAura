package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.IOException

/** The staging path remains as a hard link until the queue commits Success. */
internal expect fun publishPartWithoutReplacement(part: PlatformFile, target: PlatformFile): Boolean

internal expect fun shareFileIdentity(part: PlatformFile, target: PlatformFile): Boolean

internal class DownloadPublicationException(cause: Throwable? = null) : IOException(
    "Unable to publish the download without risking replacement of an existing file at this location.",
    cause,
)
