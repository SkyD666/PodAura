package com.skyd.anivu.ui.screen.download

import com.skyd.anivu.ui.mvi.MviIntent

sealed interface DownloadIntent : MviIntent {
    data object Init : DownloadIntent
}