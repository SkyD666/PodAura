package com.skyd.podaura.nano.di

import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.core.AssistantConnectionResult
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.core.AssistantTurnRequest
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module

actual val assistantPlatformModule = module {
    single<AssistantRuntime> {
        object : AssistantRuntime {
            override val available = false
            override fun run(request: AssistantTurnRequest, connection: AssistantConnection) =
                flowOf(AssistantRunEvent.Failed(AssistantError.UNAVAILABLE))

            override suspend fun verify(connection: suspend () -> AssistantConnection) =
                AssistantConnectionResult(false, AssistantError.UNAVAILABLE)
        }
    }
}
