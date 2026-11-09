package com.skyd.podaura.podnya.runtime

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggerFactory
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker

/** Call during platform module initialization, before any Koog class creates a static logger. */
fun initializePodnyaLogging() {
    val previous = KotlinLoggingConfiguration.loggerFactory
    if (previous !is PodnyaLoggerFactory) {
        KotlinLoggingConfiguration.loggerFactory = PodnyaLoggerFactory(previous)
    }
}

internal class PodnyaLoggerFactory(private val delegate: KLoggerFactory) : KLoggerFactory {
    override fun logger(name: String): KLogger = when {
        // Darwin's lambda name resolver can return an empty name. Never evaluate its payload.
        name.isBlank() || name == "UnknownLogger" || name == "ai.koog" || name.startsWith("ai.koog.") ->
            SilentLogger(name)

        else -> delegate.logger(name)
    }
}

private class SilentLogger(override val name: String) : KLogger {
    override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean = false
    override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) = Unit
}
