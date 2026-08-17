package com.skyd.podaura.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.podaura.model.repository.article.IArticleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class PlayerArticleContextState(
    val articleId: String? = null,
    val isFavorite: Boolean? = null,
    val isFavoriteUpdating: Boolean = false,
)

sealed interface PlayerArticleContextEvent {
    data class FavoriteFailed(val message: String) : PlayerArticleContextEvent
}

class PlayerArticleContextViewModel(
    private val articleRepository: IArticleRepository,
) : ViewModel() {
    private val selectedArticle = MutableStateFlow<SelectedArticle?>(null)
    private val updatingArticleIds = MutableStateFlow<Set<String>>(emptySet())
    private val eventChannel = Channel<PlayerArticleContextEvent>(Channel.BUFFERED)

    val events: Flow<PlayerArticleContextEvent> = eventChannel.receiveAsFlow()

    val state: StateFlow<PlayerArticleContextState> = combine(
        selectedArticle.flatMapLatest { article ->
            if (article == null) {
                flowOf(PlayerArticleContextState())
            } else {
                articleRepository.observeArticleFavorite(article.articleId)
                    .map { favorite ->
                        PlayerArticleContextState(
                            articleId = article.articleId,
                            isFavorite = favorite,
                        )
                    }
                    .onStart {
                        emit(
                            PlayerArticleContextState(
                                articleId = article.articleId,
                                isFavorite = article.initialFavorite,
                            )
                        )
                    }
                    .catch { throwable ->
                        if (throwable is CancellationException) throw throwable
                        emit(PlayerArticleContextState(articleId = article.articleId))
                    }
            }
        },
        updatingArticleIds,
    ) { articleState, updatingIds ->
        articleState.copy(
            isFavoriteUpdating = articleState.articleId in updatingIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerArticleContextState(),
    )

    fun bindArticle(articleId: String?, initialFavorite: Boolean?) {
        val article = if (articleId != null && initialFavorite != null) {
            SelectedArticle(articleId = articleId, initialFavorite = initialFavorite)
        } else {
            null
        }
        if (selectedArticle.value?.articleId != article?.articleId) {
            selectedArticle.value = article
        }
    }

    fun setFavorite(favorite: Boolean) {
        val currentState = state.value
        val articleId = currentState.articleId ?: return
        if (currentState.isFavorite == null || articleId in updatingArticleIds.value) return

        updatingArticleIds.update { it + articleId }
        viewModelScope.launch {
            try {
                articleRepository.favoriteArticle(articleId, favorite).collect {}
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (selectedArticle.value?.articleId == articleId) {
                    eventChannel.send(
                        PlayerArticleContextEvent.FavoriteFailed(
                            throwable.message ?: throwable.toString()
                        )
                    )
                }
            } finally {
                updatingArticleIds.update { it - articleId }
            }
        }
    }

    private data class SelectedArticle(
        val articleId: String,
        val initialFavorite: Boolean,
    )
}
