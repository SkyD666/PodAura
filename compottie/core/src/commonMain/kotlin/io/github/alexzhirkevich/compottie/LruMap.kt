package io.github.alexzhirkevich.compottie

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

internal class LruMap<T : Any>(
    private val delegate: MutableMap<Any, T> = LinkedHashMap(),
    private val limit: Int,
) : MutableMap<Any, T> by delegate {

    @OptIn(InternalCompottieApi::class)
    private val suspendGetOrPutMutex = MultiOwnerMutex()

    private val lock = reentrantLock()

    override fun put(key: Any, value: T): T? = lock.withLock { putRaw(key, value) }

    override fun clear() = lock.withLock { clearRaw() }

    override fun putAll(from: Map<out Any, T>) = lock.withLock { putAllRaw(from) }

    override fun remove(key: Any): T? = lock.withLock { removeRaw(key) }

    override fun get(key: Any): T? = lock.withLock { getRaw(key) }

    fun getOrPut(key: Any?, put: () -> T): T {
        if (key == null)
            return put()

        return lock.withLock {
            getRaw(key) ?: run {
                val v = put()
                putRaw(key, v)
                v
            }
        }
    }

    @OptIn(InternalCompottieApi::class)
    suspend fun getOrPutSuspend(key: Any, put: suspend () -> T): T {
        return suspendGetOrPutMutex.withLock(key) {
            get(key) ?: put().also { put(key, it) }
        }
    }

    private fun putRaw(key: Any, value: T): T? {

        if (limit < 1) {
            return value
        }

        while (size > limit && remove(keys.firstOrNull()) != null) {
            // nothing
        }

        return delegate.put(key, value)
    }

    private fun putAllRaw(from: Map<out Any, T>) {
        from.forEach {
            putRaw(it.key, it.value)
        }
    }

    private fun getRaw(key: Any): T? {
        val cached = removeRaw(key) ?: return null
        putRaw(key, cached)
        return cached
    }

    private fun removeRaw(key: Any): T? = delegate.remove(key)

    private fun clearRaw() = delegate.clear()
}
