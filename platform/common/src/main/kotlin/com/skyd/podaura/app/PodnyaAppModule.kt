package com.skyd.podaura.app

import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.di.podnyaModule
import com.skyd.podaura.podnya.ui.PodnyaChat
import com.skyd.podaura.podnya.ui.PodnyaSettings
import com.skyd.podaura.ui.screen.podnya.PodnyaPages
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_name
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

/** Application wiring only: shared owns navigation; Podnya owns the page content. */
internal val builtInPodnyaPlugins = listOf(articleSearchPlugin, feedSearchPlugin, downloadPlugin)

val podnyaAppModule = podnyaAppModule(builtInPodnyaPlugins)

internal fun podnyaAppModule(plugins: List<PodnyaPlugin>) = module {
    includes(podnyaModule)
    plugins.forEach { includes(*it.modules) }
    single {
        PodnyaPages(
            available = get<AssistantRuntime>().available,
            title = { getString(Res.string.podnya_name) },
            chat = { callbacks ->
                PodnyaChat(
                    onBack = callbacks.onBack,
                    onSettings = callbacks.onSettings,
                    onSearch = callbacks.onSearch,
                    onAction = plugins.bindNavigation(scope = this, navigate = callbacks.navigate),
                    formatDate = callbacks.formatDate,
                )
            },
            settings = { onBack ->
                PodnyaSettings(onBack)
            },
        )
    }
}
