package com.skyd.podaura.podnya.model.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.skyd.fundation.di.get

actual fun conversationDatabasePath(): String =
    get<Context>().getDatabasePath(CONVERSATION_DATABASE_FILE_NAME).canonicalPath

actual fun ConversationDatabase.Companion.builder(path: String): RoomDatabase.Builder<ConversationDatabase> =
    Room.databaseBuilder<ConversationDatabase>(context = get<Context>(), name = path)
        .setDriver(AndroidSQLiteDriver())
