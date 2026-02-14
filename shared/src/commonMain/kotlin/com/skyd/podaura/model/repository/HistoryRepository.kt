package com.skyd.podaura.model.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.RoomRawQuery
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.READ_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.ReadHistoryBean
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import com.skyd.podaura.model.db.dao.ReadHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class HistoryRepository(
    private val readHistoryDao: ReadHistoryDao,
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestReadHistoryList(): Flow<Pager<Int, ReadHistoryWithArticle>> = flow {
        emit(Pager(pagingConfig) { readHistoryDao.getReadHistoryList() })
    }.flowOn(Dispatchers.IO)

    fun requestMediaPlayHistoryList(): Flow<Pager<Int, MediaPlayHistoryWithArticle>> = flow {
        emit(Pager(pagingConfig) { mediaPlayHistoryDao.getMediaPlayHistoryList() })
    }.flowOn(Dispatchers.IO)

    fun searchReadHistoryList(query: String): Flow<Pager<Int, ReadHistoryWithArticle>> = flow {
        emit(Pager(pagingConfig) {
            readHistoryDao.searchReadHistoryList(genSearchReadHistoryQuery(query))
        })
    }.flowOn(Dispatchers.IO)

    fun searchMediaPlayHistoryList(query: String): Flow<Pager<Int, MediaPlayHistoryWithArticle>> =
        flow {
            emit(Pager(pagingConfig) {
                mediaPlayHistoryDao.searchMediaPlayHistoryList(genSearchMediaPlayHistoryQuery(query))
            })
        }.flowOn(Dispatchers.IO)

    fun deleteReadHistory(articleId: String): Flow<Int> = flow {
        emit(readHistoryDao.deleteReadHistory(articleId))
    }.flowOn(Dispatchers.IO)

    fun deleteMediaPlayHistory(path: String): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteMediaPlayHistory(path))
    }.flowOn(Dispatchers.IO)

    fun deleteAllReadHistory(): Flow<Int> = flow {
        emit(readHistoryDao.deleteAllReadHistory())
    }.flowOn(Dispatchers.IO)

    fun deleteAllMediaPlayHistory(): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteAllMediaPlayHistory())
    }.flowOn(Dispatchers.IO)

    private fun genSearchReadHistoryQuery(query: String): RoomRawQuery {
        val orderBy = "ORDER BY `${ReadHistoryBean.LAST_TIME_COLUMN}` DESC"
        if (query.isBlank()) {
            return RoomRawQuery("SELECT * FROM `${READ_HISTORY_TABLE_NAME}` $orderBy")
        }
        val texts = mutableListOf<String>()
        val articleColumns = arrayOf(
            ArticleBean.TITLE_COLUMN, ArticleBean.AUTHOR_COLUMN,
            ArticleBean.DESCRIPTION_COLUMN, ArticleBean.CONTENT_COLUMN
        ).map { "AT.`$it`" }
        val whereColumns = articleColumns.joinToString(" OR ") {
            texts += "%${query}%"
            "$it LIKE ?"
        }
        val sql = "SELECT * FROM `${READ_HISTORY_TABLE_NAME}` H " +
                "LEFT JOIN `${ARTICLE_TABLE_NAME}` AT " +
                "ON H.${ReadHistoryBean.ARTICLE_ID_COLUMN} = AT.${ArticleBean.ARTICLE_ID_COLUMN} " +
                "  WHERE $whereColumns $orderBy"
        return RoomRawQuery(sql) {
            texts.forEachIndexed { index, text -> it.bindText(index + 1, text) }
        }
    }

    private fun genSearchMediaPlayHistoryQuery(query: String): RoomRawQuery {
        val orderBy = "ORDER BY `${MediaPlayHistoryBean.LAST_TIME_COLUMN}` DESC"
        if (query.isBlank()) {
            return RoomRawQuery("SELECT * FROM `${MEDIA_PLAY_HISTORY_TABLE_NAME}` $orderBy")
        }
        val texts = mutableListOf<String>()
        val articleColumns = arrayOf(
            ArticleBean.TITLE_COLUMN, ArticleBean.AUTHOR_COLUMN,
            ArticleBean.DESCRIPTION_COLUMN, ArticleBean.CONTENT_COLUMN
        ).map { "AT.`$it`" }
        val historyColumns = arrayOf(MediaPlayHistoryBean.PATH_COLUMN).map { "H.`$it`" }
        val whereColumns = (articleColumns + historyColumns).joinToString(" OR ") {
            texts += "%${query}%"
            "$it LIKE ?"
        }
        val sql = "SELECT * FROM `${MEDIA_PLAY_HISTORY_TABLE_NAME}` H " +
                "LEFT JOIN `${ARTICLE_TABLE_NAME}` AT " +
                "ON H.${MediaPlayHistoryBean.ARTICLE_ID_COLUMN} = AT.${ArticleBean.ARTICLE_ID_COLUMN} " +
                "  WHERE $whereColumns $orderBy"
        return RoomRawQuery(sql) {
            texts.forEachIndexed { index, text -> it.bindText(index + 1, text) }
        }
    }
}
