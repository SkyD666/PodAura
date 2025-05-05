package com.skyd.podaura.model.repository.importexport.opml

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupVo

data class OpmlGroupWithFeed(
    val group: GroupVo,
    val feeds: MutableList<FeedBean>,
)