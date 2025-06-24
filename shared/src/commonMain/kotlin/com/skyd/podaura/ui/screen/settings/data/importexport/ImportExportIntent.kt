package com.skyd.podaura.ui.screen.settings.data.importexport

import com.skyd.mvi.MviIntent
import io.github.vinceglb.filekit.PlatformFile

sealed interface ImportExportIntent : MviIntent {
    data object Init : ImportExportIntent
    data class ExportOpml(val outputDir: PlatformFile) : ImportExportIntent
    data class ImportPrefer(val jsonFile: PlatformFile) : ImportExportIntent
    data class ExportPrefer(val outputFile: PlatformFile) : ImportExportIntent
}