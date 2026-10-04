package com.skyd.podaura.util.coil.localmedia

// The native macOS target has no libmpv integration; keep its existing embedded-artwork fallback.
internal actual fun getLocalVideoThumbnailData(filePath: String): ByteArray? = null
