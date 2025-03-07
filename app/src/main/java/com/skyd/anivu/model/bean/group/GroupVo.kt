package com.skyd.anivu.model.bean.group

import android.os.Parcelable
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
open class GroupVo(
    val groupId: String,
    open val name: String,
    open val isExpanded: Boolean,
) : BaseBean, Parcelable {
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

    object DefaultGroup :
        GroupVo(
            DEFAULT_GROUP_ID,
            appContext.getString(R.string.default_feed_group),
            appContext.dataStore.getOrDefault(FeedDefaultGroupExpandPreference),
        ) {
        private fun readResolve(): Any = DefaultGroup
        override val name: String
            get() = appContext.getString(R.string.default_feed_group)
        override val isExpanded: Boolean
            get() = appContext.dataStore.getOrDefault(FeedDefaultGroupExpandPreference)
    }

    companion object {
        const val DEFAULT_GROUP_ID = "default"
        fun GroupVo.isDefaultGroup(): Boolean = this.groupId == DEFAULT_GROUP_ID
    }
}