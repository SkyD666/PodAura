package com.skyd.podaura.nano.core

/** Each request shares the remaining time in the turn, including while waiting for the first token. */
internal enum class AssistantTimeouts(
    val turnMillis: Long,
    val requestMillis: Long,
    val idleMillis: Long
) {
    STANDARD(
        turnMillis = AssistantLimits.TURN_TIMEOUT_MILLIS,
        requestMillis = 45_000,
        idleMillis = 15_000
    ),
    EXTENDED(turnMillis = 600_000, requestMillis = 600_000, idleMillis = 600_000);

    val connectMillis: Long get() = 15_000
}
