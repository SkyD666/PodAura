package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.TranslationHeader
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationProfileValidatorTest {
    private fun custom(endpoint: String, headers: List<TranslationHeader> = emptyList()) =
        TranslationProfile(
            id = "id",
            name = "Custom",
            providerType = TranslationProviderType.CustomPodAura,
            endpoint = endpoint,
            customHeaders = headers,
            config = TranslationProviderConfig.CustomPodAura(),
        )

    @Test
    fun requiresHttpsAndRejectsEmbeddedCredentials() {
        assertTrue(TranslationProfileValidator.isValid(custom("https://example.com/api")))
        assertFalse(TranslationProfileValidator.isValid(custom("http://example.com/api")))
        assertFalse(TranslationProfileValidator.isValid(custom("https://user:password@example.com/api")))
        assertTrue(
            TranslationProfileValidator.isValid(
                custom("http://localhost:8080"),
                allowLocalHttp = true,
            )
        )
    }

    @Test
    fun rejectsDangerousOrSensitiveHeaders() {
        assertFalse(
            TranslationProfileValidator.isValid(
                custom("https://example.com", listOf(TranslationHeader("Host", "attacker.example")))
            )
        )
        assertFalse(
            TranslationProfileValidator.isValid(
                custom("https://example.com", listOf(TranslationHeader("X-Key", "secret", sensitive = true)))
            )
        )
        assertFalse(
            TranslationProfileValidator.isValid(
                custom("https://example.com", listOf(TranslationHeader("X-Test", "ok\r\nInjected: yes")))
            )
        )
    }

    @Test
    fun rejectsDisabledDefaultProfile() {
        val profile = TranslationProfile(
            id = "id",
            name = "DeepL",
            providerType = TranslationProviderType.DeepL,
            enabled = false,
            isDefault = true,
            config = TranslationProviderConfig.DeepL(),
        )

        assertFalse(TranslationProfileValidator.isValid(profile))
        assertTrue(TranslationProfileValidator.isValid(profile.copy(isDefault = false)))
    }

    @Test
    fun acceptsGoogleConfigAndRejectsMismatchedConfig() {
        val profile = TranslationProfile(
            id = "id",
            name = "Google",
            providerType = TranslationProviderType.Google,
            credentialId = "credential",
            config = TranslationProviderConfig.Google(),
        )

        assertTrue(TranslationProfileValidator.isValid(profile))
        assertFalse(
            TranslationProfileValidator.isValid(
                profile.copy(config = TranslationProviderConfig.DeepL())
            )
        )
        assertFalse(TranslationProfileValidator.isValid(profile.copy(endpoint = "https://example.com")))
    }
}
