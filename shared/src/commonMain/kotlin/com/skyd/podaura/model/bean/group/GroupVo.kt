package com.skyd.podaura.model.bean.group

import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.blockString
import kotlinx.serialization.Serializable
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.default_feed_group

@Serializable
open class GroupVo(
    val groupId: String,
    open val name: String,
    open val isExpanded: Boolean,
) : BaseBean {
    fun toPo(orderPosition: Double): GroupBean {
        return GroupBean(
            groupId = groupId,
            name = name,
            isExpanded = isExpanded,
            orderPosition = orderPosition,
        )
    }

    override fun toString(): String {
        return "groupId: $groupId, name: $name, isExpanded: $isExpanded"
    }

    object DefaultGroup : GroupVo(
        DEFAULT_GROUP_ID,
        blockString(Res.string.default_feed_group),
        dataStore.getOrDefault(FeedDefaultGroupExpandPreference),
    ) {
        override val name: String
            get() = blockString(Res.string.default_feed_group)
        override val isExpanded: Boolean
            get() = dataStore.getOrDefault(FeedDefaultGroupExpandPreference)
    }

    companion object {
        const val DEFAULT_GROUP_ID = "default"
        fun GroupVo.isDefaultGroup(): Boolean = this.groupId == DEFAULT_GROUP_ID
    }
}