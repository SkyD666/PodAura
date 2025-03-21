package com.skyd.anivu.ui.screen.about.update

import com.skyd.anivu.base.mvi.MviIntent

sealed interface UpdateIntent : MviIntent {
    data object CloseDialog : UpdateIntent
    data class CheckUpdate(val isRetry: Boolean) : UpdateIntent
    data class Update(val url: String?) : UpdateIntent
}