package com.skyd.anivu.model.bean.group.groupfeed

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupBean


data class GroupOrFeedBean(
    @Embedded
    val id: GroupIdOrFeedUrlBean,
    @Relation(
        parentColumn = GroupBean.GROUP_ID_COLUMN,
        entityColumn = GroupBean.GROUP_ID_COLUMN,
    )
    val group: GroupBean?,
    @Relation(
        parentColumn = FeedBean.URL_COLUMN,
        entityColumn = FeedBean.URL_COLUMN,
    )
    val feed: FeedViewBean?,
)
