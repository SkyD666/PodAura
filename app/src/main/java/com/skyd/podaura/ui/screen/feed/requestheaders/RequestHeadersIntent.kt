package com.skyd.podaura.ui.screen.feed.requestheaders

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface RequestHeadersIntent : MviIntent {
    data class Init(val feedUrl: String) : RequestHeadersIntent
    data class UpdateHeaders(val feedUrl: String, val headers: FeedBean.RequestHeaders) :
        RequestHeadersIntent
}