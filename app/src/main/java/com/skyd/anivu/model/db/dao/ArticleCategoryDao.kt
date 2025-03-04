package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.article.ARTICLE_CATEGORY_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleCategoryBean

@Dao
interface ArticleCategoryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfNotExist(categories: List<ArticleCategoryBean>)

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_CATEGORY_TABLE_NAME
        WHERE ${ArticleCategoryBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    fun getArticleCategoryBean(articleId: String): List<ArticleCategoryBean>
}