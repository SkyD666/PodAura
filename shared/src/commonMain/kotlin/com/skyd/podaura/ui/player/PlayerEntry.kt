package com.skyd.podaura.ui.player

import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid

sealed interface PlayerOpenRequest {
    data class Media(
        val mode: PlayDataMode,
        val requestId: String = Uuid.random().toString(),
    ) : PlayerOpenRequest

    data class Files(
        val files: List<PlatformFile>,
        val requestId: String = Uuid.random().toString(),
    ) : PlayerOpenRequest

    data object Resume : PlayerOpenRequest
}

/** All entry requests pass consent once, before a platform creates its player or service. */
abstract class PlayerEntry(
    private val hasAcceptedTerms: (() -> Boolean)? = null,
) {
    fun open(request: PlayerOpenRequest) {
        if (hasAcceptedTerms?.invoke() ?: dataStore.getOrDefault(AcceptTermsPreference)) {
            openAccepted(request)
        } else showTerms()
    }

    protected abstract fun openAccepted(request: PlayerOpenRequest)
    protected abstract fun showTerms()
}

expect open class PlatformPlayerEntry : PlayerEntry {
    protected override fun openAccepted(request: PlayerOpenRequest)
    protected override fun showTerms()
}
