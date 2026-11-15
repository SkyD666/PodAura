@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.skyd.podaura.podnya.model.db

import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageException
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.F_SETLK
import platform.posix.F_WRLCK
import platform.posix.O_CREAT
import platform.posix.O_RDWR
import platform.posix.SEEK_SET
import platform.posix.close
import platform.posix.fcntl
import platform.posix.flock
import platform.posix.open

actual class ConversationDatabaseLock actual constructor(private val path: String) {
    private var descriptor = -1

    actual fun acquire() {
        check(descriptor == -1)
        val opened = open("$path.lock", O_CREAT or O_RDWR, 384)
        check(opened >= 0) { "Cannot open conversation storage lock" }
        if (memScoped {
                val request = alloc<flock>()
                request.l_type = F_WRLCK.toShort()
                request.l_whence = SEEK_SET.toShort()
                request.l_start = 0
                request.l_len = 0
                fcntl(opened, F_SETLK, request.ptr)
            } != 0
        ) {
            close(opened)
            throw ConversationStorageException(ConversationStorageState.OCCUPIED)
        }
        descriptor = opened
    }

    actual fun release() {
        if (descriptor >= 0) {
            close(descriptor)
            descriptor = -1
        }
    }
}
