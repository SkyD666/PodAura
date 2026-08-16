package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.ArticleTranslation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class InMemoryTranslationCacheEntry(
    val cacheKey: String,
    val articleId: String,
    val profileId: String,
    val translation: ArticleTranslation,
)

class InMemoryTranslationCache(
    private val maximumEntries: Int = 32,
    private val maximumBytes: Long = 8L * 1024L * 1024L,
) {
    private data class StoredEntry(
        val value: InMemoryTranslationCacheEntry,
        val byteSize: Long,
        val accessOrder: Long,
    )

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, StoredEntry>()
    private var accessCounter = 0L
    private var totalBytes = 0L

    suspend fun get(cacheKey: String): InMemoryTranslationCacheEntry? = mutex.withLock {
        val stored = entries[cacheKey] ?: return@withLock null
        entries[cacheKey] = stored.copy(accessOrder = ++accessCounter)
        stored.value.copy(translation = stored.value.translation.copy(fromCache = true))
    }

    suspend fun put(entry: InMemoryTranslationCacheEntry) = mutex.withLock {
        val byteSize = entry.translation.title.encodeToByteArray().size.toLong() +
                entry.translation.html.encodeToByteArray().size.toLong()
        if (byteSize > maximumBytes || maximumEntries <= 0) return@withLock
        entries.remove(entry.cacheKey)?.let { totalBytes -= it.byteSize }
        entries[entry.cacheKey] = StoredEntry(entry, byteSize, ++accessCounter)
        totalBytes += byteSize
        while (entries.size > maximumEntries || totalBytes > maximumBytes) {
            val oldest = entries.minByOrNull { it.value.accessOrder } ?: break
            entries.remove(oldest.key)
            totalBytes -= oldest.value.byteSize
        }
    }

    suspend fun clear() = mutex.withLock {
        entries.clear()
        totalBytes = 0L
    }

    suspend fun clearForProfile(profileId: String) = mutex.withLock {
        removeMatching { it.profileId == profileId }
    }

    suspend fun clearForArticle(articleId: String) = mutex.withLock {
        removeMatching { it.articleId == articleId }
    }

    suspend fun size(): Int = mutex.withLock { entries.size }

    private fun removeMatching(predicate: (InMemoryTranslationCacheEntry) -> Boolean) {
        val keys = entries.filterValues { predicate(it.value) }.keys.toList()
        keys.forEach { key ->
            entries.remove(key)?.let { totalBytes -= it.byteSize }
        }
    }
}
