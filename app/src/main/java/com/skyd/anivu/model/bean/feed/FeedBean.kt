package com.skyd.anivu.model.bean.feed

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.model.bean.group.GroupVo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val FEED_TABLE_NAME = "Feed"

@Parcelize
@Serializable
@Entity(tableName = FEED_TABLE_NAME)
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
    @ColumnInfo(name = PREVIOUS_FEED_URL_COLUMN)
    val previousFeedUrl: String? = null,
    @ColumnInfo(name = NEXT_FEED_URL_COLUMN)
    val nextFeedUrl: String? = null,
) : BaseBean, Parcelable {
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
        const val PREVIOUS_FEED_URL_COLUMN = "previousFeedUrl"
        const val NEXT_FEED_URL_COLUMN = "nextFeedUrl"

        fun FeedBean.isDefaultGroup(): Boolean =
            this.groupId == null || this.groupId == GroupVo.DEFAULT_GROUP_ID
    }

    @Parcelize
    @Serializable
    data class RequestHeaders(val headers: Map<String, String>) : BaseBean, Parcelable
}