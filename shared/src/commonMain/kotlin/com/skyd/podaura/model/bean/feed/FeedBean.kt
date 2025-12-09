package com.skyd.podaura.model.bean.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import co.touchlab.kermit.Logger
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
    @ColumnInfo(name = ORDER_POSITION_COLUMN)
    val orderPosition: Double = 0.0,
    /**
     * From low to high
     * | Favorite filter | 1- 2bit | 00=None,   01=Favorite, 10=Unfavorite, 11=Not used |
     * |   Read filter   | 3- 4bit | 00=None,   01=Read,     10=Unread,     11=Not used |
     * |      Sort       | 5- 8bit | 000x=Date, 001x=Title,  xxx0=Desc,      xxx1=Asc   |
     * |   Mute filter   | 9-10bit | 00=None,   01=Mute,     10=Unmute,     11=Not used |
     */
    @ColumnInfo(name = FILTER_MASK_COLUMN)
    val filterMask: Int = 0,
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
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val FILTER_MASK_COLUMN = "filterMask"

        const val TAG = "FeedBean"
        const val DEFAULT_FILTER_MASK = 0
        fun parseFilterMaskToFavorite(filterMask: Int): Boolean? {
            val twoBit = filterMask and 0B11
            return when (twoBit) {
                0 -> null
                1 -> true
                2 -> false
                else -> {
                    Logger.w(TAG) { "Illegal favorite filterMask: $twoBit" }
                    null
                }
            }
        }

        fun parseFilterMaskToRead(filterMask: Int): Boolean? {
            val twoBit = (filterMask and 0B1100) shr 2
            return when (twoBit) {
                0 -> null
                1 -> true
                2 -> false
                else -> {
                    Logger.w(TAG) { "Illegal read filterMask: $twoBit" }
                    null
                }
            }
        }

        fun parseFilterMaskToSort(filterMask: Int): SortBy {
            val twoBit = (filterMask and 0B11110000) shr 4
            val asc = (twoBit and 0B1) == 1
            val field = twoBit shr 1
            return when (field) {
                0 -> SortBy.Date(asc)
                1 -> SortBy.Title(asc)
                else -> {
                    Logger.w(TAG) { "Illegal read sort: $twoBit" }
                    SortBy.Date(asc)
                }
            }
        }

        fun parseFilterMaskToMute(filterMask: Int): Boolean? {
            val twoBit = (filterMask and 0B1100000000) shr 8
            return when (twoBit) {
                0 -> null
                1 -> true
                2 -> false
                else -> {
                    Logger.w(TAG) { "Illegal mute filterMask: $twoBit" }
                    null
                }
            }
        }

        fun newFilterMask(
            filterMask: Int,
            filterFavorite: Boolean? = parseFilterMaskToFavorite(filterMask),
            filterRead: Boolean? = parseFilterMaskToRead(filterMask),
            filterMute: Boolean? = parseFilterMaskToMute(filterMask),
            sort: SortBy = parseFilterMaskToSort(filterMask),
        ): Int {
            var newFilterMask = filterMask and 0B1111111111.inv()
            val filterFavoriteBit = if (filterFavorite == null) 0 else if (filterFavorite) 1 else 2
            val filterReadBit = if (filterRead == null) 0 else if (filterRead) 1 else 2
            val filterMuteBit = if (filterMute == null) 0 else if (filterMute) 1 else 2
            val sortField = when (sort) {
                is SortBy.Date -> 0
                is SortBy.Title -> 1
            }
            val sortBit = (sortField shl 1) or (if (sort.asc) 1 else 0)
            newFilterMask = newFilterMask or
                    filterFavoriteBit or
                    (filterReadBit shl 2) or
                    (sortBit shl 4) or
                    (filterMuteBit shl 8)
            return newFilterMask
        }
    }

    @Serializable
    data class RequestHeaders(val headers: Map<String, String>) : BaseBean

    val filterFavorite: Boolean? get() = parseFilterMaskToFavorite(filterMask)
    val filterRead: Boolean? get() = parseFilterMaskToRead(filterMask)
    val sort: SortBy get() = parseFilterMaskToSort(filterMask)

    sealed class SortBy(open val asc: Boolean) {
        data class Date(override val asc: Boolean) : SortBy(asc)
        data class Title(override val asc: Boolean) : SortBy(asc)

        companion object {
            val default = Date(false)
        }
    }
}