package com.skyd.anivu.ui.screen.about.update

import com.skyd.anivu.ui.mvi.MviIntent

sealed interface UpdateIntent : MviIntent {
    data object CloseDialog : UpdateIntent
    data class CheckUpdate(val isRetry: Boolean) : UpdateIntent
}