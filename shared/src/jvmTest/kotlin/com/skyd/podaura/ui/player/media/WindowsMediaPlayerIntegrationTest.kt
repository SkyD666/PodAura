package com.skyd.podaura.ui.player.media

import com.skyd.fundation.jna.windows.WindowsMediaPlayer
import com.skyd.fundation.jna.windows.WindowsMediaPlayerSession
import com.skyd.fundation.jna.windows.WindowsMediaWindowRegistration
import com.skyd.fundation.jna.windows.WindowsMediaType
import com.skyd.fundation.jna.windows.WindowsNowPlayingInfo
import com.skyd.fundation.jna.windows.WindowsPlaybackState
import com.skyd.fundation.jna.windows.WindowsRemoteCommandAvailability
import com.skyd.fundation.jna.windows.WindowsRemoteCommand
import com.skyd.fundation.jna.windows.WindowsTaskbarTooltips
import com.skyd.podaura.ui.window.initWindowsAppIdentity
import com.sun.jna.Native
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.ptr.PointerByReference
import java.awt.Canvas
import java.awt.Frame
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WindowsMediaPlayerIntegrationTest {
    @Test
    fun registersApplicationIdentityMetadataForUnpackagedJvmRuns() {
        if (!isWindowsX64()) return

        Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, APP_USER_MODEL_REGISTRY_KEY)
        Advapi32Util.registrySetExpandableStringValue(
            WinReg.HKEY_CURRENT_USER,
            APP_USER_MODEL_REGISTRY_KEY,
            "DisplayName",
            "Incorrect test value",
        )

        initWindowsAppIdentity()

        assertEquals(
            "PodAura",
            Advapi32Util.registryGetExpandableStringValue(
                WinReg.HKEY_CURRENT_USER,
                APP_USER_MODEL_REGISTRY_KEY,
                "DisplayName",
            ),
        )
        val applicationId = PointerByReference()
        val result = Shell32.INSTANCE.GetCurrentProcessExplicitAppUserModelID(applicationId)
        assertEquals(0, result.toInt())
        try {
            assertEquals(APP_USER_MODEL_ID, applicationId.value.getWideString(0))
        } finally {
            Ole32.INSTANCE.CoTaskMemFree(applicationId.value)
        }
    }

    @Test
    fun packagesWindowsMediaPlayerShimForX64() {
        if (!isWindowsX64()) return
        assertNotNull(
            javaClass.classLoader.getResource(
                "win32-x86-64/podaura_windows_media_player.dll"
            )
        )
    }

    @Test
    fun loadsAndReopensTheRealWindowsNativeSession() {
        if (!isWindowsX64()) return

        val session = WindowsMediaPlayer.openSession { true }
        session.update(
            info = nowPlayingInfo(),
            commandAvailability = commandAvailability(),
        )
        session.clear()
        session.close()

        WindowsMediaPlayer.openSession { true }.close()
    }

    @Test
    fun rollsBackWindowRegistrationWhenSmtcRejectsTheHwnd() {
        if (!isWindowsX64()) return

        var frame: Frame? = null
        var session: WindowsMediaPlayerSession? = null
        var registration: WindowsMediaWindowRegistration? = null
        try {
            SwingUtilities.invokeAndWait {
                val child = Canvas()
                val createdFrame = Frame("PodAura SMTC rollback integration test").apply {
                    add(child)
                    setSize(320, 180)
                    setLocation(-10_000, -10_000)
                    isVisible = true
                }
                frame = createdFrame
                val createdSession = WindowsMediaPlayer.openSession { true }
                session = createdSession
                val childWindowHandle = PointerValue.of(Native.getComponentPointer(child))

                repeat(2) {
                    assertFailsWith<IllegalStateException> {
                        createdSession.attachWindow(
                            windowHandle = childWindowHandle,
                            isMainWindow = true,
                            tooltips = tooltips(),
                        )
                    }
                }
                registration = createdSession.attachWindow(
                    windowHandle = PointerValue.of(Native.getWindowPointer(createdFrame)),
                    isMainWindow = true,
                    tooltips = tooltips(),
                )
            }
        } finally {
            SwingUtilities.invokeAndWait {
                registration?.close()
                session?.close()
                frame?.dispose()
            }
        }
    }

    @Test
    fun rejectsASecondMainWindowWithoutConsumingItsRegistration() {
        if (!isWindowsX64()) return

        var mainFrame: Frame? = null
        var secondaryFrame: Frame? = null
        var session: WindowsMediaPlayerSession? = null
        var mainRegistration: WindowsMediaWindowRegistration? = null
        var secondaryRegistration: WindowsMediaWindowRegistration? = null
        try {
            SwingUtilities.invokeAndWait {
                val createdMainFrame = Frame("PodAura main window integration test").apply {
                    setSize(320, 180)
                    setLocation(-10_000, -10_000)
                    isVisible = true
                }
                val createdSecondaryFrame = Frame("PodAura secondary window integration test").apply {
                    setSize(320, 180)
                    setLocation(-10_000, -10_000)
                    isVisible = true
                }
                mainFrame = createdMainFrame
                secondaryFrame = createdSecondaryFrame
                val createdSession = WindowsMediaPlayer.openSession { true }
                session = createdSession
                mainRegistration = createdSession.attachWindow(
                    windowHandle = PointerValue.of(Native.getWindowPointer(createdMainFrame)),
                    isMainWindow = true,
                    tooltips = tooltips(),
                )
                val secondaryWindowHandle = PointerValue.of(
                    Native.getWindowPointer(createdSecondaryFrame)
                )

                repeat(2) {
                    assertFailsWith<IllegalStateException> {
                        createdSession.attachWindow(
                            windowHandle = secondaryWindowHandle,
                            isMainWindow = true,
                            tooltips = tooltips(),
                        )
                    }
                }
                secondaryRegistration = createdSession.attachWindow(
                    windowHandle = secondaryWindowHandle,
                    isMainWindow = false,
                    tooltips = tooltips(),
                )
            }
        } finally {
            SwingUtilities.invokeAndWait {
                secondaryRegistration?.close()
                mainRegistration?.close()
                session?.close()
                secondaryFrame?.dispose()
                mainFrame?.dispose()
            }
        }
    }

    @Test
    fun attachesSmtcAndTaskbarControlsToARealTopLevelWindow() {
        if (!isWindowsX64()) return

        val expectedCommands = buildList {
            repeat(TASKBAR_CLICK_REPETITIONS) {
                add(WindowsRemoteCommand.Previous)
                add(WindowsRemoteCommand.TogglePlayPause)
                add(WindowsRemoteCommand.Next)
            }
        }
        val commands = Collections.synchronizedList(mutableListOf<WindowsRemoteCommand>())
        val commandsArrived = CountDownLatch(expectedCommands.size)
        var frame: Frame? = null
        var session: WindowsMediaPlayerSession? = null
        var registration: WindowsMediaWindowRegistration? = null
        SwingUtilities.invokeAndWait {
            val createdFrame = Frame("PodAura Windows media integration test").apply {
                setSize(320, 180)
                setLocation(-10_000, -10_000)
                isVisible = true
            }
            frame = createdFrame
            val createdSession = WindowsMediaPlayer.openSession { command ->
                commands += command
                commandsArrived.countDown()
                true
            }
            session = createdSession
            registration = createdSession.attachWindow(
                windowHandle = PointerValue.of(Native.getWindowPointer(createdFrame)),
                isMainWindow = true,
                tooltips = tooltips(),
            )
            createdSession.update(
                info = nowPlayingInfo(),
                commandAvailability = commandAvailability(enableQueueNavigation = true),
            )
            repeat(TASKBAR_CLICK_REPETITIONS) {
                sendTaskbarButton(createdFrame, PREVIOUS_BUTTON_ID)
                sendTaskbarButton(createdFrame, PLAY_PAUSE_BUTTON_ID)
                sendTaskbarButton(createdFrame, NEXT_BUTTON_ID)
            }
            registration?.updateTooltips(tooltips())
        }
        try {
            assertTrue(commandsArrived.await(5, TimeUnit.SECONDS))
            assertEquals(expectedCommands, commands)
        } finally {
            SwingUtilities.invokeAndWait {
                session?.clear()
                registration?.close()
                session?.close()
                frame?.dispose()
            }
        }
    }

    private fun isWindowsX64(): Boolean =
        System.getProperty("os.name").contains("windows", ignoreCase = true) &&
                System.getProperty("os.arch").lowercase() in setOf("amd64", "x86_64")

    private fun tooltips() = WindowsTaskbarTooltips(
        previous = "Previous",
        play = "Play",
        pause = "Pause",
        next = "Next",
    )

    private fun nowPlayingInfo() = WindowsNowPlayingInfo(
        title = "PodAura media integration test",
        artist = "PodAura",
        album = "System media controls",
        durationSeconds = 120.0,
        elapsedSeconds = 30.0,
        playbackRate = 1.0,
        defaultPlaybackRate = 1.0,
        queueIndex = 0,
        queueCount = 1,
        mediaType = WindowsMediaType.Audio,
        playbackState = WindowsPlaybackState.Playing,
        artwork = null,
    )

    private fun commandAvailability(
        enableQueueNavigation: Boolean = false,
    ) = WindowsRemoteCommandAvailability(
        canPlay = false,
        canPause = true,
        canTogglePlayPause = true,
        canGoPrevious = enableQueueNavigation,
        canGoNext = enableQueueNavigation,
        canChangePlaybackPosition = true,
    )

    private fun sendTaskbarButton(frame: Frame, buttonId: Int) {
        User32.INSTANCE.PostMessage(
            WinDef.HWND(Native.getWindowPointer(frame)),
            WINDOW_COMMAND,
            WinDef.WPARAM(
                (TASKBAR_BUTTON_CLICKED.toLong() shl 16) or buttonId.toLong()
            ),
            WinDef.LPARAM(0),
        )
    }

    private object PointerValue {
        fun of(pointer: com.sun.jna.Pointer): Long = com.sun.jna.Pointer.nativeValue(pointer)
    }

    private companion object {
        const val TASKBAR_BUTTON_CLICKED = 0x1800
        const val PREVIOUS_BUTTON_ID = 0x5001
        const val PLAY_PAUSE_BUTTON_ID = 0x5002
        const val NEXT_BUTTON_ID = 0x5003
        const val TASKBAR_CLICK_REPETITIONS = 20
        const val WINDOW_COMMAND = 0x0111
        const val APP_USER_MODEL_ID = "com.skyd.podaura"
        const val APP_USER_MODEL_REGISTRY_KEY =
            "Software\\Classes\\AppUserModelId\\$APP_USER_MODEL_ID"
    }
}
