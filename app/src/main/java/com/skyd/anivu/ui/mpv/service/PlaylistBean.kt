package com.skyd.anivu.ui.mpv.service

data class PlaylistBean(
    val path: String,
    val customMediaData: CustomMediaData,
)

data class CustomMediaData(
    val articleId: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val artist: String? = null,
)