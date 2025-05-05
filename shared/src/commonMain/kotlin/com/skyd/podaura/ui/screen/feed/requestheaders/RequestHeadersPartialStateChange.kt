package com.skyd.podaura.ui.screen.feed.requestheaders

import com.skyd.podaura.model.bean.feed.FeedBean


internal sealed interface RequestHeadersPartialStateChange {
    fun reduce(oldState: RequestHeadersState): RequestHeadersState

    sealed interface LoadingDialog : RequestHeadersPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: RequestHeadersState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface UpdateHeaders : RequestHeadersPartialStateChange {
        override fun reduce(oldState: RequestHeadersState): RequestHeadersState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : UpdateHeaders
        data class Failed(val msg: String) : UpdateHeaders
    }

    sealed interface Header : RequestHeadersPartialStateChange {
        override fun reduce(oldState: RequestHeadersState): RequestHeadersState {
            return when (this) {
                is Success -> oldState.copy(
                    headersState = HeadersState.Success(
                        headers = headers ?: FeedBean.RequestHeaders(mapOf()),
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    headersState = HeadersState.Failed(msg = msg),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val headers: FeedBean.RequestHeaders?) : Header
        data class Failed(val msg: String) : Header
    }
}
