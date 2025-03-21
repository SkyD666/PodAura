package com.skyd.anivu.model.repository.importexport

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface IImportRepository {
    data class ImportOpmlResult(
        val time: Long,
        val importedFeedCount: Int,
    )

    fun importOpmlMeasureTime(
        opmlUri: Uri,
        strategy: ImportOpmlConflictStrategy,
    ): Flow<ImportOpmlResult>
}