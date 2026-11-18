package com.skyd.podaura.podnya.core

import kotlinx.coroutines.flow.Flow

interface AssistantRuntime {
    val available: Boolean
    fun run(request: AssistantTurnRequest, connection: AssistantConnection): Flow<AssistantRunEvent>

    /** Uses the saved configuration and synthetic data; does not access application business data. */
    suspend fun verify(connection: suspend () -> AssistantConnection): AssistantConnectionResult
}
