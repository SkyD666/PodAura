package com.skyd.fundation.credential

import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DesktopCredentialStoreTest {
    @Test
    fun windowsRejectsOversizedUtf8BlobsBeforeAccessingTheSystemStore() {
        assertFailsWith<CredentialTooLongException> {
            WindowsCredentialStore.put(id = "oversize-test", secret = "é".repeat(1280) + "x")
        }
    }

    @Test
    fun nativeCredentialStoreSupportsCreateUpdateReadAndDelete() = runBlocking {
        val platform = System.getProperty("os.name").lowercase()
        if (!platform.contains("mac") && !platform.startsWith("windows")) return@runBlocking

        val store = DesktopCredentialStore()
        val id = "test-${UUID.randomUUID()}"
        val initialSecret = "initial-${UUID.randomUUID()}"
        val updatedSecret = "é".repeat(1280)

        try {
            assertNull(store.get(id))
            store.put(id, initialSecret)
            assertEquals(initialSecret, store.get(id))
            store.put(id, updatedSecret)
            assertEquals(updatedSecret, store.get(id))
        } finally {
            store.delete(id)
        }
        assertNull(store.get(id))
    }
}
