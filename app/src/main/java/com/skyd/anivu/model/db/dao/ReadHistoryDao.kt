package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.history.READ_HISTORY_TABLE_NAME
import com.skyd.anivu.model.bean.history.ReadHistoryBean
import com.skyd.anivu.model.bean.history.ReadHistoryWithArticle

@Dao
interface ReadHistoryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateReadHistory(readHistoryBean: ReadHistoryBean)

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
}