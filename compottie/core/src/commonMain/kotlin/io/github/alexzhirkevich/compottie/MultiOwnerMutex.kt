package io.github.alexzhirkevich.compottie

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@InternalCompottieApi
public class MultiOwnerMutex {

    private val lock = reentrantLock()
    private val mutex = mutableMapOf<Any, Mutex>()

    public suspend fun <T> withLock(key: Any, action: suspend () -> T): T {
        val keyLock = lock.withLock {
            mutex.getOrPut(key, ::Mutex)
        }

        return try {
            keyLock.withLock {
                action()
            }
        } finally {
            lock.withLock {
                mutex.remove(key)
            }
        }
    }
}
