package com.skyd.podaura.ext

import com.fleeksoft.ksoup.Ksoup
import de.cketti.codepoints.codePointAt
import de.cketti.codepoints.deluxe.toCodePoint
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.authority
import io.ktor.http.decodeURLQueryComponent

fun String.encodeURL(): String = URLBuilder(this).toString()

fun String.decodeURL(): String = decodeURLQueryComponent()

val String.isUrl: Boolean
    get() {
        val regex = Regex("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]")
        return regex.matches(this)
    }

fun String.isHttpOrHttps(): Boolean = runCatching {
    val urlBuilder = URLBuilder(this)
    urlBuilder.protocol in arrayOf(URLProtocol.HTTP, URLProtocol.HTTPS)
}.getOrDefault(false)

fun String.httpDomain(): String? {
    val urlBuilder = URLBuilder(this)
    if (!isHttpOrHttps()) {
        return null
    }
    return with(urlBuilder) { "${protocol}://${authority}" }
}

expect fun String.asPlatformFile(): PlatformFile

fun CharSequence.splitByBlank(limit: Int = 0): List<String> = trim().split("\\s+".toRegex(), limit)

fun String.firstCodePointOrNull(): String? =
    if (isEmpty()) null else codePointAt(0).toCodePoint().toChars().concatToString()

fun String.readable(): String = Ksoup.parse(html = this).text()

fun String.validateFileName(maxFilenameLength: Int = 255): String {
    if (isEmpty()) return ""

    // File name and suffix
    var name: String = this
    var extension = ""
    val dotIndex = lastIndexOf(".")
    if (dotIndex != -1) {
        name = substring(0, dotIndex)
        extension = substring(dotIndex)
    }

    // Check file name length
    if (length > maxFilenameLength) {
        name = name.substring(0, maxFilenameLength - extension.length)
    }

    // Remove illegal chars
    name = name.replace("[\\\\/:*?\"<>|]".toRegex(), "")

    return name + extension
}

inline val String.extName: String
    get() = substringAfterLast(".", missingDelimiterValue = "")

fun <C : CharSequence> C?.ifNullOfBlank(defaultValue: () -> C): C =
    if (!isNullOrBlank()) this else defaultValue()

expect fun String.isLocalFile(): Boolean

expect fun String.isLocalFileExists(): Boolean

expect fun String.isNetworkUrl(): Boolean

fun <T : CharSequence> T.takeIfNotBlank(): T? = takeIf { it.isNotBlank() }
