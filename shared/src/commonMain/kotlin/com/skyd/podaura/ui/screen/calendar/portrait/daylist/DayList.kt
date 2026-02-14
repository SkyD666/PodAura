package com.skyd.podaura.ui.screen.calendar.portrait.daylist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.getOrNull
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.screen.calendar.portrait.daylist.item.ArticleItem
import com.skyd.podaura.ui.screen.calendar.portrait.daylist.item.TimeItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DayList(
    day: Long,
    contentPadding: PaddingValues,
    onError: (String) -> Unit,
    viewModel: DayListViewModel = koinViewModel(key = day.toString()),
) {
    val dispatch = viewModel.getDispatcher(day, startWith = DayListIntent.Init(day = day))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    when (val articleListState = uiState.articleListState) {
        is ArticleListState.Failed -> ErrorPlaceholder(
            modifier = Modifier.sizeIn(maxHeight = 200.dp),
            text = articleListState.msg,
            contentPadding = contentPadding,
        )

        is ArticleListState.Init -> CircularProgressPlaceholder(
            contentPadding = contentPadding,
        )

        is ArticleListState.Success -> {
            val result = articleListState.articlePagingDataFlow.collectAsLazyPagingItems()
            PagingRefreshStateIndicator(
                lazyPagingItems = result,
                placeholderPadding = contentPadding,
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    val keyGenerator = result.safeItemKey {
                        if (it is ArticleWithFeed) {
                            it.articleWithEnclosure.article.articleId
                        } else {
                            it
                        }
                    }
                    for (i in 0..<result.itemCount) {
                        val item = result.getOrNull(i)
                        if (item is Long) {
                            stickyHeader(key = keyGenerator(i)) {
                                TimeItem(item)
                            }
                        } else if (item is ArticleWithFeed) {
                            item(key = keyGenerator(i)) {
                                ArticleItem(item)
                            }
                        }
                    }
                }
            }
        }
    }

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is DayListEvent.InitResultEvent.Failed -> onError(event.msg)
        }
    }
}