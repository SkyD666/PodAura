package com.skyd.podaura.model.bean.group.groupfeed

import androidx.room3.Embedded
import androidx.room3.Relation
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupBean


data class GroupOrFeedBean(
    @Embedded
    val id: GroupIdOrFeedUrlBean,
    @Relation(
        parentColumns = [GroupBean.GROUP_ID_COLUMN],
        entityColumns = [GroupBean.GROUP_ID_COLUMN],
    )
    val group: GroupBean?,
    @Relation(
        parentColumns = [FeedBean.URL_COLUMN],
        entityColumns = [FeedBean.URL_COLUMN],
    )
    val feed: FeedViewBean?,
)
