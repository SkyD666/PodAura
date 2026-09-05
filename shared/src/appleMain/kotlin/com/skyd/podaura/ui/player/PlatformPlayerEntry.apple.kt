package com.skyd.podaura.ui.player

/** Native hosts supply navigation when they implement external document opening. */
actual open class PlatformPlayerEntry(
    private val onOpen: (PlayerOpenRequest) -> Unit,
    private val onShowTerms: () -> Unit,
) : PlayerEntry() {
    protected actual override fun openAccepted(request: PlayerOpenRequest) = onOpen(request)
    protected actual override fun showTerms() = onShowTerms()
}
