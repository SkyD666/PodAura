package com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml

import android.net.Uri
import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.model.repository.importexport.opml.ImportOpmlConflictStrategy

sealed interface ImportOpmlIntent : MviIntent {
    data object Init : ImportOpmlIntent
    data class ImportOpml(
        val opmlUri: Uri,
        val strategy: ImportOpmlConflictStrategy,
    ) : ImportOpmlIntent
}