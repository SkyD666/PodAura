package com.skyd.podaura.ui.screen.about.update

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface UpdateIntent : MviIntent {
    data object CloseDialog : UpdateIntent
    data class CheckUpdate(val isRetry: Boolean) : UpdateIntent
}