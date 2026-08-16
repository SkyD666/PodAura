package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.TranslationCapabilities
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult

interface TranslationProvider {
    val type: TranslationProviderType

    suspend fun getCapabilities(profile: TranslationProfile): TranslationCapabilities

    suspend fun verify(
        profile: TranslationProfile,
        credential: String? = null,
    ): TranslationVerificationResult

    suspend fun translateHtml(
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
    ): TranslationProviderResult
}
