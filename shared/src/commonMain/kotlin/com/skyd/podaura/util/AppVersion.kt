package com.skyd.podaura.util

data class AppVersion(
    val name: String,
    val code: Long?,
)

expect val appVersion: AppVersion
