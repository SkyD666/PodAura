package com.skyd.podaura.model.repository.fullcontent

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.AF_UNSPEC
import platform.posix.SOCK_STREAM
import platform.posix.addrinfo
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.memcpy
import platform.posix.memset
import platform.posix.sockaddr_in
import platform.posix.sockaddr_in6

internal actual object PlatformHostAddressResolver : HostAddressResolver {
    actual override suspend fun resolve(host: String): List<ByteArray> =
        withContext(Dispatchers.Default) { resolveBlocking(host) }

    private fun resolveBlocking(host: String): List<ByteArray> = memScoped {
        val hints = alloc<addrinfo>()
        memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        val result = alloc<CPointerVar<addrinfo>>()
        result.value = null
        val error = getaddrinfo(host, null, hints.ptr, result.ptr)
        if (error != 0) error("Unable to resolve host (getaddrinfo: $error)")

        try {
            buildList {
                var current = result.value
                while (current != null) {
                    val info = current.pointed
                    val address = when (info.ai_family) {
                        AF_INET -> ByteArray(4).also { bytes ->
                            val source = info.ai_addr!!.reinterpret<sockaddr_in>()
                                .pointed.sin_addr.ptr
                            bytes.usePinned { memcpy(it.addressOf(0), source, bytes.size.convert()) }
                        }

                        AF_INET6 -> ByteArray(16).also { bytes ->
                            val source = info.ai_addr!!.reinterpret<sockaddr_in6>()
                                .pointed.sin6_addr.ptr
                            bytes.usePinned { memcpy(it.addressOf(0), source, bytes.size.convert()) }
                        }

                        else -> null
                    }
                    address?.let(::add)
                    current = info.ai_next
                }
            }
        } finally {
            result.value?.let(::freeaddrinfo)
        }
    }
}
