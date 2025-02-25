package com.skyd.anivu.ext

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap

fun <K, V> snapshotStateMapSaver() = Saver<SnapshotStateMap<K, V>, Any>(
    save = { state -> state.toList() },
    restore = { value ->
        @Suppress("UNCHECKED_CAST")
        (value as? List<Pair<K, V>>)?.toMutableStateMap() ?: mutableStateMapOf()
    }
)

fun calculateHashMapInitialCapacity(
    initialCapacity: Int,
    loadFactor: Float = 0.75f,
): Int = (initialCapacity / loadFactor).toInt() + 1

fun List<String>.longestCommonPrefix(): String {
    if (isEmpty()) return ""

    val list = sorted()
    val first = list.first()
    val last = list.last()

    val minLen = minOf(first.length, last.length)
    for (i in 0 until minLen) {
        if (first[i] != last[i]) {
            return first.substring(0, i)
        }
    }
    return first.substring(0, minLen)
}