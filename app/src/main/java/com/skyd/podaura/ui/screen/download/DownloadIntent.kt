package com.skyd.podaura.ui.screen.download

import com.skyd.mvi.MviIntent

sealed interface DownloadIntent : MviIntent {
    data object Init : DownloadIntent
}