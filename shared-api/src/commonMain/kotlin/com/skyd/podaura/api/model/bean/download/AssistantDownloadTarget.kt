package com.skyd.podaura.api.model.bean.download

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.encodeUtf8

@Serializable
sealed interface AssistantDownloadTarget {
    val url: String
    val key: String

    @Serializable
    @SerialName("Attachment")
    data class Attachment(val articleId: String, override val url: String) :
        AssistantDownloadTarget {
        override val key: String
            get() = "attachment:${"${articleId.length}:$articleId$url".encodeUtf8().sha256().hex()}"
    }

    @Serializable
    @SerialName("DirectLink")
    data class DirectLink(override val url: String) : AssistantDownloadTarget {
        override val key: String
            get() = "link:${url.encodeUtf8().sha256().hex()}"
    }
}
