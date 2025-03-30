package com.skyd.anivu.model.repository.importexport.opml

import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.group.GroupVo

data class OpmlGroupWithFeed(
    val group: GroupVo,
    val feeds: MutableList<FeedBean>,
)