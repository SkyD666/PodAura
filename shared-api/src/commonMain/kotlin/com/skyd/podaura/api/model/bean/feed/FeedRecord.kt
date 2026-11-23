package com.skyd.podaura.api.model.bean.feed

/** Public subscription details. Request headers and other credentials are never included. */
data class FeedRecord(
    val url: String,
    val title: String?,
    val nickname: String?,
    val description: String?,
    val customDescription: String?,
    val muted: Boolean,
)
