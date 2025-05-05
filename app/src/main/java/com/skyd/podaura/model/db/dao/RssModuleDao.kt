package com.skyd.podaura.model.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.skyd.podaura.model.bean.article.RSS_MEDIA_TABLE_NAME
import com.skyd.podaura.model.bean.article.RssMediaBean

@Dao
interface RssModuleDao {
    @Transaction
    @Upsert
    suspend fun upsert(rssMediaBean: RssMediaBean)

    @Transaction
    @Query(
        """
        SELECT * FROM $RSS_MEDIA_TABLE_NAME
        WHERE ${RssMediaBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    suspend fun getRssMediaBean(articleId: String): RssMediaBean
}