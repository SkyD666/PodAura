package com.skyd.anivu.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.text.parseAsHtml
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.showToast
import net.dankito.readability4j.extended.Readability4JExtended

fun String.toEncodedUrl(allow: String? = ":/-![].,%?&="): String {
    return Uri.encode(this, allow)
}

fun String.toDecodedUrl(): String {
    return Uri.decode(this)
}

fun String.toRemoveHtml(): String = parseAsHtml().toString()

fun CharSequence.splitByBlank(limit: Int = 0): List<String> = trim().split("\\s+".toRegex(), limit)

fun String.firstCodePointOrNull(): String? =
    if (isEmpty()) null else String(Character.toChars(codePointAt(0)))

fun String.readable(): String =
    Readability4JExtended("", this).parse().textContent?.trim().orEmpty()

fun String.copy(context: Context) {
    try {
        val systemService = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        systemService.setPrimaryClip(ClipData.newPlainText("text", this))
        // If you show a copy confirmation toast in Android 13, the user sees duplicate messages.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            context.getString(R.string.copied).showToast()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun String.validateFileName(maxFilenameLength: Int = 255): String {
    if (isEmpty()) {
        return ""
    }

    // 获取文件名和后缀名
    var name: String = this
    var extension = ""
    val dotIndex = lastIndexOf(".")
    if (dotIndex != -1) {
        name = substring(0, dotIndex)
        extension = substring(dotIndex)
    }

    // 检查文件名长度
    if (length > maxFilenameLength) {
        // 如果文件名过长，则截断后半部分
        name = name.substring(0, maxFilenameLength - extension.length)
    }

    // 去除文件名中的非法字符
    name = name.replace("[\\\\/:*?\"<>|]".toRegex(), "")

    // 返回合法的文件名和后缀名
    return name + extension
}

fun <C : CharSequence> C?.ifNullOfBlank(defaultValue: () -> C): C =
    if (!isNullOrBlank()) this else defaultValue()