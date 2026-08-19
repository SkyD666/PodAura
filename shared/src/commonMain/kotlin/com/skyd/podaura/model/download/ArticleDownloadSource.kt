package com.skyd.podaura.model.download

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ArticleDownloadSource(
    val articleId: String,
    val feedUrl: String,
)

private val articleDownloadSourceJson = Json {
    ignoreUnknownKeys = true
}

internal fun ArticleDownloadSource.encode(): String =
    articleDownloadSourceJson.encodeToString(this)

internal fun String?.decodeArticleDownloadSource(): ArticleDownloadSource? =
    this?.let {
        runCatching {
            articleDownloadSourceJson.decodeFromString<ArticleDownloadSource>(it)
        }.getOrNull()
    }
