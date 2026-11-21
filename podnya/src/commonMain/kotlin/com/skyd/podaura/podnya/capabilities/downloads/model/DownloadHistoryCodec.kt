package com.skyd.podaura.podnya.capabilities.downloads.model

import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistoryCodec
import com.skyd.podaura.podnya.core.CapabilityRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

internal object DownloadHistoryCodec : AssistantHistoryCodec {
    override val capabilityId = "downloads"
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            // Older cards wrote the qualified Kotlin name for these two target types.
            val legacyTargets = listOf(
                AssistantDownloadTarget.Attachment.serializer(),
                AssistantDownloadTarget.DirectLink.serializer(),
            ).associateBy { it.descriptor.serialName }
            polymorphicDefaultDeserializer(AssistantDownloadTarget::class) { type ->
                legacyTargets[type?.substringAfterLast('.')]
            }
        }
    }

    override fun encodeContent(content: AssistantContent): CapabilityRecord? {
        if (content !is DownloadContent) return null
        return CapabilityRecord(
            capabilityId = capabilityId,
            version = 1,
            payload = json.encodeToString(content),
            fallback = content.historySnapshot().summary,
        )
    }

    override fun decodeContent(record: CapabilityRecord): AssistantContent {
        require(record.version == 1)
        return json.decodeFromString<DownloadContent>(record.payload)
    }

    fun retainedBytes(content: DownloadContent): Int =
        json.encodeToString(encodeContent(content)).encodeToByteArray().size
}
