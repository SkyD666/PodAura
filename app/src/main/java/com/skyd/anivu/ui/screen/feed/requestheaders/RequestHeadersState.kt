package com.skyd.anivu.ui.screen.feed.requestheaders

import com.skyd.anivu.base.mvi.MviViewState
import com.skyd.anivu.model.bean.feed.FeedBean

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