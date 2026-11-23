package com.skyd.podaura.ui.screen.nano

import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import androidx.navigation3.runtime.entryProvider
import com.skyd.podaura.ui.player.mini.hasMiniPlayerMetadata
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoNavigationTest {
    @Test
    fun onlyChatUsesTheMiniPlayer() {
        val entries = entryProvider<NavKey> { nanoEntries() }
        assertTrue(entries(NanoRoute).metadata.hasMiniPlayerMetadata())
        assertFalse(entries(AssistantSettingsRoute).metadata.hasMiniPlayerMetadata())
    }

    @Test
    fun routeKeysRoundTripWithShortNames() {
        val json = Json { serializersModule = PodAuraSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val keys = mapOf<String, NavKey>(
            "NanoRoute" to NanoRoute,
            "AssistantSettingsRoute" to AssistantSettingsRoute,
        )
        for ((name, key) in keys) {
            val saved = """{"type":"$name"}"""
            assertEquals(key, json.decodeFromString(serializer, saved))
            assertEquals(saved, json.encodeToString(serializer, key))
        }
    }
}
