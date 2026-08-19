package com.skyd.podaura.model.repository.fullcontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

internal actual object PlatformHostAddressResolver : HostAddressResolver {
    actual override suspend fun resolve(host: String): List<ByteArray> =
        withContext(Dispatchers.IO) {
            InetAddress.getAllByName(host).map(InetAddress::getAddress)
        }
}
