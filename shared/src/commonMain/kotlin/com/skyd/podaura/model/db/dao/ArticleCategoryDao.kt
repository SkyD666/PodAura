package com.skyd.podaura.model.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.skyd.podaura.model.bean.article.ARTICLE_CATEGORY_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleCategoryBean

@Dao
interface ArticleCategoryDao {
    @Transaction
    @Upsert
    suspend fun upsert(categories: List<ArticleCategoryBean>)

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_CATEGORY_TABLE_NAME
        WHERE ${ArticleCategoryBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    suspend fun getArticleCategoryBean(articleId: String): List<ArticleCategoryBean>
}
