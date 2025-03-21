package com.skyd.anivu.model.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import kotlinx.serialization.Serializable

const val ARTICLE_NOTIFICATION_RULE_TABLE_NAME = "ArticleNotificationRule"

@Serializable
@Entity(tableName = ARTICLE_NOTIFICATION_RULE_TABLE_NAME)
data class ArticleNotificationRuleBean(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    val id: Int = 0,
    @ColumnInfo(name = NAME_COLUMN)
    var name: String,
    @ColumnInfo(name = REGEX_COLUMN)
    var regex: String,
) : BaseBean {
    companion object {
        const val ID_COLUMN = "id"
        const val NAME_COLUMN = "name"
        const val REGEX_COLUMN = "regex"
    }

    private fun isValid() = regex.isNotBlank() && runCatching { Regex(regex) }.getOrNull() != null

    fun match(data: ArticleWithEnclosureBean): Boolean {
        if (!isValid()) return false
        return Regex(regex).run {
            matches(data.article.title.orEmpty()) ||
                    matches(data.article.description.orEmpty()) ||
                    matches(data.article.content.orEmpty()) ||
                    matches(data.article.content.orEmpty())
        }
    }
}

