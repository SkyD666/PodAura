package com.skyd.podaura.ui.screen.translation

import androidx.compose.runtime.Composable
import com.skyd.podaura.model.bean.translation.TranslationError
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.translation_error_authentication
import podaura.shared.generated.resources.translation_error_configuration
import podaura.shared.generated.resources.translation_error_content_rejected
import podaura.shared.generated.resources.translation_error_content_too_large
import podaura.shared.generated.resources.translation_error_html
import podaura.shared.generated.resources.translation_error_html_not_supported
import podaura.shared.generated.resources.translation_error_missing_credential
import podaura.shared.generated.resources.translation_error_network
import podaura.shared.generated.resources.translation_error_quota
import podaura.shared.generated.resources.translation_error_rate_limited
import podaura.shared.generated.resources.translation_error_secure_storage
import podaura.shared.generated.resources.translation_error_service_unavailable
import podaura.shared.generated.resources.translation_error_timeout
import podaura.shared.generated.resources.translation_error_unsupported_language

@Composable
fun translationErrorText(error: TranslationError?): String = stringResource(
    when (error) {
        TranslationError.Authentication -> Res.string.translation_error_authentication
        TranslationError.UnsupportedLanguage -> Res.string.translation_error_unsupported_language
        TranslationError.QuotaExceeded -> Res.string.translation_error_quota
        is TranslationError.RateLimited -> Res.string.translation_error_rate_limited
        is TranslationError.ContentTooLarge -> Res.string.translation_error_content_too_large
        TranslationError.NetworkUnavailable -> Res.string.translation_error_network
        TranslationError.Timeout -> Res.string.translation_error_timeout
        TranslationError.ContentRejected -> Res.string.translation_error_content_rejected
        TranslationError.InvalidHtml -> Res.string.translation_error_html
        TranslationError.HtmlNotSupported -> Res.string.translation_error_html_not_supported
        TranslationError.MissingCredential -> Res.string.translation_error_missing_credential
        TranslationError.SecureStorageUnavailable ->
            Res.string.translation_error_secure_storage

        TranslationError.InvalidConfiguration,
        null -> Res.string.translation_error_configuration

        TranslationError.ServiceUnavailable -> Res.string.translation_error_service_unavailable
    }
)
