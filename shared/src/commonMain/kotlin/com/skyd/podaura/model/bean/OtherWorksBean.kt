package com.skyd.podaura.model.bean

import org.jetbrains.compose.resources.DrawableResource

data class OtherWorksBean(
    val name: String,
    val icon: DrawableResource,
    val description: String,
    val url: String,
) : BaseBean