package com.skyd.podaura.model.bean.group

import androidx.room3.Embedded
import androidx.room3.Relation
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean

/**
 * A [group] contains many [feeds].
 */
data class GroupWithFeedBean(
    @Embedded
    var group: GroupBean,
    @Relation(
        parentColumns = [GroupBean.GROUP_ID_COLUMN],
        entityColumns = [FeedBean.GROUP_ID_COLUMN],
    )
    var feeds: List<FeedViewBean>,
)
