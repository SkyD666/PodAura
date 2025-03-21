package com.skyd.anivu.model.bean.download.bt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.skyd.anivu.base.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val SESSION_PARAMS_TABLE_NAME = "SessionParams"

@Parcelize
@Serializable
@Entity(
    tableName = SESSION_PARAMS_TABLE_NAME,
    primaryKeys = [
        BtDownloadInfoBean.LINK_COLUMN
    ],
    foreignKeys = [
        ForeignKey(
            entity = BtDownloadInfoBean::class,
            parentColumns = [BtDownloadInfoBean.LINK_COLUMN],
            childColumns = [SessionParamsBean.LINK_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(SessionParamsBean.LINK_COLUMN),
    ]
)
data class SessionParamsBean(
    @ColumnInfo(name = LINK_COLUMN)
    val link: String,
    @ColumnInfo(name = DATA_COLUMN, typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray,
) : BaseBean, Parcelable {
    companion object {
        const val LINK_COLUMN = "link"
        const val DATA_COLUMN = "data"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionParamsBean

        if (link != other.link) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = link.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}