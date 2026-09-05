package com.skyd.podaura.ui.activity.player

import android.content.Intent
import android.net.Uri
import com.skyd.podaura.ui.player.PlayerOpenRequest
import com.skyd.podaura.ui.player.jumper.PLAY_DATA_MODE_KEY
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile

internal fun Intent?.toPlayerOpenRequest(requestId: String): PlayerOpenRequest {
    if (this == null) return PlayerOpenRequest.Resume
    getStringExtra(PLAY_DATA_MODE_KEY)?.let {
        return PlayerOpenRequest.Media(PlayDataMode.decodeFromString(it), requestId)
    }

    val uris = linkedSetOf<Uri>()
    data?.let(uris::add)
    clipData?.let { clip ->
        for (index in 0 until clip.itemCount) {
            clip.getItemAt(index).uri?.let(uris::add)
        }
    }
    return if (uris.isEmpty()) PlayerOpenRequest.Resume
    else PlayerOpenRequest.Files(uris.map(::PlatformFile), requestId)
}
