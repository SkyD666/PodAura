package com.skyd.fundation.util

expect val platform: Platform

enum class Platform {
    Android,
    iOS,
    Linux,
    macOS_Jvm,
    macOS_Native,
    Windows
}

val Platform.isPhone: Boolean get() = this == Platform.Android || this == Platform.iOS

val Platform.isJvm: Boolean get() = this == Platform.Windows || this == Platform.macOS_Jvm || this == Platform.Linux

expect fun exitApp()
