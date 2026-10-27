package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.mpv.MPV
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Use mpv's active binding, including input.conf overrides, rather than hardcoding Space. */
internal fun MPV.playbackCommandForKey(key: String): PlayerCommand? {
    val bindings = getPropertyString("input-bindings") ?: return null
    val binding = Json.parseToJsonElement(bindings).jsonArray.asSequence()
        .map { it.jsonObject }
        .filter { it["key"]?.jsonPrimitive?.content == key }
        .filter { (it["priority"]?.jsonPrimitive?.int ?: -1) >= 0 }
        .maxByOrNull { it.getValue("priority").jsonPrimitive.int }
        ?: return null
    return when (binding["cmd"]?.jsonPrimitive?.content) {
        "cycle pause" -> PlayerCommand.PlayOrPause
        "set pause no" -> PlayerCommand.Paused(false)
        "set pause yes" -> PlayerCommand.Paused(true)
        "playlist-next" -> PlayerCommand.NextMedia
        "playlist-prev" -> PlayerCommand.PreviousMedia
        else -> null
    }
}
