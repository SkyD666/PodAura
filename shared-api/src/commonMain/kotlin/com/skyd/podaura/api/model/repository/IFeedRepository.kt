package com.skyd.podaura.api.model.repository

import com.skyd.podaura.api.model.bean.FeedQuery
import com.skyd.podaura.api.model.bean.FeedRecord

/** Read saved subscriptions without fetching or changing them. */
interface IFeedRepository {
    suspend fun query(query: FeedQuery): List<FeedRecord>
}
