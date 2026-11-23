package com.skyd.podaura.api.model.bean.download

/** A local subscription and optional publication bounds. A null limit selects the whole range. */
data class AssistantDownloadScope(
    val feedUrl: String,
    val publishedFromMillis: Long? = null,
    val publishedBeforeMillis: Long? = null,
    val limit: Int? = null,
)
