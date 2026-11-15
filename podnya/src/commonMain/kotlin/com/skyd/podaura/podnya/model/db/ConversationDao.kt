package com.skyd.podaura.podnya.model.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.ACTIVITY_AT_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.CONVERSATION_ID_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.CREATED_AT_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.LAST_STATUS_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.RETAINED_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationRecord.Companion.TITLE_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationTurnRecord.Companion.ERROR_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationTurnRecord.Companion.STATUS_COLUMN
import com.skyd.podaura.podnya.model.db.ConversationTurnRecord.Companion.TURN_ID_COLUMN

/** List projections intentionally exclude drafts, contexts, reply bodies and cards. */
data class ConversationSummaryRecord(
    val conversationId: String,
    val title: String,
    val createdAt: Long,
    val activityAt: Long,
    val lastStatus: String?,
    val retained: Boolean,
)

@Dao
abstract class ConversationDao {
    @Query("SELECT $CONVERSATION_ID_COLUMN, $TITLE_COLUMN, $CREATED_AT_COLUMN, $ACTIVITY_AT_COLUMN, $LAST_STATUS_COLUMN, $RETAINED_COLUMN FROM $CONVERSATION_TABLE_NAME WHERE $RETAINED_COLUMN = 1 ORDER BY $ACTIVITY_AT_COLUMN DESC, $CREATED_AT_COLUMN DESC, $CONVERSATION_ID_COLUMN ASC LIMIT :limit OFFSET :offset")
    abstract suspend fun summaries(offset: Int, limit: Int): List<ConversationSummaryRecord>

    @Query("SELECT * FROM $CONVERSATION_TABLE_NAME WHERE $CONVERSATION_ID_COLUMN = :id")
    abstract suspend fun metadata(id: String): ConversationRecord?

    @Query("SELECT * FROM $CONVERSATION_TABLE_NAME WHERE $RETAINED_COLUMN = 0 LIMIT 1")
    abstract suspend fun blank(): ConversationRecord?

    @Query("SELECT * FROM $CONVERSATION_WORKSPACE_TABLE_NAME LIMIT 1")
    abstract suspend fun workspace(): ConversationWorkspaceRecord?

    @Query("SELECT * FROM $CONVERSATION_TURN_TABLE_NAME WHERE $CONVERSATION_ID_COLUMN = :id AND $TURN_ID_COLUMN <= :anchor ORDER BY $TURN_ID_COLUMN DESC LIMIT :limit")
    abstract suspend fun before(id: String, anchor: Long, limit: Int): List<ConversationTurnRecord>

    @Query("SELECT * FROM $CONVERSATION_TURN_TABLE_NAME WHERE $CONVERSATION_ID_COLUMN = :id AND $TURN_ID_COLUMN > :anchor ORDER BY $TURN_ID_COLUMN ASC LIMIT :limit")
    abstract suspend fun after(id: String, anchor: Long, limit: Int): List<ConversationTurnRecord>

    @Upsert
    abstract suspend fun putMetadata(record: ConversationRecord)

    @Upsert
    abstract suspend fun putTurn(record: ConversationTurnRecord)

    @Upsert
    abstract suspend fun putWorkspace(record: ConversationWorkspaceRecord)

    @Query("DELETE FROM $CONVERSATION_TABLE_NAME WHERE $CONVERSATION_ID_COLUMN = :id")
    abstract suspend fun remove(id: String)

    @Query("UPDATE $CONVERSATION_TURN_TABLE_NAME SET $STATUS_COLUMN = 'INTERRUPTED', $ERROR_COLUMN = 'STREAM_INTERRUPTED' WHERE $STATUS_COLUMN = 'RUNNING'")
    abstract suspend fun interruptTurns()

    @Query("UPDATE $CONVERSATION_TABLE_NAME SET $LAST_STATUS_COLUMN = 'INTERRUPTED' WHERE $LAST_STATUS_COLUMN = 'RUNNING'")
    abstract suspend fun interruptSummaries()

    @Transaction
    open suspend fun recover() {
        interruptTurns()
        interruptSummaries()
    }

    /** Admission and terminal writes share this atomic boundary with metadata and workspace. */
    @Transaction
    open suspend fun commit(
        metadata: ConversationRecord?,
        turn: ConversationTurnRecord?,
        workspace: ConversationWorkspaceRecord?,
    ) {
        metadata?.let { putMetadata(it) }
        turn?.let { putTurn(it) }
        workspace?.let { putWorkspace(it) }
    }

    @Transaction
    open suspend fun delete(
        id: String,
        workspace: ConversationWorkspaceRecord,
        replacement: ConversationRecord?
    ) {
        remove(id)
        replacement?.let { putMetadata(it) }
        putWorkspace(workspace)
    }
}
