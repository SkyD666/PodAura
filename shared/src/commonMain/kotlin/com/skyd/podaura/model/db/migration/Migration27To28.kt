package com.skyd.podaura.model.db.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration27To28 : Migration(27, 28) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `TranslationProfile` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `providerType` TEXT NOT NULL,
                `endpoint` TEXT,
                `credentialId` TEXT,
                `customHeadersJson` TEXT NOT NULL,
                `requestTimeoutMillis` INTEGER NOT NULL,
                `enabled` INTEGER NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `targetLanguage` TEXT NOT NULL,
                `providerConfigJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
