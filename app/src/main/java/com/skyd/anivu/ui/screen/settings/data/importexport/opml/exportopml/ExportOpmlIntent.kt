package com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml

import android.net.Uri
import com.skyd.anivu.base.mvi.MviIntent

sealed interface ExportOpmlIntent : MviIntent {
    data object Init : ExportOpmlIntent
    data class ExportOpml(val outputDir: Uri) : ExportOpmlIntent
}