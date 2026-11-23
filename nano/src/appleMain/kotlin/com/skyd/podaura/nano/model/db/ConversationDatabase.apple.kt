package com.skyd.podaura.nano.model.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import com.skyd.fundation.util.joinPath

actual fun conversationDatabasePath(): String =
    joinPath(Const.DB_DIR, CONVERSATION_DATABASE_FILE_NAME)

actual fun ConversationDatabase.Companion.builder(path: String): RoomDatabase.Builder<ConversationDatabase> =
    Room.databaseBuilder<ConversationDatabase>(name = path).setDriver(BundledSQLiteDriver())
