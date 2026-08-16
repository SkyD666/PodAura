package com.skyd.podaura.model.repository.translation

import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopCredentialStoreTest {
    @Test
    fun macOsKeychainSupportsCreateUpdateReadAndDelete() = runTest {
        if (!System.getProperty("os.name").contains("mac", ignoreCase = true)) return@runTest

        val store = DesktopCredentialStore()
        val id = "test-${UUID.randomUUID()}"
        val initialSecret = "initial-${UUID.randomUUID()}"
        val updatedSecret = "updated-${UUID.randomUUID()}"

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
