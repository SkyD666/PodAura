package com.skyd.podaura.model.repository.importexport.opml

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

interface IExportOpmlRepository {
    fun exportOpmlMeasureTime(outputDir: PlatformFile): Flow<Long>
}