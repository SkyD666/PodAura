package com.skyd.anivu.ui.component.deeplink

import androidx.navigation.NavOptions

data class DeepLinkData(
    val deepLink: String,
    val navOptions: NavOptions? = null,
)
