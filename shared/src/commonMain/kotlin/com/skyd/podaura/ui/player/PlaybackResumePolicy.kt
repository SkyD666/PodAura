package com.skyd.podaura.ui.player

/** Separates system pauses from explicit user intent, including overlapping interruptions. */
internal class PlaybackResumePolicy(
    initiallyBackground: Boolean = false,
    initiallyScreenLocked: Boolean = false,
) {
    var background = initiallyBackground
        private set
    private var interrupted = false
    private var resumeOnForeground = false
    private var resumeAfterInterruption = false
    private var userAllowsResume = true
    private var pictureInPictureActive = false
    private var screenLocked = initiallyScreenLocked
    private var resumeAfterUnlock = false

    /** Returns whether this transition needs a pause (lock) or a resume (unlock). */
    fun setScreenLocked(locked: Boolean, playing: Boolean, backgroundEnabled: Boolean): Boolean {
        if (screenLocked == locked) return false
        screenLocked = locked
        if (locked) {
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
        if (resume && background && !backgroundEnabled && !pictureInPictureActive) {
            resumeOnForeground = true
            return false
        }
        return resume
    }

    fun setPictureInPictureActive(active: Boolean): Boolean {
        pictureInPictureActive = active
        if (!active || interrupted || screenLocked) return false
        // PiP can finish starting after the background notification paused playback.
        return resumeOnForeground.also { resumeOnForeground = false }
    }

    fun userAction(allowsResume: Boolean = false, audioSessionActivated: Boolean = false) {
        userAllowsResume = allowsResume
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
        if (enabled || pictureInPictureActive || !playing || interrupted || screenLocked) return false
        resumeOnForeground = userAllowsResume
        return true
    }

    fun enterForeground(): Boolean {
        background = false
        if (interrupted || screenLocked) return false
        return resumeOnForeground.also { resumeOnForeground = false }
    }

    /** Reapply system policy when a deferred load starts playing or the engine becomes ready. */
    fun pauseWhenRequired(playing: Boolean, ready: Boolean, backgroundEnabled: Boolean): Boolean {
        if (!ready || !playing) return false
        if (screenLocked) {
            resumeAfterUnlock = userAllowsResume
            return true
        }
        if (interrupted) {
            resumeAfterInterruption = userAllowsResume
            return true
        }
        if (background && !backgroundEnabled && !pictureInPictureActive) {
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
        if (resume && screenLocked) {
            resumeAfterUnlock = true
            return false
        }
        if (resume && background && !backgroundEnabled && !pictureInPictureActive) {
            resumeOnForeground = true
            return false
        }
        return resume
    }
}

/** Null means this command does not change playback intent. */
internal fun PlayerCommand.playbackIntent(paused: Boolean): Boolean? = when (this) {
    is PlayerCommand.Paused -> !this.paused
    PlayerCommand.PlayOrPause -> paused
    is PlayerCommand.LoadList, is PlayerCommand.PlayFileInPlaylist,
    PlayerCommand.NextMedia, PlayerCommand.PreviousMedia -> true
    else -> null
}
