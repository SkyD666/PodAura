package com.skyd.podaura.nano.ui

import androidx.compose.runtime.Composable
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_error_authentication
import com.skyd.nano.resources.nano_error_budget_exhausted
import com.skyd.nano.resources.nano_error_configuration_changed
import com.skyd.nano.resources.nano_error_configuration_required
import com.skyd.nano.resources.nano_error_conversation_storage
import com.skyd.nano.resources.nano_error_credential_storage_unavailable
import com.skyd.nano.resources.nano_error_credential_too_long
import com.skyd.nano.resources.nano_error_disclosure_required
import com.skyd.nano.resources.nano_error_endpoint_or_model
import com.skyd.nano.resources.nano_error_generation_limit
import com.skyd.nano.resources.nano_error_invalid_address
import com.skyd.nano.resources.nano_error_invalid_output
import com.skyd.nano.resources.nano_error_network
import com.skyd.nano.resources.nano_error_quota_exceeded
import com.skyd.nano.resources.nano_error_rate_limit
import com.skyd.nano.resources.nano_error_request_limited
import com.skyd.nano.resources.nano_error_stale_input
import com.skyd.nano.resources.nano_error_stream_interrupted
import com.skyd.nano.resources.nano_error_timeout
import com.skyd.nano.resources.nano_error_tools_unsupported
import com.skyd.nano.resources.nano_error_unavailable
import com.skyd.nano.resources.nano_search_context_required
import com.skyd.nano.resources.nano_settings_failed
import com.skyd.podaura.nano.core.AssistantError
import org.jetbrains.compose.resources.stringResource

@Composable
fun nanoErrorText(error: AssistantError): String = stringResource(
    when (error) {
        AssistantError.CONFIGURATION_STORAGE -> Res.string.nano_settings_failed
        AssistantError.CONFIGURATION_CHANGED -> Res.string.nano_error_configuration_changed
        AssistantError.CONVERSATION_STORAGE -> Res.string.nano_error_conversation_storage
        AssistantError.SEARCH_CONTEXT_REQUIRED -> Res.string.nano_search_context_required
        AssistantError.GENERATION_LIMIT -> Res.string.nano_error_generation_limit
        AssistantError.STALE_INPUT -> Res.string.nano_error_stale_input

        AssistantError.CONFIGURATION_REQUIRED -> Res.string.nano_error_configuration_required
        AssistantError.DISCLOSURE_REQUIRED -> Res.string.nano_error_disclosure_required
        AssistantError.INVALID_ADDRESS -> Res.string.nano_error_invalid_address
        AssistantError.CREDENTIAL_TOO_LONG -> Res.string.nano_error_credential_too_long
        AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE -> Res.string.nano_error_credential_storage_unavailable
        AssistantError.AUTHENTICATION -> Res.string.nano_error_authentication
        AssistantError.ENDPOINT_OR_MODEL -> Res.string.nano_error_endpoint_or_model
        AssistantError.RATE_LIMIT -> Res.string.nano_error_rate_limit
        AssistantError.QUOTA_EXCEEDED -> Res.string.nano_error_quota_exceeded
        AssistantError.REQUEST_LIMITED -> Res.string.nano_error_request_limited
        AssistantError.NETWORK -> Res.string.nano_error_network
        AssistantError.TIMEOUT -> Res.string.nano_error_timeout
        AssistantError.TOOLS_UNSUPPORTED -> Res.string.nano_error_tools_unsupported
        AssistantError.INVALID_OUTPUT -> Res.string.nano_error_invalid_output
        AssistantError.BUDGET_EXHAUSTED -> Res.string.nano_error_budget_exhausted
        AssistantError.STREAM_INTERRUPTED -> Res.string.nano_error_stream_interrupted
        AssistantError.UNAVAILABLE -> Res.string.nano_error_unavailable
    }
)
