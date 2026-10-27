package com.skyd.podaura.model.bean.group

data class GroupWithFeedCount(
    val group: GroupVo,
    val feedCount: Int,
    val mutedFeedCount: Int,
)
