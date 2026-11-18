package com.skyd.podaura.podnya.core

/** Stable API and preference values. Only explicitly supported connections use these. */
enum class AssistantReasoningEffort(val id: String, internal val timeoutOverride: AssistantTimeouts?) {
    LOW("low", null),
    MEDIUM("medium", AssistantTimeouts.EXTENDED),
    HIGH("high", AssistantTimeouts.EXTENDED),
    XHIGH("xhigh", AssistantTimeouts.EXTENDED),
    MAX("max", AssistantTimeouts.EXTENDED);

    companion object {
        fun fromId(id: String): AssistantReasoningEffort? = entries.firstOrNull { it.id == id }
    }
}
