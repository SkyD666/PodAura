package com.skyd.podaura.ui.activity.player

/** A stopped PiP can be hidden by the lock screen; only a finished activity was dismissed. */
internal class PipDismissalTracker {
    private var enteredPip = false

    fun onModeChanged(inPipMode: Boolean) {
        // Android may clear the native PiP flag before delivering onDestroy.
        if (inPipMode) enteredPip = true
    }

    fun onFullPlayerResumed() {
        enteredPip = false
    }

    fun isDismissed(isFinishing: Boolean): Boolean = enteredPip && isFinishing
}
