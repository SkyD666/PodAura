package com.skyd.podaura.model.bean

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.serialization.Serializable

const val ARTICLE_NOTIFICATION_RULE_TABLE_NAME = "ArticleNotificationRule"

@Serializable
@Entity(
    tableName = ARTICLE_NOTIFICATION_RULE_TABLE_NAME,
    indices = [Index(value = [ArticleNotificationRuleBean.IS_MANAGED_COLUMN, ArticleNotificationRuleBean.FEED_URLS_COLUMN])],
)
data class ArticleNotificationRuleBean(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    val id: Int = 0,
    @ColumnInfo(name = NAME_COLUMN)
    var name: String,
    @ColumnInfo(name = REGEX_COLUMN)
    var regex: String,
    @ColumnInfo(name = FEED_URLS_COLUMN, defaultValue = "'[]'")
    val feedUrls: List<String> = emptyList(),
    @ColumnInfo(name = GROUP_IDS_COLUMN, defaultValue = "'[]'")
    val groupIds: List<String> = emptyList(),
    @ColumnInfo(name = IS_MANAGED_COLUMN, defaultValue = "0")
    val isManaged: Boolean = false,
) : BaseBean {
    companion object {
        const val ID_COLUMN = "id"
        const val NAME_COLUMN = "name"
        const val REGEX_COLUMN = "regex"
        const val FEED_URLS_COLUMN = "feedUrls"
        const val GROUP_IDS_COLUMN = "groupIds"
        const val IS_MANAGED_COLUMN = "isManaged"
    }

    fun isValid(): Boolean {
        if (regex.isBlank() && feedUrls.isEmpty() && groupIds.isEmpty()) return false
        if (isManaged && (feedUrls.size != 1 || groupIds.isNotEmpty() || regex.isNotBlank())) return false
        return regex.isBlank() || runCatching { Regex(regex) }.isSuccess
    }

    fun match(data: ArticleWithEnclosureBean, groupId: String? = null): Boolean {
        if (!isValid()) return false
        if (feedUrls.isNotEmpty() && data.article.feedUrl !in feedUrls) return false
        if (groupIds.isNotEmpty() && (groupId ?: GroupVo.DEFAULT_GROUP_ID) !in groupIds) {
            return false
        }
        if (regex.isBlank()) return true
        val fields = listOf(data.article.title, data.article.description, data.article.content)
        return Regex(regex).let { pattern -> fields.any { pattern.matches(it.orEmpty()) } }
    }
}
