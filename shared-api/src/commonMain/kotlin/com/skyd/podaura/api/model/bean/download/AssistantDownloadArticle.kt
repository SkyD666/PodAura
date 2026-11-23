package com.skyd.podaura.api.model.bean.download

data class AssistantDownloadArticle(
    val articleId: String,
    val title: String,
    val feedUrl: String,
    val attachments: List<AssistantDownloadAttachment>,
)

data class AssistantDownloadAttachment(
    val url: String,
    val type: String?,
    val length: Long,
    val mediaKind: AttachmentMediaKind,
) {
    val isMedia: Boolean get() = mediaKind != AttachmentMediaKind.Other
}

enum class AttachmentMediaKind { Audio, Video, Playlist, Other }
