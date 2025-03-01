package com.skyd.anivu.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@Composable
fun <T : Any> PagingRefreshStateIndicator(
    lazyPagingItems: LazyPagingItems<T>,
    errorContent: @Composable (Throwable) -> Unit = { ErrorPlaceholder(it.message.orEmpty()) },
    loadingContent: @Composable () -> Unit = { CircularProgressPlaceholder() },
    emptyContent: @Composable () -> Unit = {
        EmptyPlaceholder(modifier = Modifier.verticalScroll(rememberScrollState()))
    },
    placeholderPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    val loadStateRefresh = lazyPagingItems.loadState.refresh
    when {
        loadStateRefresh is LoadState.Error -> {
            Box(modifier = Modifier.padding(placeholderPadding)) {
                errorContent(loadStateRefresh.error)
            }
        }

        lazyPagingItems.itemCount > 0 -> content()

        loadStateRefresh is LoadState.Loading -> {
            Box(modifier = Modifier.padding(placeholderPadding)) {
                loadingContent()
            }
        }

        loadStateRefresh is LoadState.NotLoading -> {
            Box(modifier = Modifier.padding(placeholderPadding)) {
                emptyContent()
            }
        }
    }
}
