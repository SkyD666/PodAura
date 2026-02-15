package com.skyd.podaura.ui.screen.calendar.daylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.fundation.ext.hour
import com.skyd.fundation.ext.withStyle
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.getOrNull
import com.skyd.podaura.ext.lastIndex
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.IndexBar
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.screen.calendar.daylist.item.ArticleItem
import com.skyd.podaura.ui.screen.calendar.daylist.item.TimeItem
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DayList(
    day: Long,
    contentPadding: PaddingValues = PaddingValues(),
    nestedScrollConnection: NestedScrollConnection? = null,
    onError: (String) -> Unit,
    viewModel: DayListViewModel = koinViewModel(key = day.toString()),
) {
    val scope = rememberCoroutineScope()
    val dispatch = viewModel.getDispatcher(day, startWith = DayListIntent.Init(day = day))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                val hourStrings = remember(articleListState.hours) {
                    articleListState.hours.map { it.withStyle.toString() }
                }
                var showIndexBarTip by remember { mutableStateOf(false) }
                var indexBarIndex by remember { mutableIntStateOf(0) }
                PagingRefreshStateIndicator(
                    lazyPagingItems = result,
                    placeholderPadding = contentPadding,
                ) {
                    Box {
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .thenIfNotNull(nestedScrollConnection) { nestedScroll(it) },
                            state = lazyListState,
                        ) {
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
                                    stickyHeader {
                                        TimeItem(item)
                                    }
                                } else if (item is ArticleWithFeed) {
                                    item(key = keyGenerator(i)) {
                                        ArticleItem(item)
                                    }
                                }
                            }
                        }
                        if (articleListState.hours.size > 1) {
                            IndexBar(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                indexes = articleListState.hours,
                                onDisplay = { hourStrings[it] },
                                onIndexChanged = { index ->
                                    indexBarIndex = index
                                    scope.launch {
                                        val listIndex = result.itemSnapshotList.indexOfFirst {
                                            (it as? Long)?.hour == articleListState.hours[index]
                                        }
                                        if (listIndex in 0..result.lastIndex) {
                                            lazyListState.scrollToItem(listIndex)
                                        }
                                    }
                                },
                                onShowTip = { showIndexBarTip = it },
                            )
                        }
                        AnimatedVisibility(
                            visible = showIndexBarTip,
                            modifier = Modifier.align(Alignment.Center),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Text(
                                text = hourStrings[indexBarIndex],
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(20.dp)
                                    .animateContentSize(),
                                style = MaterialTheme.typography.displayMedium,
                            )
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