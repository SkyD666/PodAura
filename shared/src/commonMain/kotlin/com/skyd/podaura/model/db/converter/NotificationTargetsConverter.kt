package com.skyd.podaura.model.db.converter

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

class NotificationTargetsConverter {
    @ColumnTypeConverter
    fun decode(value: String): List<String> = Json.decodeFromString(value)

    @ColumnTypeConverter
    fun encode(value: List<String>): String = Json.encodeToString(value)
}
