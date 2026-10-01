package com.skyd.podaura.ui.screen.article

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.article.ArticleDeleteResult
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.article.ArticleRepository.SelectedPlaylistMedia
import com.skyd.podaura.model.repository.download.SelectedDownloadPlan
import com.skyd.podaura.model.repository.download.SelectedDownloadResult
import kotlinx.coroutines.flow.Flow


internal sealed interface ArticlePartialStateChange {
    fun reduce(oldState: ArticleState): ArticleState

    sealed interface Selection : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            val selection = oldState.selectionState
            val editable = selection.active && !selection.busy && selection.confirmation == null
            val next = when (this) {
                is Enter -> if (!selection.active && !selection.busy) {
                    ArticleSelectionState(active = true, selectedIds = setOfNotNull(articleId))
                } else selection

                Exit -> ArticleSelectionState()
                is Cancelled -> ArticleSelectionState()
                is Toggle -> if (editable) selection.copy(
                    selectedIds = if (articleId in selection.selectedIds) {
                        selection.selectedIds - articleId
                    } else {
                        selection.selectedIds + articleId
                    },
                ) else selection

                Clear -> if (editable) selection.copy(selectedIds = emptySet()) else selection
                is Loading -> selection.copy(busy = true, confirmation = null, progress = progress)
                is Progress -> selection.copy(busy = true, progress = progress)
                is Completed -> selection.copy(busy = false, progress = null)
                is Selected -> selection.copy(selectedIds = articleIds, busy = false)
                is Confirmation -> selection.copy(
                    confirmation = plan,
                    busy = false,
                    progress = null
                )

                DismissConfirmation -> selection.copy(confirmation = null)
                is Downloaded -> selection.copy(busy = false, progress = null)
                is PlaylistPrepared -> selection.copy(
                    busy = false,
                    progress = null,
                    playlistMedias = result.medias.takeIf { it.isNotEmpty() },
                )

                DismissPlaylist -> selection.copy(playlistMedias = null)
                is Failed -> selection.copy(busy = false, progress = null)
            }
            return oldState.copy(selectionState = next)
        }

        data class Enter(val articleId: String?) : Selection
        data object Exit : Selection
        data class Toggle(val articleId: String) : Selection
        data object Clear : Selection
        data class Loading(val progress: BatchProgress? = null) : Selection
        data class Progress(val progress: BatchProgress) : Selection
        data class Completed(val result: BatchProgress) : Selection
        data class Cancelled(val progress: BatchProgress?) : Selection
        data class Selected(val articleIds: Set<String>) : Selection
        data class Confirmation(val plan: SelectedDownloadPlan) : Selection
        data object DismissConfirmation : Selection
        data class Downloaded(val result: SelectedDownloadResult) : Selection
        data class PlaylistPrepared(val result: SelectedPlaylistMedia) : Selection
        data object DismissPlaylist : Selection
        data class Failed(val msg: String) : Selection
    }

    sealed interface LoadingDialog : ArticlePartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ArticleState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Init : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = filterMask,
                    articleListState = ArticleListState.Success(
                        articlePagingDataFlow = articlePagingDataFlow,
                        loading = false,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    articleListState = ArticleListState.Failed(msg = msg, loading = false),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    articleListState = oldState.articleListState.let {
                        when (it) {
                            is ArticleListState.Failed -> it.copy(loading = true)
                            is ArticleListState.Init -> it.copy(loading = true)
                            is ArticleListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data class Success(
            val articlePagingDataFlow: Flow<PagingData<ArticleWithFeed>>,
            val filterMask: Int,
        ) : Init

        data class Failed(val msg: String) : Init
        data object Loading : Init
    }

    sealed interface RefreshArticleList : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> {
                    val articleListState = oldState.articleListState
                    oldState.copy(
                        articleListState = when (articleListState) {
                            is ArticleListState.Init -> articleListState.copy(loading = false)
                            is ArticleListState.Failed -> articleListState.copy(loading = false)
                            is ArticleListState.Success -> articleListState.copy(
                                articlePagingDataFlow = articleListState.articlePagingDataFlow,
                                loading = false,
                            )
                        },
                        loadingDialog = false,
                    )
                }

                is Loading -> oldState.copy(
                    articleListState = oldState.articleListState.let {
                        when (it) {
                            is ArticleListState.Failed -> it.copy(loading = true)
                            is ArticleListState.Init -> it.copy(loading = true)
                            is ArticleListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data object Success : RefreshArticleList
        data object Loading : RefreshArticleList
        data class Failed(val msg: String) : RefreshArticleList
    }

    sealed interface FavoriteArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : FavoriteArticle
        data class Failed(val msg: String) : FavoriteArticle
    }

    sealed interface ReadArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ReadArticle
        data class Failed(val msg: String) : ReadArticle
    }

    sealed interface DeleteArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val result: ArticleDeleteResult) : DeleteArticle
        data class Failed(val msg: String) : DeleteArticle
    }

    sealed interface UpdateFilter : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = filterMask,
                    loadingDialog = false,
                )
            }
        }

        data class Success(val filterMask: Int) : UpdateFilter
    }

    data class OnEditFeedDialog(val feedUrl: String?) : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return oldState.copy(editFeedUrl = feedUrl)
        }
    }
}
