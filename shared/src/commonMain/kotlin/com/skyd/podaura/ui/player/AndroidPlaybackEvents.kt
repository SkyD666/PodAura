package com.skyd.podaura.ui.player

/** Normalizes Android visibility callbacks and screen broadcasts before applying playback policy. */
internal class AndroidPlaybackEvents(
    private val policy: PlaybackResumePolicy,
    private var screenOff: Boolean,
    private val playing: () -> Boolean,
    private val ready: () -> Boolean,
    private val backgroundEnabled: () -> Boolean,
    private val systemPause: (Boolean) -> Unit,
) {
    /**
     * [off] and [locked] must be sampled from PowerManager/KeyguardManager at this callback,
     * not cached from broadcasts. onStop can precede SCREEN_OFF for the same physical lock.
     */
    fun setPlayerVisible(visible: Boolean, off: Boolean, locked: Boolean) {
        if (visible) {
            if (policy.enterForeground()) systemPause(false)
            updateScreenState(off, locked)
        } else {
            updateScreenState(off, locked)
            if (policy.enterBackground(playing(), backgroundEnabled())) systemPause(true)
        }
    }

    fun updateScreenState(off: Boolean, locked: Boolean) {
        // A broadcast and onStop can report one transition; revoke explicit Play only once.
        if (off && !screenOff) policy.onScreenOff()
        screenOff = off
        val screenLocked = off || locked
        if (policy.setScreenLocked(screenLocked, playing(), backgroundEnabled())) {
            systemPause(screenLocked)
        } else {
            enforcePolicy()
        }
    }

    fun enforcePolicy() {
        if (policy.pauseWhenRequired(playing(), ready(), backgroundEnabled())) systemPause(true)
    }
}
