package com.skyd.podaura.model.repository.article

import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.repository.download.IDownloadManager

class DownloadArticleProtectionResolver(
    private val downloadManager: IDownloadManager,
    private val enclosureDao: EnclosureDao,
) {
    suspend fun getProtectedArticleIds(enabled: Boolean): List<String> {
        if (!enabled) return emptyList()

        val tasks = downloadManager.getAllDownloadTasks()
        val articleIds = tasks.mapNotNull { task ->
            task.articleDownloadSource?.articleId?.takeIf { it.isNotBlank() }
        }.toMutableSet()
        val legacyUrls = tasks.mapNotNull { task ->
            task.url.takeIf { task.articleDownloadSource == null && it.isNotBlank() }
        }.distinct()

        legacyUrls.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { urls ->
            articleIds += enclosureDao.getArticleIdsByUrls(urls)
        }
        return articleIds.toList()
    }

    private companion object {
        const val SQLITE_BIND_CHUNK_SIZE = 900
    }
}
