package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.skyd.anivu.model.bean.article.ENCLOSURE_TABLE_NAME
import com.skyd.anivu.model.bean.article.EnclosureBean
import kotlinx.coroutines.flow.Flow

@Dao
interface EnclosureDao {
    @Query(
        """
        SELECT * from $ENCLOSURE_TABLE_NAME 
        WHERE ${EnclosureBean.ARTICLE_ID_COLUMN} = :articleId
        AND ${EnclosureBean.URL_COLUMN} = :url
        """
    )
    suspend fun queryEnclosureByLink(
        articleId: String,
        url: String?,
    ): EnclosureBean?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun innerUpdateEnclosure(enclosureBean: EnclosureBean)

    @Transaction
    @Upsert
    suspend fun upsert(enclosureBeanList: List<EnclosureBean>)

    @Transaction
    @Delete
    suspend fun deleteEnclosure(enclosureBean: EnclosureBean): Int

    @Transaction
    @Query("DELETE FROM $ENCLOSURE_TABLE_NAME WHERE ${EnclosureBean.ARTICLE_ID_COLUMN} LIKE :articleId")
    suspend fun deleteEnclosure(articleId: String): Int

    @Transaction
    @Query(
        """
        SELECT * FROM $ENCLOSURE_TABLE_NAME 
        WHERE ${EnclosureBean.ARTICLE_ID_COLUMN} LIKE :articleId
        """
    )
    fun getEnclosureList(articleId: String): Flow<List<EnclosureBean>>

    @Transaction
    @Query(
        "SELECT ${EnclosureBean.ARTICLE_ID_COLUMN} FROM $ENCLOSURE_TABLE_NAME " +
                "WHERE ${EnclosureBean.URL_COLUMN} = :path"
    )
    suspend fun getMediaArticleId(path: String): String?
}