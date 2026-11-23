package com.skyd.podaura.nano.model.db

import com.skyd.podaura.nano.model.bean.conversation.ConversationStorageException
import com.skyd.podaura.nano.model.bean.conversation.ConversationStorageState
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

actual class ConversationDatabaseLock actual constructor(path: String) {
    private val file = File("${File(path).canonicalPath}.lock")
    private var handle: RandomAccessFile? = null
    private var lock: FileLock? = null

    actual fun acquire() {
        check(handle == null)
        file.parentFile?.mkdirs()
        val opened = RandomAccessFile(file, "rw")
        try {
            val acquired = try {
                opened.channel.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            } ?: throw ConversationStorageException(ConversationStorageState.OCCUPIED)
            handle = opened
            lock = acquired
        } catch (failure: Throwable) {
            opened.close()
            throw failure
        }
    }

    actual fun release() {
        try {
            lock?.release()
        } finally {
            lock = null
            handle?.close()
            handle = null
        }
    }
}
