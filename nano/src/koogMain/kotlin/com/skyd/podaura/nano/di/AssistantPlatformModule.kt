package com.skyd.podaura.nano.di

import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.runtime.KoogAssistantRuntime
import com.skyd.podaura.nano.runtime.KoogTurnCapabilities
import com.skyd.podaura.nano.runtime.initializeNanoLogging
import org.koin.dsl.module

actual val assistantPlatformModule = module {
    initializeNanoLogging()
    single { KoogTurnCapabilities(capabilities = getAll(), plans = getAll()) }
    single<AssistantRuntime> { KoogAssistantRuntime(capabilities = get<KoogTurnCapabilities>()::create) }
}
