package com.skyd.podaura.model.repository.importexport.opml

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

interface IImportOpmlRepository {
    data class ImportOpmlResult(
        val time: Long,
        val importedFeedCount: Int,
    )

    fun importOpmlMeasureTime(
        opmlFile: PlatformFile,
        strategy: ImportOpmlConflictStrategy,
    ): Flow<ImportOpmlResult>
}