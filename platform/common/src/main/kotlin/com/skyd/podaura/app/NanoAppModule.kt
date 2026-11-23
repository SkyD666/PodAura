package com.skyd.podaura.app

import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_name
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.di.nanoModule
import com.skyd.podaura.nano.ui.NanoChat
import com.skyd.podaura.nano.ui.NanoSettings
import com.skyd.podaura.ui.screen.nano.NanoPages
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

/** Application wiring only: shared owns navigation; Nano owns the page content. */
internal val builtInNanoPlugins = listOf(articleSearchPlugin, feedSearchPlugin, downloadPlugin)

val nanoAppModule = nanoAppModule(builtInNanoPlugins)

internal fun nanoAppModule(plugins: List<NanoPlugin>) = module {
    includes(nanoModule)
    plugins.forEach { includes(*it.modules) }
    single {
        NanoPages(
            available = get<AssistantRuntime>().available,
            title = { getString(Res.string.nano_name) },
            chat = { callbacks ->
                NanoChat(
                    onBack = callbacks.onBack,
                    onSettings = callbacks.onSettings,
                    onSearch = callbacks.onSearch,
                    onAction = plugins.bindNavigation(scope = this, navigate = callbacks.navigate),
                    formatDate = callbacks.formatDate,
                )
            },
            settings = { onBack ->
                NanoSettings(onBack)
            },
        )
    }
}
