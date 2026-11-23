package com.skyd.podaura.nano.model.db

/** OS-owned lock next to the actual database. A leftover file does not imply ownership. */
expect class ConversationDatabaseLock(path: String) {
    fun acquire()
    fun release()
}
