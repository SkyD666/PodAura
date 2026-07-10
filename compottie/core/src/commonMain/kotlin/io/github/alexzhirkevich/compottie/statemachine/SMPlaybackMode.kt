package io.github.alexzhirkevich.compottie.statemachine

internal enum class SMPlaybackMode(val isReverse: Boolean, val isBounce: Boolean) {
    Forward(false, false),
    Reverse(true, false),
    Bounce(false, true),
    ReverseBounce(true, true);
}
