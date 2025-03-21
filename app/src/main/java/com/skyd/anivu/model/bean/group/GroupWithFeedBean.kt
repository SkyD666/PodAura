package com.skyd.anivu.model.bean.group

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean

/**
 * A [group] contains many [feeds].
 */
data class GroupWithFeedBean(
    @Embedded
    var group: GroupBean,
    @Relation(
        parentColumn = GroupBean.GROUP_ID_COLUMN,
        entityColumn = FeedBean.GROUP_ID_COLUMN,
    )
    var feeds: List<FeedViewBean>,
)
