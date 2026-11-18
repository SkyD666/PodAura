package com.skyd.podaura.podnya.core

/** Stable API and preference values. Only explicitly supported connections use these. */
enum class AssistantReasoningEffort(val id: String) {
    LOW("low"), MEDIUM("medium"), HIGH("high"), XHIGH("xhigh"), MAX("max");

    companion object {
        fun fromId(id: String): AssistantReasoningEffort? = entries.firstOrNull { it.id == id }
    }
}
