package com.skyd.podaura.model.repository.importexport.opml

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface IExportOpmlRepository {
    fun exportOpmlMeasureTime(outputDir: Uri): Flow<Long>
}