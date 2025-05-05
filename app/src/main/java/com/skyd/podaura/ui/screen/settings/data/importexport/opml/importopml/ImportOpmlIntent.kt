package com.skyd.podaura.ui.screen.settings.data.importexport.opml.importopml

import android.net.Uri
import com.skyd.podaura.model.repository.importexport.opml.ImportOpmlConflictStrategy
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface ImportOpmlIntent : MviIntent {
    data object Init : ImportOpmlIntent
    data class ImportOpml(
        val opmlUri: Uri,
        val strategy: ImportOpmlConflictStrategy,
    ) : ImportOpmlIntent
}