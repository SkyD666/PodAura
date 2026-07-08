package com.skyd.podaura.model.db.converter

import androidx.room3.ColumnTypeConverter
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.serialization.json.Json

class RequestHeadersConverter {
    @ColumnTypeConverter
    fun fromString(string: String?): FeedBean.RequestHeaders? {
        string ?: return null
        return FeedBean.RequestHeaders(headers = Json.decodeFromString(string))
    }

    @ColumnTypeConverter
    fun headersToString(headers: FeedBean.RequestHeaders?): String? {
        val list = headers?.headers ?: return null
        return Json.encodeToString(list)
    }
}
