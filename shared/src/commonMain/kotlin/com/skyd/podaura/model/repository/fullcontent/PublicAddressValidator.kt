package com.skyd.podaura.model.repository.fullcontent

import io.ktor.http.Url

internal fun interface HostAddressResolver {
    suspend fun resolve(host: String): List<ByteArray>
}

internal expect object PlatformHostAddressResolver : HostAddressResolver

internal class PublicAddressValidator(
    private val resolver: HostAddressResolver = PlatformHostAddressResolver,
) {
    suspend fun validate(url: String): Url {
        val parsed = runCatching { Url(url) }.getOrElse {
            throw FullContentException("Invalid article URL", it)
        }
        if (parsed.protocol.name !in setOf("http", "https")) {
            throw FullContentException("Unsupported article URL")
        }
        if (parsed.host.isBlank()) throw FullContentException("Article URL has no host")

        val addresses = runCatching { resolver.resolve(parsed.host) }.getOrElse {
            throw FullContentException("Unable to resolve article host", it)
        }
        if (addresses.isEmpty() || addresses.any { !it.isPublicAddress() }) {
            throw FullContentException("Article URL resolves to a non-public address")
        }
        return parsed
    }
}

private fun ByteArray.isPublicAddress(): Boolean = when (size) {
    4 -> isPublicIpv4()
    16 -> isPublicIpv6()
    else -> false
}

private fun ByteArray.isPublicIpv4(): Boolean {
    val first = this[0].toUByte().toInt()
    val second = this[1].toUByte().toInt()
    return when {
        first == 0 -> false
        first == 10 -> false
        first == 100 && second in 64..127 -> false
        first == 127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 -> false
        first == 192 && second == 168 -> false
        first == 198 && second in 18..19 -> false
        first >= 224 -> false
        else -> true
    }
}

private fun ByteArray.isPublicIpv6(): Boolean {
    val first = this[0].toUByte().toInt()
    val second = this[1].toUByte().toInt()
    if (all { it == 0.toByte() }) return false
    if (dropLast(1).all { it == 0.toByte() } && last() == 1.toByte()) return false
    if (first and 0xFE == 0xFC) return false
    if (first == 0xFE && second and 0xC0 == 0x80) return false
    if (first == 0xFF) return false

    val ipv4Mapped = take(10).all { it == 0.toByte() } &&
        this[10] == 0xFF.toByte() && this[11] == 0xFF.toByte()
    if (ipv4Mapped) return copyOfRange(12, 16).isPublicIpv4()

    // Unspecified, IPv4-compatible, discard-only and documentation prefixes are not public targets.
    if (take(12).all { it == 0.toByte() }) return false
    if (first == 0x01 && second == 0x00 && drop(2).take(6).all { it == 0.toByte() }) return false
    if (first == 0x20 && second == 0x01 && this[2] == 0x0D.toByte() && this[3] == 0xB8.toByte()) {
        return false
    }
    return true
}
