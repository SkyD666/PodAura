package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.runtime.KoogAssistantRuntime
import com.skyd.podaura.podnya.runtime.KoogTurnCapabilities
import com.skyd.podaura.podnya.runtime.initializePodnyaLogging
import org.koin.dsl.module

actual val assistantPlatformModule = module {
    initializePodnyaLogging()
    single { KoogTurnCapabilities(capabilities = getAll(), plans = getAll()) }
    single<AssistantRuntime> { KoogAssistantRuntime(capabilities = get<KoogTurnCapabilities>()::create) }
}
