package com.skyd.podaura.model.bean.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val FEED_TABLE_NAME = "Feed"

@Serializable
@Entity(
    tableName = FEED_TABLE_NAME,
    indices = [
        Index(FeedBean.MUTE_COLUMN),
    ]
)
data class FeedBean(
    @PrimaryKey
    @ColumnInfo(name = URL_COLUMN)
    val url: String,
    @ColumnInfo(name = TITLE_COLUMN)
    val title: String? = null,
    @ColumnInfo(name = DESCRIPTION_COLUMN)
    val description: String? = null,
    @ColumnInfo(name = LINK_COLUMN)
    var link: String? = null,
    @ColumnInfo(name = ICON_COLUMN)
    var icon: String? = null,
    @ColumnInfo(name = GROUP_ID_COLUMN)
    var groupId: String? = null,
    @ColumnInfo(name = NICKNAME_COLUMN)
    var nickname: String? = null,
    @ColumnInfo(name = CUSTOM_DESCRIPTION_COLUMN)
    val customDescription: String? = null,
    @ColumnInfo(name = CUSTOM_ICON_COLUMN)
    val customIcon: String? = null,
    @ColumnInfo(name = SORT_XML_ARTICLES_ON_UPDATE_COLUMN)
    val sortXmlArticlesOnUpdate: Boolean = false,
    @ColumnInfo(name = REQUEST_HEADERS_COLUMN)
    val requestHeaders: RequestHeaders? = null,
    @ColumnInfo(name = MUTE_COLUMN)
    val mute: Boolean = false,
) : BaseBean {
    companion object {
        const val URL_COLUMN = "url"
        const val TITLE_COLUMN = "title"
        const val DESCRIPTION_COLUMN = "description"
        const val LINK_COLUMN = "link"
        const val ICON_COLUMN = "icon"
        const val GROUP_ID_COLUMN = "groupId"
        const val NICKNAME_COLUMN = "nickname"
        const val CUSTOM_DESCRIPTION_COLUMN = "customDescription"
        const val CUSTOM_ICON_COLUMN = "customIcon"
        const val SORT_XML_ARTICLES_ON_UPDATE_COLUMN = "sortXmlArticlesOnUpdate"
        const val REQUEST_HEADERS_COLUMN = "requestHeaders"
        const val MUTE_COLUMN = "mute"
    }

    @Serializable
    data class RequestHeaders(val headers: Map<String, String>) : BaseBean
}