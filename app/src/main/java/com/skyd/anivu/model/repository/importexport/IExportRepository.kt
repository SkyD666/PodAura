package com.skyd.anivu.model.repository.importexport

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface IExportRepository {
    fun exportOpmlMeasureTime(outputDir: Uri): Flow<Long>
}