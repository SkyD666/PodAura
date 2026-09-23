package com.skyd.podaura.ui.notification

import kotlinx.serialization.json.Json
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

internal const val NOTIFICATION_URI_PREFIX = "podaura://notification/"

/** Only opaque UUIDs cross the OS activation boundary; article lists stay in the app data folder. */
internal fun notificationIdFromUri(value: String): String? = runCatching {
    val uri = URI(value)
    if (uri.scheme != "podaura" || uri.host != "notification" || uri.port != -1 ||
        uri.userInfo != null || uri.query != null || uri.fragment != null
    ) return null
    val id = uri.path.removePrefix("/")
    id.takeIf { UUID.fromString(it).toString() == it }
}.getOrNull()

internal class DesktopNotificationStore(private val directory: Path) {
    fun save(articleIds: List<String>): String {
        require(articleIds.isNotEmpty())
        val ids = articleIds.distinct()
        require(ids.size <= MAX_ARTICLES && ids.all { UUID.fromString(it).toString() == it })
        Files.createDirectories(directory)
        val id = UUID.randomUUID().toString()
        val temporary = Files.createTempFile(directory, ".pending-", ".json")
        try {
            Files.writeString(temporary, Json.encodeToString(ids))
            Files.move(temporary, directory.resolve("$id.json"), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return id
    }

    fun read(activationUri: String): List<String>? {
        val id = notificationIdFromUri(activationUri) ?: return null
        val file = directory.resolve("$id.json")
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_PAYLOAD_BYTES) return null
        return runCatching {
            Json.decodeFromString<List<String>>(Files.readString(file)).takeIf { ids ->
                ids.isNotEmpty() && ids.size <= MAX_ARTICLES &&
                    ids.all { UUID.fromString(it).toString() == it }
            }
        }.getOrNull()
    }

    fun remove(id: String) {
        require(UUID.fromString(id).toString() == id)
        Files.deleteIfExists(directory.resolve("$id.json"))
    }

    private companion object {
        const val MAX_ARTICLES = 5_000
        const val MAX_PAYLOAD_BYTES = 256_000L
    }
}
