package com.skyd.anivu.ext

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

fun NavController.popBackStackWithLifecycle() {
    if (currentBackStackEntry?.lifecycleIsResumed() == true) {
        popBackStack()
    }
}

inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : CustomNavType<T>(
    type = T::class,
    isNullableAllowed = isNullableAllowed,
) {
    override fun get(bundle: Bundle, key: String) =
        bundle.getString(key)?.toDecodedUrl()?.let<String, T>(json::decodeFromString)

    override fun parseValue(value: String): T = json.decodeFromString(value.toDecodedUrl())

    override fun serializeAsValue(value: T): String =
        json.encodeToString(value).toEncodedUrl(allow = null)

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, json.encodeToString(value).toEncodedUrl(allow = null))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CustomNavType<*>
        return type == that.type
    }
}

inline fun <reified T : Any> listType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = serializableType<List<T>>(isNullableAllowed, json)

abstract class CustomNavType<T : Any>(
    val type: KClass<T>,
    isNullableAllowed: Boolean = false,
) : NavType<T>(isNullableAllowed)