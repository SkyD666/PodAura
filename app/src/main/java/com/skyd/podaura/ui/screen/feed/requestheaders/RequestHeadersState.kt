package com.skyd.podaura.ui.screen.feed.requestheaders

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.mvi.MviViewState

data class RequestHeadersState(
    val headersState: HeadersState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = RequestHeadersState(
            headersState = HeadersState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface HeadersState {
    data class Success(val headers: FeedBean.RequestHeaders) : HeadersState
    data object Init : HeadersState
    data class Failed(val msg: String) : HeadersState
}