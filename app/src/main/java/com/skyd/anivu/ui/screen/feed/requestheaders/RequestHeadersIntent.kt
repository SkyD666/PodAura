package com.skyd.anivu.ui.screen.feed.requestheaders

import com.skyd.anivu.ui.mvi.MviIntent
import com.skyd.anivu.model.bean.feed.FeedBean

sealed interface RequestHeadersIntent : MviIntent {
    data class Init(val feedUrl: String) : RequestHeadersIntent
    data class UpdateHeaders(val feedUrl: String, val headers: FeedBean.RequestHeaders) :
        RequestHeadersIntent
}