package com.skyd.anivu.model.bean.group

import android.os.Parcelable
import androidx.annotation.Keep
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.BaseBean
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.getString
import com.skyd.anivu.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import com.skyd.anivu.model.preference.dataStore
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.default_feed_group

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

    @Parcelize
    object DefaultGroup : GroupVo(
        DEFAULT_GROUP_ID,
        appContext.getString(Res.string.default_feed_group),
        dataStore.getOrDefault(FeedDefaultGroupExpandPreference),
    ) {
        @Keep
        private fun readResolve(): Any = DefaultGroup
        override val name: String
            get() = appContext.getString(Res.string.default_feed_group)
        override val isExpanded: Boolean
            get() = dataStore.getOrDefault(FeedDefaultGroupExpandPreference)
    }

    companion object {
        const val DEFAULT_GROUP_ID = "default"
        fun GroupVo.isDefaultGroup(): Boolean = this.groupId == DEFAULT_GROUP_ID
    }
}