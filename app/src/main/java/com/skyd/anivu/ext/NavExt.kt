package com.skyd.anivu.ext

import android.os.Bundle
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import com.skyd.anivu.ext.UuidListType.Companion.decodeUuidList
import com.skyd.anivu.ext.UuidListType.Companion.encodeUuidList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.UUID

fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

fun NavController.popBackStackWithLifecycle(): Boolean {
    if (currentBackStackEntry?.lifecycleIsResumed() == true) {
        return popBackStack()
    }
    return true
}

inline fun <reified T> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = if (isNullableAllowed) {
    object : NavType<T?>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.toDecodedUrl()?.let<String, T?>(::parseValue)

        override fun parseValue(value: String): T? {
            if (value == "null") return null
            return json.decodeFromString(value.toDecodedUrl())
        }

        override fun serializeAsValue(value: T?): String =
            value?.let { json.encodeToString(value).toEncodedUrl(allow = null) } ?: "null"


        override fun put(bundle: Bundle, key: String, value: T?) {
            bundle.putString(key, serializeAsValue(value))
        }
    }
} else {
    object : NavType<T>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.toDecodedUrl()?.let<String, T>(::parseValue)

        override fun parseValue(value: String): T = json.decodeFromString(value.toDecodedUrl())

        override fun serializeAsValue(value: T): String =
            json.encodeToString(value).toEncodedUrl(allow = null)


        override fun put(bundle: Bundle, key: String, value: T) {
            bundle.putString(key, serializeAsValue(value))
        }
    }
}

inline fun <reified T> listType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = if (isNullableAllowed) {
    serializableType<List<T>?>(true, json)
} else {
    serializableType<List<T>>(false, json)
}

@Serializable
data class UuidList(val uuids: List<String>)

abstract class UuidListType<T>(
    isNullableAllowed: Boolean = false,
) : NavType<T>(isNullableAllowed) {
    companion object {
        fun encodeUuidList(uuidList: List<UUID>): String {
            val totalBytes = ByteArray(16 * uuidList.size)
            val buf = ByteBuffer.wrap(totalBytes)
            for (u in uuidList) {
                buf.putLong(u.mostSignificantBits)
                buf.putLong(u.leastSignificantBits)
            }
            return Base64.encodeToString(totalBytes, Base64.NO_WRAP or Base64.URL_SAFE)
        }

        fun decodeUuidList(uuidListString: String): List<UUID> {
            val bytes: ByteArray = Base64.decode(uuidListString, Base64.NO_WRAP or Base64.URL_SAFE)
            val uuids = mutableListOf<UUID>()
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.remaining() >= 16) {
                val msb = buffer.long
                val lsb = buffer.long
                uuids += UUID(msb, lsb)
            }
            return uuids
        }
    }
}

fun uuidListType(
    isNullableAllowed: Boolean = false,
) = if (isNullableAllowed) {
    object : UuidListType<UuidList?>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.let<String, UuidList?>(::parseValue)

        override fun parseValue(value: String): UuidList? {
            if (value == "null") return null
            return UuidList(decodeUuidList(value).map { it.toString() })
        }

        override fun serializeAsValue(value: UuidList?) =
            value?.let { encodeUuidList(value.uuids.map { UUID.fromString(it) }) } ?: "null"

        override fun put(bundle: Bundle, key: String, value: UuidList?) {
            bundle.putString(key, serializeAsValue(value))
        }
    }
} else {
    object : NavType<UuidList>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.let<String, UuidList>(::parseValue)

        override fun parseValue(value: String) =
            UuidList(decodeUuidList(value).map { it.toString() })

        override fun serializeAsValue(value: UuidList) =
            encodeUuidList(value.uuids.map { UUID.fromString(it) })

        override fun put(bundle: Bundle, key: String, value: UuidList) {
            bundle.putString(key, serializeAsValue(value))
        }
    }
}