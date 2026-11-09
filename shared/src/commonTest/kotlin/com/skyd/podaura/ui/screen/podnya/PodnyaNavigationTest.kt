package com.skyd.podaura.ui.screen.podnya

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

class PodnyaNavigationTest {
    @Test
    fun onlyChatUsesTheMiniPlayer() {
        val entries = entryProvider<NavKey> { podnyaEntries() }
        assertTrue(entries(PodnyaRoute).metadata.hasMiniPlayerMetadata())
        assertFalse(entries(AssistantSettingsRoute).metadata.hasMiniPlayerMetadata())
    }

    @Test
    fun savedKeysFromBeforeTheModuleMoveStillRestore() {
        val json = Json { serializersModule = PodAuraSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val keys = mapOf<String, NavKey>(
            "PodnyaRoute" to PodnyaRoute,
            "AssistantSettingsRoute" to AssistantSettingsRoute,
        )
        for ((name, key) in keys) {
            val saved = """{"type":"com.skyd.podaura.feature.podnya.integration.$name"}"""
            assertEquals(key, json.decodeFromString(serializer, saved))
            assertEquals(saved, json.encodeToString(serializer, key))
        }
    }
}
