package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantReasoningEffort

/** Inner waits share the outer turn's cancellation; they never restart its total budget. */
internal data class PodnyaTimeouts(
    val turnMillis: Long = AssistantLimits.TURN_TIMEOUT_MILLIS,
    val requestMillis: Long = 45_000,
    val idleMillis: Long = 15_000,
) {
    val connectMillis: Long = 15_000

    companion object {
        fun forConnection(connection: AssistantConnection): PodnyaTimeouts =
            if (connection.reasoningEffort != null && connection.reasoningEffort != AssistantReasoningEffort.LOW) {
                PodnyaTimeouts(turnMillis = 600_000, requestMillis = 600_000, idleMillis = 600_000)
            } else {
                PodnyaTimeouts()
            }
    }
}
