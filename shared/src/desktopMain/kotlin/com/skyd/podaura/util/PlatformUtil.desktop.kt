package com.skyd.podaura.util

actual val platform: Platform
    get() {
        val system = System.getProperty("os.name").lowercase()
        return when {
            system.contains("win") -> Platform.Windows
            arrayOf("nix", "nux", "aix").any { system.contains(it) } -> Platform.Linux
            system.contains("mac") -> Platform.MacOS
            else -> Platform.Linux
        }
    }