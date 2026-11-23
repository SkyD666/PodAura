package com.skyd.podaura.api.model.bean.article

/** Unprocessed local metadata. Callers decide how much text to show or process. */
data class ArticleRecord(
    val articleId: String,
    val title: String? = null,
    val sourceTitle: String? = null,
    val sourceNickname: String? = null,
    val dateMillis: Long? = null,
    val content: String? = null,
    val description: String? = null,
)
