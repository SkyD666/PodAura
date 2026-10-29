package com.skyd.podaura.model.bean.playlist

/** Moves identities, so stale indices cannot move a different item after a deletion. */
internal fun movePlaylistItem(order: List<String>, from: String, to: String): List<String> {
    val fromIndex = order.indexOf(from)
    val toIndex = order.indexOf(to)
    require(fromIndex >= 0 && toIndex >= 0) { "The playlist changed during the drag" }
    return order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

/** Only shared items exchange slots; additions and removals are never propagated by a reorder. */
internal fun mergePlaylistOrder(existing: List<String>, ordered: List<String>): List<String> {
    val existingSet = existing.toSet()
    val shared = ordered.filter { it in existingSet }.distinct()
    val sharedSet = shared.toSet()
    val iterator = shared.iterator()
    return existing.map { if (it in sharedSet) iterator.next() else it }
}
