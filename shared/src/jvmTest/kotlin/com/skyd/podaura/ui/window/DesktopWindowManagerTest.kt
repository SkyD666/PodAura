package com.skyd.podaura.ui.window

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWindowManagerTest {

    @Test
    fun startsWithOnlyMainWindow() {
        val manager = DesktopWindowManager()

        assertEquals(
            listOf(DesktopWindowId.Main),
            manager.windows.map { it.id },
        )
    }

    @Test
    fun openingPlayerTwiceKeepsOneWindowAndRequestsActivation() {
        val manager = DesktopWindowManager()

        manager.openOrActivate(DesktopWindowSpec.Player)
        val firstActivation = manager.windows.single { it.id == DesktopWindowId.Player }
            .activationToken

        manager.openOrActivate(DesktopWindowSpec.Player)
        val playerEntries = manager.windows.filter { it.id == DesktopWindowId.Player }

        assertEquals(1, playerEntries.size)
        assertTrue(playerEntries.single().activationToken > firstActivation)
    }

    @Test
    fun activatingWindowOnlyChangesThatWindowsToken() {
        val manager = DesktopWindowManager()
        manager.openOrActivate(DesktopWindowSpec.Player)
        val playerActivation = manager.windows.single { it.id == DesktopWindowId.Player }
            .activationToken
        val mainActivation = manager.windows.single { it.id == DesktopWindowId.Main }
            .activationToken

        manager.activate(DesktopWindowId.Main)

        val activatedMain = manager.windows.single { it.id == DesktopWindowId.Main }
            .activationToken
        assertTrue(activatedMain > mainActivation)
        assertEquals(
            playerActivation,
            manager.windows.single { it.id == DesktopWindowId.Player }.activationToken,
        )
    }

    @Test
    fun closingAndReopeningPlayerDoesNotAffectMainWindow() {
        val manager = DesktopWindowManager()
        manager.openOrActivate(DesktopWindowSpec.Player)

        manager.close(DesktopWindowId.Player)

        assertEquals(
            listOf(DesktopWindowId.Main),
            manager.windows.map { it.id },
        )

        manager.openOrActivate(DesktopWindowSpec.Player)

        assertEquals(
            listOf(DesktopWindowId.Main, DesktopWindowId.Player),
            manager.windows.map { it.id },
        )
    }

    @Test
    fun mainWindowCannotBeRemovedFromManager() {
        val manager = DesktopWindowManager()

        manager.close(DesktopWindowId.Main)

        assertEquals(
            listOf(DesktopWindowId.Main),
            manager.windows.map { it.id },
        )
    }
}
