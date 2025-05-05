package com.skyd.podaura.util

expect val platform: Platform

enum class Platform {
    Android,
    IOS,
    Linux,
    MacOS,
    Windows
}