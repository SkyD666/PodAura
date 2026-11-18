package com.skyd.podaura.podnya.ui

import androidx.compose.runtime.Composable
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_error_authentication
import com.skyd.podnya.resources.podnya_error_budget_exhausted
import com.skyd.podnya.resources.podnya_error_configuration_changed
import com.skyd.podnya.resources.podnya_error_configuration_required
import com.skyd.podnya.resources.podnya_error_conversation_storage
import com.skyd.podnya.resources.podnya_error_credential_storage_unavailable
import com.skyd.podnya.resources.podnya_error_credential_too_long
import com.skyd.podnya.resources.podnya_error_disclosure_required
import com.skyd.podnya.resources.podnya_error_endpoint_or_model
import com.skyd.podnya.resources.podnya_error_generation_limit
import com.skyd.podnya.resources.podnya_error_invalid_address
import com.skyd.podnya.resources.podnya_error_invalid_output
import com.skyd.podnya.resources.podnya_error_network
import com.skyd.podnya.resources.podnya_error_quota_exceeded
import com.skyd.podnya.resources.podnya_error_rate_limit
import com.skyd.podnya.resources.podnya_error_request_limited
import com.skyd.podnya.resources.podnya_error_stale_input
import com.skyd.podnya.resources.podnya_error_stream_interrupted
import com.skyd.podnya.resources.podnya_error_timeout
import com.skyd.podnya.resources.podnya_error_tools_unsupported
import com.skyd.podnya.resources.podnya_error_unavailable
import com.skyd.podnya.resources.podnya_search_context_required
import com.skyd.podnya.resources.podnya_settings_failed
import org.jetbrains.compose.resources.stringResource

@Composable
fun podnyaErrorText(error: AssistantError): String = stringResource(
    when (error) {
        AssistantError.CONFIGURATION_STORAGE -> Res.string.podnya_settings_failed
        AssistantError.CONFIGURATION_CHANGED -> Res.string.podnya_error_configuration_changed
        AssistantError.CONVERSATION_STORAGE -> Res.string.podnya_error_conversation_storage
        AssistantError.SEARCH_CONTEXT_REQUIRED -> Res.string.podnya_search_context_required
        AssistantError.GENERATION_LIMIT -> Res.string.podnya_error_generation_limit
        AssistantError.STALE_INPUT -> Res.string.podnya_error_stale_input

        AssistantError.CONFIGURATION_REQUIRED -> Res.string.podnya_error_configuration_required
        AssistantError.DISCLOSURE_REQUIRED -> Res.string.podnya_error_disclosure_required
        AssistantError.INVALID_ADDRESS -> Res.string.podnya_error_invalid_address
        AssistantError.CREDENTIAL_TOO_LONG -> Res.string.podnya_error_credential_too_long
        AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE -> Res.string.podnya_error_credential_storage_unavailable
        AssistantError.AUTHENTICATION -> Res.string.podnya_error_authentication
        AssistantError.ENDPOINT_OR_MODEL -> Res.string.podnya_error_endpoint_or_model
        AssistantError.RATE_LIMIT -> Res.string.podnya_error_rate_limit
        AssistantError.QUOTA_EXCEEDED -> Res.string.podnya_error_quota_exceeded
        AssistantError.REQUEST_LIMITED -> Res.string.podnya_error_request_limited
        AssistantError.NETWORK -> Res.string.podnya_error_network
        AssistantError.TIMEOUT -> Res.string.podnya_error_timeout
        AssistantError.TOOLS_UNSUPPORTED -> Res.string.podnya_error_tools_unsupported
        AssistantError.INVALID_OUTPUT -> Res.string.podnya_error_invalid_output
        AssistantError.BUDGET_EXHAUSTED -> Res.string.podnya_error_budget_exhausted
        AssistantError.STREAM_INTERRUPTED -> Res.string.podnya_error_stream_interrupted
        AssistantError.UNAVAILABLE -> Res.string.podnya_error_unavailable
    }
)
