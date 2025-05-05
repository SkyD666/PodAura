package com.skyd.podaura.ui.screen.download

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface DownloadIntent : MviIntent {
    data object Init : DownloadIntent
}