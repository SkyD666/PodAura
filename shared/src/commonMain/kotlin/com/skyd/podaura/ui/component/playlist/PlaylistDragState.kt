package com.skyd.podaura.ui.component.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean

/** A drag is a local preview. Only releasing at a different position commits it. */
internal class PlaylistDragState {
    var preview: List<PlaylistMediaWithArticleBean?>? by mutableStateOf(null)
        private set
    var saving by mutableStateOf(false)
        private set
    private var original: List<PlaylistMediaWithArticleBean?> = emptyList()
    private var draggedUrl: String? = null

    fun start(items: List<PlaylistMediaWithArticleBean?>, url: String) {
        if (saving) return
        original = items.toList()
        preview = original
        draggedUrl = url
    }

    fun move(from: Int, to: Int, loaded: List<PlaylistMediaWithArticleBean?>? = null) {
        val current = preview?.toMutableList() ?: return
        if (saving || from !in current.indices || to !in current.indices) return
        // Fill unloaded slots only if the known rows still have their original indices.
        // A same-size refresh can otherwise insert a moved row twice into the preview.
        if (loaded != null && loaded.size == original.size && original.indices.all { index ->
                original[index] == null || original[index]?.playlistMediaBean?.stableUrl ==
                        loaded[index]?.playlistMediaBean?.stableUrl
            }
        ) {
            original = original.mapIndexed { index, item -> item ?: loaded[index] }
            current.indices.forEach { if (current[it] == null) current[it] = loaded[it] }
        }
        if (current[from] == null || current[to] == null) return
        current.add(to, current.removeAt(from))
        preview = current
    }

    fun step(items: List<PlaylistMediaWithArticleBean?>, url: String, offset: Int): Boolean {
        if (saving || preview != null) return false
        val index = items.indexOfFirst { it?.playlistMediaBean?.stableUrl == url }
        if (index < 0 || items.getOrNull(index + offset) == null) return false
        start(items, url)
        move(index, index + offset)
        return true
    }

    fun cancel() {
        if (saving) return
        preview = null
        original = emptyList()
        draggedUrl = null
    }

    suspend fun finish(
        onMove: suspend (from: String, to: String, original: List<PlaylistMediaWithArticleBean?>) -> Unit,
    ) {
        if (saving) return
        val url = draggedUrl ?: return
        val from = original.indexOfFirst { it?.playlistMediaBean?.stableUrl == url }
        val to = preview?.indexOfFirst { it?.playlistMediaBean?.stableUrl == url } ?: -1
        if (from < 0 || to < 0 || from == to) {
            cancel()
            return
        }
        val target = original[to]?.playlistMediaBean?.stableUrl ?: run { cancel(); return }
        saving = true
        try {
            onMove(url, target, original)
        } finally {
            saving = false
            cancel()
        }
    }
}
