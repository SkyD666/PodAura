package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import kotlin.uuid.Uuid

internal data class PendingConversationChange(
    val metadata: AssistantSessionState,
    val turn: AssistantSessionTurn?,
    val version: Long,
    val due: Long,
)

/** Repository-owned deltas. Access to mutable changes requires the repository's state lock. */
internal class ConversationPendingChanges(private val clock: () -> Long) {
    private val changes = mutableMapOf<Uuid, PendingConversationChange>()
    private var version = 0L
    val keys: Set<Uuid> get() = changes.keys
    val values: Collection<PendingConversationChange> get() = changes.values

    operator fun get(id: Uuid) = changes[id]

    fun remove(id: Uuid) {
        changes.remove(id)
    }

    fun dueIds() = changes.filterValues { it.due <= clock() }.keys.toList()

    fun stage(
        metadata: AssistantSessionState,
        turn: AssistantSessionTurn?,
        delayMillis: Long,
    ): PendingConversationChange {
        val previous = changes[metadata.id]
        val next = PendingConversationChange(
            metadata = metadata.copy(turns = emptyList()),
            turn = turn ?: previous?.turn,
            version = ++version,
            due = minOf(previous?.due ?: Long.MAX_VALUE, clock() + delayMillis),
        )
        changes[metadata.id] = next
        return next
    }

    fun prepare(
        metadata: AssistantSessionState,
        captured: PendingConversationChange?,
    ): AssistantSessionState =
        if (captured != null && (captured.metadata.draftRevision != metadata.draftRevision || captured.metadata.editorId != metadata.editorId)) {
            mergeEditor(metadata = metadata, editor = captured.metadata)
        } else {
            metadata.copy(
                position = captured?.metadata?.position ?: metadata.position,
                rawPosition = captured?.metadata?.rawPosition ?: metadata.rawPosition,
            )
        }

    /** Clear only the committed version; edits and deltas arriving during I/O remain pending. */
    fun committed(
        writing: AssistantSessionState,
        turn: AssistantSessionTurn?,
        captured: PendingConversationChange?,
    ): AssistantSessionState {
        val latest = changes[writing.id]
        val merged = if (latest != null && latest.version != captured?.version) {
            mergeEditor(metadata = writing, editor = latest.metadata).let { merged ->
                if (latest.turn != captured?.turn && latest.turn != null) {
                    merged.copy(
                        lastStatus = latest.turn.status,
                        activityAt = latest.metadata.activityAt,
                    )
                } else merged
            }
        } else writing
        if (latest != null && latest.version != captured?.version) {
            changes[writing.id] = latest.copy(
                metadata = merged,
                turn = latest.turn?.takeUnless { it == turn },
            )
        } else {
            changes.remove(writing.id)
        }
        return merged
    }

    private fun mergeEditor(metadata: AssistantSessionState, editor: AssistantSessionState) =
        metadata.copy(
            draft = editor.draft,
            editorId = editor.editorId,
            draftRevision = editor.draftRevision,
            selectionStart = editor.selectionStart,
            selectionEnd = editor.selectionEnd,
            position = editor.position,
            rawPosition = editor.rawPosition,
        )
}
