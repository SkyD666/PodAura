package com.skyd.anivu.model.bean.group.groupfeed

import androidx.room.ColumnInfo
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.group.GroupBean


data class GroupIdOrFeedUrlBean(
    @ColumnInfo(GroupBean.GROUP_ID_COLUMN)
    val groupId: String?,
    @ColumnInfo(FeedBean.URL_COLUMN)
    val feedUrl: String?,
)
