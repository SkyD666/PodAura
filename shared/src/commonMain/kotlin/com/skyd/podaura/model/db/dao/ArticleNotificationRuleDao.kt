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
import kotlinx.coroutines.flow.first

@Dao
interface ArticleNotificationRuleDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setArticleNotificationRule(bean: ArticleNotificationRuleBean)

    @Transaction
    @Query(value = "DELETE FROM $ARTICLE_NOTIFICATION_RULE_TABLE_NAME WHERE $ID_COLUMN = :id")
    suspend fun removeArticleNotificationRule(id: Int): Int

    @Transaction
    @Query(value = "SELECT * FROM $ARTICLE_NOTIFICATION_RULE_TABLE_NAME ORDER BY isManaged, id")
    fun getAllArticleNotificationRules(): Flow<List<ArticleNotificationRuleBean>>

    @Query("SELECT * FROM $ARTICLE_NOTIFICATION_RULE_TABLE_NAME WHERE id = :id")
    suspend fun getRule(id: Int): ArticleNotificationRuleBean?

    @Transaction
    suspend fun saveUserRule(rule: ArticleNotificationRuleBean) {
        require(!rule.isManaged && rule.name.isNotBlank() && rule.isValid())
        require(rule.id == 0 || getRule(rule.id)?.isManaged == false)
        setArticleNotificationRule(rule)
    }

    @Transaction
    suspend fun moveUserFeedTargets(oldUrl: String, newUrl: String) {
        getAllArticleNotificationRules().first()
            .filter { !it.isManaged && oldUrl in it.feedUrls }
            .forEach { rule ->
                setArticleNotificationRule(
                    rule.copy(
                        feedUrls = rule.feedUrls
                            .map { if (it == oldUrl) newUrl else it }
                            .distinct(),
                    )
                )
            }
    }
}
