@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getResourceEnvironment
import org.jetbrains.compose.resources.getSystemEnvironment
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleScriptCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

// From https://youtrack.jetbrains.com/issue/CMP-6614#focus=Comments-27-10849123.0-0

val resourceEnvironmentFix: Unit = run {
    getResourceEnvironment = ::myResourceEnvironment
}

@Composable
fun ResourceEnvironmentFix(content: @Composable () -> Unit) {
    resourceEnvironmentFix

    val default = LocalComposeEnvironment.current
    CompositionLocalProvider(
        LocalComposeEnvironment provides object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment(): ResourceEnvironment {
                val environment = default.rememberEnvironment()
                return mapEnvironment(environment)
            }
        },
        content = content
    )
}

private fun myResourceEnvironment(): ResourceEnvironment {
    val environment = getSystemEnvironment()
    return mapEnvironment(environment)
}

private fun mapEnvironment(environment: ResourceEnvironment): ResourceEnvironment {
    val locale = NSLocale.preferredLanguages.firstOrNull()
        ?.let { NSLocale(it as String) }
        ?: NSLocale.currentLocale
    val script = locale.objectForKey(NSLocaleScriptCode) as? String

    return ResourceEnvironment(
        language = environment.language,
        region = when (environment.language.language) {
            "zh" -> when (script) {
                "Hans" -> RegionQualifier("CN")
                "Hant" -> RegionQualifier("TW")
                else -> environment.region
            }

            "ja" -> RegionQualifier("JP")

            "eo" -> RegionQualifier("UY")

            else -> environment.region
        },
        theme = environment.theme,
        density = environment.density
    )
}
