package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import com.skyd.podaura.model.repository.importexport.opml.ImportOpmlConflictStrategy
import com.skyd.podaura.ui.mvi.MviIntent
import io.github.vinceglb.filekit.PlatformFile

sealed interface ImportOpmlIntent : MviIntent {
    data object Init : ImportOpmlIntent
    data class ImportOpml(
        val opmlFile: PlatformFile,
        val strategy: ImportOpmlConflictStrategy,
    ) : ImportOpmlIntent
}