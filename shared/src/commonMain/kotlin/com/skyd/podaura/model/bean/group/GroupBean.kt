package com.skyd.podaura.model.bean.group

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val GROUP_TABLE_NAME = "Group"

@Serializable
@Entity(
    tableName = GROUP_TABLE_NAME,
    indices = [
        Index(GroupBean.ORDER_POSITION_COLUMN),
    ]
)
open class GroupBean(
    @PrimaryKey
    @ColumnInfo(name = GROUP_ID_COLUMN)
    val groupId: String,
    @ColumnInfo(name = NAME_COLUMN)
    open val name: String,
    @ColumnInfo(name = IS_EXPANDED_COLUMN)
    open val isExpanded: Boolean = true,
    @ColumnInfo(name = ORDER_POSITION_COLUMN)
    open val orderPosition: Double,
) : BaseBean {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupBean) return false

        if (groupId != other.groupId) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    fun toVo(): GroupVo {
        if (groupId.isBlank() || groupId == GroupVo.DefaultGroup.groupId) {
            return GroupVo.DefaultGroup
        }
        return GroupVo(groupId, name, isExpanded)
    }

    companion object {
        const val NAME_COLUMN = "name"
        const val GROUP_ID_COLUMN = "groupId"
        const val IS_EXPANDED_COLUMN = "isExpanded"
        const val ORDER_POSITION_COLUMN = "orderPosition"
    }
}