package com.skyd.podaura.ui.screen.search

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedViewBean
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SearchStateTest {
    @Test
    fun interleavedSearchResultsAndFailuresStayInTheirOwnTab() {
        val feeds = SearchResultState.Success(flowOf(PagingData.empty<FeedViewBean>()))
        val articles = SearchResultState.Success(flowOf(PagingData.empty<ArticleWithFeed>()))
        var state = SearchState.initial()
        state = SearchPartialStateChange.FeedSearchResult(feeds).reduce(state)
        state = SearchPartialStateChange.ArticleSearchResult(SearchResultState.Loading).reduce(state)
        assertSame(expected = feeds, actual = state.feedSearchResultState)

        state = SearchPartialStateChange.ArticleSearchResult(articles).reduce(state)
        assertSame(expected = feeds, actual = state.feedSearchResultState)
        assertSame(expected = articles, actual = state.articleSearchResultState)

        val failure = SearchResultState.Failed(msg = "Search failed")
        state = SearchPartialStateChange.FeedSearchResult(failure).reduce(state)
        assertSame(expected = articles, actual = state.articleSearchResultState)
        state = SearchPartialStateChange.FeedSearchResult(feeds).reduce(state)
        state = SearchPartialStateChange.ArticleSearchResult(failure).reduce(state)
        assertSame(expected = feeds, actual = state.feedSearchResultState)
        assertEquals(expected = failure, actual = state.articleSearchResultState)
    }
}
