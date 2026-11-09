package com.skyd.podaura.ui.screen.podnya

/** Host actions and date formatting supplied to the chat page. */
class PodnyaChatCallbacks(
    val onBack: () -> Unit,
    val onSettings: () -> Unit,
    val onSearch: () -> Unit,
    val onOpenArticle: (String) -> Unit,
    val onOpenFeed: (String) -> Unit,
    val formatDate: (Long) -> String,
)
