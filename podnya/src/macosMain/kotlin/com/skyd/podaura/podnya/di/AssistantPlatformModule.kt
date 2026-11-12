package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module

actual val assistantPlatformModule = module {
    single<AssistantRuntime> {
        object : AssistantRuntime {
            override val available = false
            override fun run(request: AssistantTurnRequest, connection: AssistantConnection) =
                flowOf(AssistantRunEvent.Failed(AssistantError.UNAVAILABLE))

            override suspend fun verify() =
                AssistantConnectionResult(false, AssistantError.UNAVAILABLE)
        }
    }
}
