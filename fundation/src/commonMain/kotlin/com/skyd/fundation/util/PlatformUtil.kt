package com.skyd.fundation.util

expect val platform: Platform

enum class Platform {
    Android,
    IOS,
    Linux,
    MacOS,
    Windows
}

val Platform.isPhone: Boolean get() = this == Platform.Android || this == Platform.IOS

expect fun exitApp()