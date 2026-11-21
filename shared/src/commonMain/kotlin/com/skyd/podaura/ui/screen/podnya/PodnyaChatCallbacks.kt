package com.skyd.podaura.ui.screen.podnya

import androidx.navigation3.runtime.NavKey

/** Host actions and date formatting supplied to the chat page. */
class PodnyaChatCallbacks(
    val onBack: () -> Unit,
    val onSettings: () -> Unit,
    val onSearch: () -> Unit,
    val navigate: (NavKey) -> Unit,
    val formatDate: (Long) -> String,
)
