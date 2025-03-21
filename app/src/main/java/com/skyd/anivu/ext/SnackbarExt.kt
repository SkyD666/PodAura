package com.skyd.anivu.ext

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun SnackbarHostState.showSnackbar(
    scope: CoroutineScope,
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = true,
    duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
) {
    scope.launch {
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration,
        )
    }
}