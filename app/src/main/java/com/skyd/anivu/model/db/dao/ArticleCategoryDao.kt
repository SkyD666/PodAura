package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.skyd.anivu.model.bean.article.ARTICLE_CATEGORY_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleCategoryBean

@Dao
interface ArticleCategoryDao {
    @Transaction
    @Upsert
    fun upsert(categories: List<ArticleCategoryBean>)

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_CATEGORY_TABLE_NAME
        WHERE ${ArticleCategoryBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    fun getArticleCategoryBean(articleId: String): List<ArticleCategoryBean>
}