package com.skyd.anivu.ui.screen.settings.data.importexport

import android.net.Uri
import com.skyd.anivu.base.mvi.MviIntent

sealed interface ImportExportIntent : MviIntent {
    data object Init : ImportExportIntent
    data class ImportPrefer(val inputFile: Uri) : ImportExportIntent
    data class ExportPrefer(val outputFile: Uri) : ImportExportIntent
}