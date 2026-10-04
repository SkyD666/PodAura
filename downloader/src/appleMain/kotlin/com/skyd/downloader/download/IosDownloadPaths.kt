package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadDao

private val iosDocumentsPath = Regex(
    "^/.*/Containers/Data/Application/" +
            "[0-9A-Fa-f]{8}-(?:[0-9A-Fa-f]{4}-){3}[0-9A-Fa-f]{12}/Documents(?=/|$)"
)

// iOS can relocate the app container during an update; Documents keeps its relative layout.
internal suspend fun relocateIosDownloadPaths(dao: DownloadDao, documentsPath: String) {
    dao.getAllEntity().forEach { download ->
        val oldDocuments = iosDocumentsPath.find(download.path)?.value ?: return@forEach
        val path = documentsPath.trimEnd('/') + download.path.removePrefix(oldDocuments)
        if (path != download.path) dao.relocatePath(download.id, download.path, path)
    }
}
