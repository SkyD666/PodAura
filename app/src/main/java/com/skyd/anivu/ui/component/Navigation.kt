package com.skyd.anivu.ui.component

import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.ExtendedPaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import com.skyd.anivu.ui.component.UuidListType.Companion.decodeUuidList
import com.skyd.anivu.ui.component.UuidListType.Companion.encodeUuidList
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.UUID


inline fun <reified T> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = if (isNullableAllowed) {
    object : NavType<T?>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.let<String, T?>(::parseValue)

        override fun parseValue(value: String): T? {
            if (value == "null") return null
            return json.decodeFromString(value.hexToByteArray().decodeToString())
        }

        override fun serializeAsValue(value: T?): String =
            value?.let { json.encodeToString(value).encodeToByteArray().toHexString() } ?: "null"


        override fun put(bundle: Bundle, key: String, value: T?) {
            bundle.putString(key, serializeAsValue(value))
        }
    }
} else {
    object : NavType<T>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.let<String, T>(::parseValue)

        override fun parseValue(value: String): T =
            json.decodeFromString(value.hexToByteArray().decodeToString())

        override fun serializeAsValue(value: T): String =
            json.encodeToString(value).encodeToByteArray().toHexString()


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
@Parcelize
data class UuidList(val uuids: List<String>) : Parcelable

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

val EnterTransition = fadeIn(animationSpec = tween(220, delayMillis = 30)) + scaleIn(
    animationSpec = tween(220, delayMillis = 30),
    initialScale = 0.92f,
)

val ExitTransition = fadeOut(animationSpec = tween(90))

val PopEnterTransition = fadeIn(animationSpec = tween(220)) + scaleIn(
    animationSpec = tween(220),
    initialScale = 0.92f,
)

val PopExitTransition = fadeOut(animationSpec = tween(220)) + scaleOut(
    animationSpec = tween(220),
    targetScale = 0.92f,
)

@Composable
fun PodAuraNavHost(
    navController: NavHostController,
    startDestination: Any,
    builder: NavGraphBuilder.() -> Unit
) = NavHost(
    modifier = Modifier.background(MaterialTheme.colorScheme.background),
    navController = navController,
    startDestination = startDestination,
    enterTransition = { EnterTransition },
    exitTransition = { ExitTransition },
    popEnterTransition = { PopEnterTransition },
    popExitTransition = { PopExitTransition },
    builder = builder,
)

@Composable
fun <S, T : PaneScaffoldValue<S>> ExtendedPaneScaffoldPaneScope<S, T>.PodAuraAnimatedPane(
    modifier: Modifier = Modifier,
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) = AnimatedPane(
    modifier = modifier,
    enterTransition = EnterTransition,
    exitTransition = ExitTransition,
    boundsAnimationSpec = boundsAnimationSpec,
    content = content,
)