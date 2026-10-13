package com.skyd.podaura.ui.player

/** Separates system pauses from explicit user intent, including overlapping interruptions. */
internal class PlaybackResumePolicy(
    initiallyBackground: Boolean = false,
    initiallyScreenLocked: Boolean = false,
    // Android can distinguish screen transitions from leaving the app. iOS cannot.
    private val independentScreenPlayback: Boolean = false,
    var pauseOnScreenOff: Boolean = true,
) {
    var background = initiallyBackground
        private set
    private var interrupted = false
    private var resumeOnForeground = false
    private var resumeAfterInterruption = false
    private var userAllowsResume = true
    private var pictureInPictureActive = false
    private var screenLocked = initiallyScreenLocked
    private var lockedFromForeground = initiallyScreenLocked && !initiallyBackground
    private var resumeAfterUnlock = false
    private var userPlaybackOverride = false

    private val screenRequiresPause get() = screenLocked && pauseOnScreenOff && !userPlaybackOverride

    private fun backgroundRequiresPause(enabled: Boolean) =
        background && !enabled && !pictureInPictureActive && !userPlaybackOverride &&
                !(independentScreenPlayback && screenLocked && lockedFromForeground)

    /** A new Android screen-off event also ends permission granted on the lit lock screen. */
    fun onScreenOff() {
        if (independentScreenPlayback) userPlaybackOverride = false
    }

    /**
     * Returns whether this transition needs a pause (lock) or a resume (unlock).
     * Android must sample screen/keyguard state before entering the background, regardless of
     * broadcast order. A lock first observed after a genuine Home transition remains a background
     * lock; it must not undo that transition's pause. See [AndroidPlaybackEvents].
     */
    fun setScreenLocked(locked: Boolean, playing: Boolean, backgroundEnabled: Boolean): Boolean {
        if (screenLocked == locked) return false
        screenLocked = locked
        lockedFromForeground = locked && !background
        if (independentScreenPlayback) userPlaybackOverride = false
        if (locked) {
            if (!screenRequiresPause) return false
            resumeAfterUnlock = (playing && userAllowsResume) || resumeOnForeground
            resumeOnForeground = false
            return playing
        }
        val resume = resumeAfterUnlock
        resumeAfterUnlock = false
        if (interrupted) {
            resumeAfterInterruption = resumeAfterInterruption || resume
            return false
        }
        if (resume && backgroundRequiresPause(backgroundEnabled)) {
            resumeOnForeground = true
            return false
        }
        return resume
    }

    fun setPictureInPictureActive(active: Boolean): Boolean {
        pictureInPictureActive = active
        if (!active || interrupted || screenRequiresPause) return false
        // PiP can finish starting after the background notification paused playback.
        return resumeOnForeground.also { resumeOnForeground = false }
    }

    fun userAction(
        allowsResume: Boolean = false,
        audioSessionActivated: Boolean = false,
        explicitPlay: Boolean = false,
    ) {
        userAllowsResume = allowsResume
        if (!allowsResume) userPlaybackOverride = false
        else if (explicitPlay && (background || screenLocked)) userPlaybackOverride = true
        resumeOnForeground = false
        resumeAfterInterruption = false
        resumeAfterUnlock = false
        // An interruption-ended notification is not guaranteed. A successful explicit
        // activation proves that a stale interruption must no longer block Play.
        if (audioSessionActivated) this.audioSessionActivated()
    }

    /** Activation completion must not clear system resumes recorded while it was pending. */
    fun audioSessionActivated() {
        if (userAllowsResume) interrupted = false
    }

    fun enterBackground(playing: Boolean, enabled: Boolean): Boolean {
        background = true
        if (!backgroundRequiresPause(enabled) || !playing || interrupted || screenRequiresPause) return false
        resumeOnForeground = userAllowsResume
        return true
    }

    fun enterForeground(): Boolean {
        background = false
        userPlaybackOverride = false
        if (interrupted || screenRequiresPause) return false
        return resumeOnForeground.also { resumeOnForeground = false }
    }

    /** Reapply system policy when a deferred load starts playing or the engine becomes ready. */
    fun pauseWhenRequired(playing: Boolean, ready: Boolean, backgroundEnabled: Boolean): Boolean {
        if (!ready || !playing) return false
        if (screenRequiresPause) {
            resumeAfterUnlock = userAllowsResume
            return true
        }
        if (interrupted) {
            resumeAfterInterruption = userAllowsResume
            return true
        }
        if (backgroundRequiresPause(backgroundEnabled)) {
            resumeOnForeground = userAllowsResume
            return true
        }
        return false
    }

    fun beginInterruption(playing: Boolean) {
        if (!interrupted) resumeAfterInterruption =
            (playing && userAllowsResume) || resumeOnForeground || resumeAfterUnlock
        interrupted = true
        resumeOnForeground = false
        resumeAfterUnlock = false
    }

    fun endInterruption(shouldResume: Boolean, backgroundEnabled: Boolean): Boolean {
        interrupted = false
        val resume = resumeAfterInterruption && shouldResume
        resumeAfterInterruption = false
        if (!shouldResume) resumeAfterUnlock = false
        if (resume && screenRequiresPause) {
            resumeAfterUnlock = true
            return false
        }
        if (resume && backgroundRequiresPause(backgroundEnabled)) {
            resumeOnForeground = true
            return false
        }
        return resume
    }
}

/** Null means this command does not change playback intent. */
internal fun PlayerCommand.playbackIntent(paused: Boolean, eofReached: Boolean = false): Boolean? = when (this) {
    is PlayerCommand.Paused -> !this.paused
    PlayerCommand.PlayOrPause -> paused
    is PlayerCommand.LoadList, is PlayerCommand.PlayFileInPlaylist -> true
    PlayerCommand.NextMedia, PlayerCommand.PreviousMedia -> !paused || eofReached
    else -> null
}

/** Loading or selecting another item must not grant permission to play while paused. */
internal fun PlayerCommand.isExplicitPlay(paused: Boolean): Boolean = when (this) {
    is PlayerCommand.Paused -> !this.paused
    PlayerCommand.PlayOrPause -> paused
    else -> false
}
