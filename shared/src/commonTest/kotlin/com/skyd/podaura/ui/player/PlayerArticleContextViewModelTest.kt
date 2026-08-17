package com.skyd.podaura.ui.player

import com.skyd.podaura.model.repository.article.IArticleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerArticleContextViewModelTest {

    @Test
    fun switchingArticleIgnoresUpdatesFromThePreviousArticle() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeArticleRepository(
                initialFavorites = mapOf("article-a" to false, "article-b" to true)
            )
            val viewModel = PlayerArticleContextViewModel(repository)

            viewModel.bindArticle("article-a", initialFavorite = false)
            advanceUntilIdle()
            assertEquals("article-a", viewModel.state.value.articleId)
            assertFalse(viewModel.state.value.isFavorite!!)

            viewModel.bindArticle("article-b", initialFavorite = true)
            advanceUntilIdle()
            repository.favoriteState("article-a").value = true
            advanceUntilIdle()

            assertEquals("article-b", viewModel.state.value.articleId)
            assertTrue(viewModel.state.value.isFavorite!!)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun favoriteUpdateIsDisabledUntilTheDatabaseFlowConfirmsIt() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeArticleRepository(mapOf("article" to false))
            val gate = CompletableDeferred<Unit>()
            repository.favoriteGates["article"] = gate
            val viewModel = PlayerArticleContextViewModel(repository)
            viewModel.bindArticle("article", initialFavorite = false)
            advanceUntilIdle()

            viewModel.setFavorite(true)
            runCurrent()

            assertTrue(viewModel.state.value.isFavoriteUpdating)
            assertFalse(viewModel.state.value.isFavorite!!)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isFavoriteUpdating)
            assertTrue(viewModel.state.value.isFavorite!!)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun favoriteFailureRestoresTheButtonAndEmitsAnEvent() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeArticleRepository(mapOf("article" to false)).apply {
                favoriteFailures["article"] = IllegalStateException("write failed")
            }
            val viewModel = PlayerArticleContextViewModel(repository)
            viewModel.bindArticle("article", initialFavorite = false)
            advanceUntilIdle()
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

            viewModel.setFavorite(true)
            advanceUntilIdle()

            assertEquals(
                PlayerArticleContextEvent.FavoriteFailed("write failed"),
                event.await(),
            )
            assertFalse(viewModel.state.value.isFavoriteUpdating)
            assertFalse(viewModel.state.value.isFavorite!!)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletedOrMissingArticleMakesTheContextUnavailable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeArticleRepository(mapOf("article" to true))
            val viewModel = PlayerArticleContextViewModel(repository)
            viewModel.bindArticle("article", initialFavorite = true)
            advanceUntilIdle()

            repository.favoriteState("article").value = null
            advanceUntilIdle()

            assertEquals("article", viewModel.state.value.articleId)
            assertNull(viewModel.state.value.isFavorite)

            viewModel.bindArticle(null, null)
            advanceUntilIdle()

            assertNull(viewModel.state.value.articleId)
            assertNull(viewModel.state.value.isFavorite)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeArticleRepository(
        initialFavorites: Map<String, Boolean>,
    ) : IArticleRepository {
        private val favoriteStates = initialFavorites
            .mapValues { MutableStateFlow<Boolean?>(it.value) }
            .toMutableMap()

        val favoriteGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val favoriteFailures = mutableMapOf<String, Throwable>()

        fun favoriteState(articleId: String): MutableStateFlow<Boolean?> =
            favoriteStates.getOrPut(articleId) { MutableStateFlow(null) }

        override fun observeArticleFavorite(articleId: String): Flow<Boolean?> =
            favoriteState(articleId)

        override fun favoriteArticle(articleId: String, favorite: Boolean): Flow<Unit> = flow {
            favoriteGates[articleId]?.await()
            favoriteFailures[articleId]?.let { throw it }
            favoriteState(articleId).value = favorite
            emit(Unit)
        }

        override fun requestRealFeedUrls(
            feedUrls: List<String>,
            groupIds: List<String>,
            articleIds: List<String>,
        ): Flow<List<String>> = flowOf(emptyList())

        override fun refreshArticleList(feedUrls: List<String>, full: Boolean): Flow<Unit> =
            flowOf(Unit)

        override fun refreshGroupArticles(groupId: String?, full: Boolean): Flow<Unit> =
            flowOf(Unit)

        override fun readArticle(articleId: String, read: Boolean): Flow<Unit> = flowOf(Unit)

        override fun deleteArticle(articleId: String): Flow<Int> = flowOf(0)
    }
}
