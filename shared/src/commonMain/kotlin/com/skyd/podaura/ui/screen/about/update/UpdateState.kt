package com.skyd.podaura.ui.screen.about.update

import com.skyd.podaura.model.bean.UpdateBean
import com.skyd.podaura.ui.mvi.MviViewState


data class UpdateState(
    var updateUiState: UpdateUiState,
    var loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = UpdateState(
            updateUiState = UpdateUiState.Init,
            loadingDialog = false,
        )
    }
}

sealed class UpdateUiState {
    data class OpenNewerDialog(val data: UpdateBean) : UpdateUiState()
    data object OpenNoUpdateDialog : UpdateUiState()
    data object Init : UpdateUiState()
}