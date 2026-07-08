package com.skyd.podaura.model.db.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.history.READ_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.ReadHistoryBean
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface ReadHistoryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReadHistory(readHistoryBean: ReadHistoryBean)

    @Transaction
    @Query(
        "DELETE FROM $READ_HISTORY_TABLE_NAME " +
                "WHERE ${ReadHistoryBean.ARTICLE_ID_COLUMN} = :articleId"
    )
    suspend fun deleteReadHistory(articleId: String): Int

    @Transaction
    @Query("DELETE FROM $READ_HISTORY_TABLE_NAME")
    suspend fun deleteAllReadHistory(): Int

    @Transaction
    @Query("SELECT * FROM $READ_HISTORY_TABLE_NAME ORDER BY ${ReadHistoryBean.LAST_TIME_COLUMN} DESC")
    fun getReadHistoryList(): PagingSource<Int, ReadHistoryWithArticle>

    @Transaction
    @RawQuery(observedEntities = [ReadHistoryBean::class, FeedBean::class, ArticleBean::class])
    fun searchReadHistoryList(sql: RoomRawQuery): PagingSource<Int, ReadHistoryWithArticle>
}
