package com.skyd.podaura.nano.model.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

const val CONVERSATION_DATABASE_FILE_NAME = "nanoConversation.db"

@Suppress("KotlinNoActualForExpect")
expect object ConversationDatabaseConstructor : RoomDatabaseConstructor<ConversationDatabase> {
    override fun initialize(): ConversationDatabase
}

@Database(
    entities = [ConversationRecord::class, ConversationTurnRecord::class, ConversationWorkspaceRecord::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ConversationDatabaseConstructor::class)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object
}

expect fun conversationDatabasePath(): String
expect fun ConversationDatabase.Companion.builder(path: String): RoomDatabase.Builder<ConversationDatabase>
