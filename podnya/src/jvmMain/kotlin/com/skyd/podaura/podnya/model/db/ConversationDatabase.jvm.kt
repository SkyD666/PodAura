package com.skyd.podaura.podnya.model.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import java.io.File

actual fun conversationDatabasePath(): String =
    File(Const.DB_DIR, CONVERSATION_DATABASE_FILE_NAME).canonicalPath

actual fun ConversationDatabase.Companion.builder(path: String): RoomDatabase.Builder<ConversationDatabase> =
    Room.databaseBuilder<ConversationDatabase>(name = path).setDriver(BundledSQLiteDriver())
