package com.skyd.podaura.ui.player

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class PlayerLaunchDataTest {
    @Test
    fun loadCommandPreservesEveryFieldWithoutAcquiringMediaAccess() {
        val batch = ExternalMediaBatch(listOf(ExternalMedia("source", "fd://10")), emptyList())
        val playlist = listOf(PlaylistMediaWithArticleBean.fromUrl("", "fd://10", 0.0))
        val data = PlayerLaunchData("fd://10", playlist, 42L, "request", batch)

        val command = data.toLoadCommand()

        assertEquals(PlayerCommand.LoadList(playlist, "fd://10", 42L, "request", batch), command)
        assertSame(playlist, command.playlist)
        assertSame(batch, command.externalBatch)
        batch.release()
        assertFalse(batch.retain())
    }
}
