package com.skyd.podaura.ui.screen.feed.requestheaders

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.feed.FeedBean

sealed interface RequestHeadersIntent : MviIntent {
    data class Init(val feedUrl: String) : RequestHeadersIntent
    data class UpdateHeaders(val feedUrl: String, val headers: FeedBean.RequestHeaders) :
        RequestHeadersIntent
}