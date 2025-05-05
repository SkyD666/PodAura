package com.skyd.podaura.model.bean.group.groupfeed

import androidx.room.ColumnInfo
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupBean


data class GroupIdOrFeedUrlBean(
    @ColumnInfo(GroupBean.GROUP_ID_COLUMN)
    val groupId: String?,
    @ColumnInfo(FeedBean.URL_COLUMN)
    val feedUrl: String?,
)
