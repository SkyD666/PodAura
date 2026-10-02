package com.skyd.podaura.ui.player

/** Separates system pauses from explicit user intent, including overlapping interruptions. */
internal class PlaybackResumePolicy(initiallyBackground: Boolean = false) {
    var background = initiallyBackground
        private set
    private var interrupted = false
    private var resumeOnForeground = false
    private var resumeAfterInterruption = false
    private var userAllowsResume = true

    fun userAction(allowsResume: Boolean = false) {
        userAllowsResume = allowsResume
        resumeOnForeground = false
        resumeAfterInterruption = false
    }

    fun enterBackground(playing: Boolean, enabled: Boolean): Boolean {
        background = true
        if (enabled || !playing || interrupted) return false
        resumeOnForeground = userAllowsResume
        return true
    }

    fun enterForeground(): Boolean {
        background = false
        if (interrupted) return false
        return resumeOnForeground.also { resumeOnForeground = false }
    }

    /** Reapply system policy when a deferred load starts playing or the engine becomes ready. */
    fun pauseWhenRequired(playing: Boolean, ready: Boolean, backgroundEnabled: Boolean): Boolean {
        if (!ready || !playing) return false
        if (interrupted) {
            resumeAfterInterruption = userAllowsResume
            return true
        }
        if (background && !backgroundEnabled) {
            resumeOnForeground = userAllowsResume
            return true
        }
        return false
    }

    fun beginInterruption(playing: Boolean) {
        if (!interrupted) resumeAfterInterruption = (playing && userAllowsResume) || resumeOnForeground
        interrupted = true
        resumeOnForeground = false
    }

    fun endInterruption(shouldResume: Boolean, backgroundEnabled: Boolean): Boolean {
        interrupted = false
        val resume = resumeAfterInterruption && shouldResume
        resumeAfterInterruption = false
        if (resume && background && !backgroundEnabled) {
            resumeOnForeground = true
            return false
        }
        return resume
    }
}
