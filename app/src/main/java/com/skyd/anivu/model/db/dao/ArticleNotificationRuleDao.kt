package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.ARTICLE_NOTIFICATION_RULE_TABLE_NAME
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean.Companion.ID_COLUMN
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleNotificationRuleDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setArticleNotificationRule(bean: ArticleNotificationRuleBean)

    @Transaction
    @Query(value = "DELETE FROM $ARTICLE_NOTIFICATION_RULE_TABLE_NAME WHERE $ID_COLUMN = :id")
    suspend fun removeArticleNotificationRule(id: Int): Int

    @Transaction
    @Query(value = "SELECT * FROM $ARTICLE_NOTIFICATION_RULE_TABLE_NAME")
    fun getAllArticleNotificationRules(): Flow<List<ArticleNotificationRuleBean>>
}