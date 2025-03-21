package com.skyd.anivu.model.db.converter

import androidx.room.TypeConverter
import com.skyd.anivu.model.bean.feed.FeedBean
import kotlinx.serialization.json.Json

class RequestHeadersConverter {
    @TypeConverter
    fun fromString(string: String?): FeedBean.RequestHeaders? {
        string ?: return null
        return FeedBean.RequestHeaders(headers = Json.decodeFromString(string))
    }

    @TypeConverter
    fun headersToString(headers: FeedBean.RequestHeaders?): String? {
        val list = headers?.headers ?: return null
        return Json.encodeToString(list)
    }
}