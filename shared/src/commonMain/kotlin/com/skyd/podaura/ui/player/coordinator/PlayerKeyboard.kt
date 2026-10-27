package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.mpv.MPV
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val OSD_COMMAND_PREFIXES = setOf("no-osd", "osd-auto", "osd-bar", "osd-msg", "osd-msg-bar")

/** Use mpv's active binding, including input.conf overrides, rather than hardcoding Space. */
internal fun MPV.playbackCommandForKey(key: String): PlayerCommand? {
    val bindings = getPropertyString("input-bindings") ?: return null
    val binding = Json.parseToJsonElement(bindings).jsonArray.asSequence()
        .map { it.jsonObject }
        .filter { it["key"]?.jsonPrimitive?.content == key }
        .filter { (it["priority"]?.jsonPrimitive?.int ?: -1) >= 0 }
        .maxByOrNull { it.getValue("priority").jsonPrimitive.int }
        ?: return null
    val command = binding["cmd"]?.jsonPrimitive?.content?.trim()?.split(Regex("\\s+"))
        ?.dropWhile { it in OSD_COMMAND_PREFIXES }?.joinToString(" ")
    return when (command) {
        "cycle pause" -> PlayerCommand.PlayOrPause
        "set pause no" -> PlayerCommand.Paused(false)
        "set pause yes" -> PlayerCommand.Paused(true)
        "playlist-next" -> PlayerCommand.NextMedia
        "playlist-prev" -> PlayerCommand.PreviousMedia
        else -> null
    }
}
