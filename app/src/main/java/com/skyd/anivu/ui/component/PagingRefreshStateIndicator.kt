package com.skyd.anivu.ui.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@Composable
fun <T : Any> PagingRefreshStateIndicator(
    lazyPagingItems: LazyPagingItems<T>,
    abnormalContent: @Composable (@Composable () -> Unit) -> Unit = { },
    errorContent: @Composable () -> Unit = { },
    loadingContent: @Composable () -> Unit = { CircularProgressPlaceholder() },
    emptyContent: @Composable () -> Unit = {
        EmptyPlaceholder(modifier = Modifier.verticalScroll(rememberScrollState()))
    },
    content: @Composable () -> Unit,
) {
    when (lazyPagingItems.loadState.refresh) {
        is LoadState.Error -> abnormalContent(errorContent)
        LoadState.Loading -> abnormalContent(loadingContent)
        is LoadState.NotLoading -> if (lazyPagingItems.itemCount > 0) {
            content()
        } else {
            abnormalContent(emptyContent)
        }
    }
}
