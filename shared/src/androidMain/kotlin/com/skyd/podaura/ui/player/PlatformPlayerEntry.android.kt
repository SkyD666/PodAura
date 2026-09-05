package com.skyd.podaura.ui.player

import android.app.Activity
import android.content.Intent

actual open class PlatformPlayerEntry(
    private val activity: Activity,
    private val onOpen: (PlayerOpenRequest) -> Unit,
) : PlayerEntry() {
    protected actual override fun openAccepted(request: PlayerOpenRequest) = onOpen(request)

    protected actual override fun showTerms() {
        activity.startActivity(
            Intent(
                activity,
                Class.forName("com.skyd.podaura.ui.activity.MainActivity"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        )
        activity.finish()
    }
}
