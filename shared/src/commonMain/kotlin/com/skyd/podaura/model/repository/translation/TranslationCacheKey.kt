package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.TranslationContentSource
import com.skyd.podaura.model.bean.translation.TranslationProfile
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8

data class TranslationCacheIdentity(
    val cacheKey: String,
    val sourceContentHash: String,
    val profileFingerprint: String,
)

object TranslationCacheKey {
    fun create(
        articleId: String,
        contentSource: TranslationContentSource,
        title: String?,
        html: String,
        profile: TranslationProfile,
        targetLanguage: String,
        json: Json,
    ): TranslationCacheIdentity {
        val sourceHash = framedHash(title.orEmpty(), html)
        val profileFingerprint = framedHash(
            profile.providerType.name,
            profile.endpoint.orEmpty(),
            json.encodeToString(profile.customHeaders),
            profile.requestTimeoutMillis.toString(),
            json.encodeToString(profile.config),
        )
        return TranslationCacheIdentity(
            cacheKey = framedHash(
                articleId,
                contentSource.name,
                sourceHash,
                profile.id,
                profile.providerType.name,
                profileFingerprint,
                targetLanguage.uppercase(),
                TRANSLATION_ENVELOPE_VERSION.toString(),
                TRANSLATION_PROMPT_VERSION.toString(),
            ),
            sourceContentHash = sourceHash,
            profileFingerprint = profileFingerprint,
        )
    }

    fun framedHash(vararg fields: String): String = fields.joinToString(separator = "") { value ->
        "${value.encodeToByteArray().size}:$value"
    }.encodeUtf8().sha256().hex()
}
