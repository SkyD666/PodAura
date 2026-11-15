package com.skyd.podaura.podnya.model.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

const val CONVERSATION_TABLE_NAME = "Conversation"

@Entity(
    tableName = CONVERSATION_TABLE_NAME,
    primaryKeys = [ConversationRecord.CONVERSATION_ID_COLUMN],
    indices = [Index(value = [ConversationRecord.ACTIVITY_AT_COLUMN, ConversationRecord.CREATED_AT_COLUMN, ConversationRecord.CONVERSATION_ID_COLUMN])],
)
data class ConversationRecord(
    @ColumnInfo(name = CONVERSATION_ID_COLUMN) val conversationId: String,
    @ColumnInfo(name = TITLE_COLUMN) val title: String = "",
    @ColumnInfo(name = MANUALLY_NAMED_COLUMN) val manuallyNamed: Boolean = false,
    @ColumnInfo(name = CREATED_AT_COLUMN) val createdAt: Long = 0,
    @ColumnInfo(name = ACTIVITY_AT_COLUMN) val activityAt: Long = 0,
    @ColumnInfo(name = DRAFT_COLUMN) val draft: String = "",
    @ColumnInfo(name = EDITOR_ID_COLUMN) val editorId: String = "",
    @ColumnInfo(name = DRAFT_REVISION_COLUMN) val draftRevision: Long = 0,
    @ColumnInfo(name = SELECTION_START_COLUMN) val selectionStart: Int = 0,
    @ColumnInfo(name = SELECTION_END_COLUMN) val selectionEnd: Int = 0,
    @ColumnInfo(name = CONTEXT_COLUMN) val context: String = "",
    @ColumnInfo(name = MODEL_CONFIG_ID_COLUMN) val modelConfigId: String? = null,
    @ColumnInfo(name = DESTINATIONS_COLUMN) val destinations: String = "[]",
    @ColumnInfo(name = POSITION_COLUMN) val position: String = "",
    @ColumnInfo(name = LAST_TURN_ID_COLUMN) val lastTurnId: Long = 0,
    @ColumnInfo(name = LAST_STATUS_COLUMN) val lastStatus: String? = null,
    @ColumnInfo(name = RETAINED_COLUMN) val retained: Boolean = false,
) {
    companion object {
        const val CONVERSATION_ID_COLUMN = "conversationId"
        const val TITLE_COLUMN = "title"
        const val MANUALLY_NAMED_COLUMN = "manuallyNamed"
        const val CREATED_AT_COLUMN = "createdAt"
        const val ACTIVITY_AT_COLUMN = "activityAt"
        const val DRAFT_COLUMN = "draft"
        const val EDITOR_ID_COLUMN = "editorId"
        const val DRAFT_REVISION_COLUMN = "draftRevision"
        const val SELECTION_START_COLUMN = "selectionStart"
        const val SELECTION_END_COLUMN = "selectionEnd"
        const val CONTEXT_COLUMN = "context"
        const val MODEL_CONFIG_ID_COLUMN = "modelConfigId"
        const val DESTINATIONS_COLUMN = "destinations"
        const val POSITION_COLUMN = "position"
        const val LAST_TURN_ID_COLUMN = "lastTurnId"
        const val LAST_STATUS_COLUMN = "lastStatus"
        const val RETAINED_COLUMN = "retained"
    }
}

const val CONVERSATION_TURN_TABLE_NAME = "ConversationTurn"

@Entity(
    tableName = CONVERSATION_TURN_TABLE_NAME,
    primaryKeys = [ConversationTurnRecord.CONVERSATION_ID_COLUMN, ConversationTurnRecord.TURN_ID_COLUMN],
    foreignKeys = [ForeignKey(
        entity = ConversationRecord::class,
        parentColumns = [ConversationRecord.CONVERSATION_ID_COLUMN],
        childColumns = [ConversationTurnRecord.CONVERSATION_ID_COLUMN],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = [ConversationTurnRecord.CONVERSATION_ID_COLUMN, ConversationTurnRecord.TURN_ID_COLUMN])],
)
data class ConversationTurnRecord(
    @ColumnInfo(name = CONVERSATION_ID_COLUMN) val conversationId: String,
    @ColumnInfo(name = TURN_ID_COLUMN) val turnId: Long,
    @ColumnInfo(name = QUESTION_COLUMN) val question: String,
    @ColumnInfo(name = CREATED_AT_COLUMN) val createdAt: Long,
    @ColumnInfo(name = SOURCE_COLUMN) val source: String? = null,
    @ColumnInfo(name = REPLY_COLUMN) val reply: String = "",
    @ColumnInfo(name = CONTENT_COLUMN) val content: String? = null,
    @ColumnInfo(name = CONTEXT_COLUMN) val context: String = "",
    @ColumnInfo(name = STATUS_COLUMN) val status: String = "RUNNING",
    @ColumnInfo(name = ERROR_COLUMN) val error: String? = null,
) {
    companion object {
        const val CONVERSATION_ID_COLUMN = "conversationId"
        const val TURN_ID_COLUMN = "turnId"
        const val QUESTION_COLUMN = "question"
        const val CREATED_AT_COLUMN = "createdAt"
        const val SOURCE_COLUMN = "source"
        const val REPLY_COLUMN = "reply"
        const val CONTENT_COLUMN = "content"
        const val CONTEXT_COLUMN = "context"
        const val STATUS_COLUMN = "status"
        const val ERROR_COLUMN = "error"
    }
}

const val CONVERSATION_WORKSPACE_TABLE_NAME = "ConversationWorkspace"

@Entity(
    tableName = CONVERSATION_WORKSPACE_TABLE_NAME,
    primaryKeys = [ConversationWorkspaceRecord.WORKSPACE_ID_COLUMN],
)
data class ConversationWorkspaceRecord(
    @ColumnInfo(name = WORKSPACE_ID_COLUMN) val workspaceId: Int = 1,
    @ColumnInfo(name = SELECTED_CONVERSATION_ID_COLUMN) val selectedConversationId: String? = null,
    @ColumnInfo(name = LAST_MODEL_CONFIG_ID_COLUMN) val lastModelConfigId: String? = null,
) {
    companion object {
        const val WORKSPACE_ID_COLUMN = "workspaceId"
        const val SELECTED_CONVERSATION_ID_COLUMN = "selectedConversationId"
        const val LAST_MODEL_CONFIG_ID_COLUMN = "lastModelConfigId"
    }
}

