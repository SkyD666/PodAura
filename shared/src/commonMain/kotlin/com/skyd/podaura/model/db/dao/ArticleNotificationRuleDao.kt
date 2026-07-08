package com.skyd.podaura.model.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.skyd.podaura.model.bean.ARTICLE_NOTIFICATION_RULE_TABLE_NAME
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean.Companion.ID_COLUMN
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
