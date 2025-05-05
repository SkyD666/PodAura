package com.skyd.podaura.util.fileicon

import com.skyd.podaura.ext.isDirectory
import kotlinx.io.files.Path
import org.jetbrains.compose.resources.DrawableResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.ic_file_apk
import podaura.shared.generated.resources.ic_file_archive
import podaura.shared.generated.resources.ic_file_audio
import podaura.shared.generated.resources.ic_file_calendar
import podaura.shared.generated.resources.ic_file_certificate
import podaura.shared.generated.resources.ic_file_code
import podaura.shared.generated.resources.ic_file_contact
import podaura.shared.generated.resources.ic_file_directory
import podaura.shared.generated.resources.ic_file_document
import podaura.shared.generated.resources.ic_file_ebook
import podaura.shared.generated.resources.ic_file_email
import podaura.shared.generated.resources.ic_file_excel
import podaura.shared.generated.resources.ic_file_font
import podaura.shared.generated.resources.ic_file_generic
import podaura.shared.generated.resources.ic_file_image
import podaura.shared.generated.resources.ic_file_pdf
import podaura.shared.generated.resources.ic_file_powerpoint
import podaura.shared.generated.resources.ic_file_presentation
import podaura.shared.generated.resources.ic_file_spreadsheet
import podaura.shared.generated.resources.ic_file_text
import podaura.shared.generated.resources.ic_file_video
import podaura.shared.generated.resources.ic_file_word

enum class MimeTypeIcon(val resource: DrawableResource) {
    APK(Res.drawable.ic_file_apk),
    ARCHIVE(Res.drawable.ic_file_archive),
    AUDIO(Res.drawable.ic_file_audio),
    CALENDAR(Res.drawable.ic_file_calendar),
    CERTIFICATE(Res.drawable.ic_file_certificate),
    CODE(Res.drawable.ic_file_code),
    CONTACT(Res.drawable.ic_file_contact),
    DIRECTORY(Res.drawable.ic_file_directory),
    DOCUMENT(Res.drawable.ic_file_document),
    EBOOK(Res.drawable.ic_file_ebook),
    EMAIL(Res.drawable.ic_file_email),
    FONT(Res.drawable.ic_file_font),
    GENERIC(Res.drawable.ic_file_generic),
    IMAGE(Res.drawable.ic_file_image),
    PDF(Res.drawable.ic_file_pdf),
    PRESENTATION(Res.drawable.ic_file_presentation),
    SPREADSHEET(Res.drawable.ic_file_spreadsheet),
    TEXT(Res.drawable.ic_file_text),
    VIDEO(Res.drawable.ic_file_video),
    WORD(Res.drawable.ic_file_word),
    EXCEL(Res.drawable.ic_file_excel),
    POWERPOINT(Res.drawable.ic_file_powerpoint)
}

expect fun Path.mimeType(): String?

// See also https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/com/android/internal/util/MimeIconUtils.java
// See also https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Complete_list_of_MIME_types
// See also http://www.iana.org/assignments/media-types/media-types.xhtml
// See also /usr/share/mime/packages/freedesktop.org.xml
fun getFileIcon(mimetype: String): MimeTypeIcon {
    return mimeTypeToIconMap[mimetype]
        ?: typeToIconMap[mimetype.substringBefore("/")]
        ?: suffixToIconMap[mimetype.substringAfterLast("*")]
        ?: MimeTypeIcon.GENERIC
}

fun Path.fileIcon(): MimeTypeIcon {
    return if (isDirectory == true) {
        MimeTypeIcon.DIRECTORY
    } else {
        val mimetype = mimeType() ?: return MimeTypeIcon.GENERIC
        mimeTypeToIconMap[mimetype]
            ?: typeToIconMap[mimetype.substringBefore("/")]
            ?: suffixToIconMap[mimetype.substringAfterLast("*")]
            ?: MimeTypeIcon.GENERIC
    }
}

// See also https://mimesniff.spec.whatwg.org/#mime-type-groups
private val mimeTypeToIconMap = mapOf(
    "application/vnd.android.package-archive" to MimeTypeIcon.APK,
    "application/gzip" to MimeTypeIcon.ARCHIVE,
    // Not in IANA list, but Mozilla and Wikipedia say so.
    "application/java-archive" to MimeTypeIcon.ARCHIVE,
    "application/mac-binhex40" to MimeTypeIcon.ARCHIVE,
    // Not in IANA list, but AOSP MimeUtils used to say so.
    "application/rar" to MimeTypeIcon.ARCHIVE,
    "application/zip" to MimeTypeIcon.ARCHIVE,
    "application/zstd" to MimeTypeIcon.ARCHIVE,
    "application/vnd.debian.binary-package" to MimeTypeIcon.ARCHIVE,
    "application/vnd.ms-cab-compressed" to MimeTypeIcon.ARCHIVE,
    "application/vnd.rar" to MimeTypeIcon.ARCHIVE,
    "application/x-7z-compressed" to MimeTypeIcon.ARCHIVE,
    "application/x-apple-diskimage" to MimeTypeIcon.ARCHIVE,
    "application/x-bzip" to MimeTypeIcon.ARCHIVE,
    "application/x-bzip2" to MimeTypeIcon.ARCHIVE,
    "application/x-compress" to MimeTypeIcon.ARCHIVE,
    "application/x-cpio" to MimeTypeIcon.ARCHIVE,
    "application/x-deb" to MimeTypeIcon.ARCHIVE,
    "application/x-debian-package" to MimeTypeIcon.ARCHIVE,
    "application/x-gtar" to MimeTypeIcon.ARCHIVE,
    "application/x-gtar-compressed" to MimeTypeIcon.ARCHIVE,
    "application/x-gzip" to MimeTypeIcon.ARCHIVE,
    "application/x-iso9660-image" to MimeTypeIcon.ARCHIVE,
    "application/x-java-archive" to MimeTypeIcon.ARCHIVE,
    "application/x-lha" to MimeTypeIcon.ARCHIVE,
    "application/x-lzh" to MimeTypeIcon.ARCHIVE,
    "application/x-lzma" to MimeTypeIcon.ARCHIVE,
    "application/x-lzx" to MimeTypeIcon.ARCHIVE,
    "application/x-rar-compressed" to MimeTypeIcon.ARCHIVE,
    "application/x-stuffit" to MimeTypeIcon.ARCHIVE,
    "application/x-tar" to MimeTypeIcon.ARCHIVE,
    "application/x-webarchive" to MimeTypeIcon.ARCHIVE,
    "application/x-webarchive-xml" to MimeTypeIcon.ARCHIVE,
    "application/x-xz" to MimeTypeIcon.ARCHIVE,
    "application/ogg" to MimeTypeIcon.AUDIO,
    "application/x-flac" to MimeTypeIcon.AUDIO,
    "text/calendar" to MimeTypeIcon.CALENDAR,
    "text/x-vcalendar" to MimeTypeIcon.CALENDAR,
    "application/pgp-keys" to MimeTypeIcon.CERTIFICATE,
    "application/pgp-signature" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs12" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs7-certificates" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs7-certreqresp" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs7-crl" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs7-mime" to MimeTypeIcon.CERTIFICATE,
    "application/x-pkcs7-signature" to MimeTypeIcon.CERTIFICATE,
    "application/x-x509-ca-cert" to MimeTypeIcon.CERTIFICATE,
    "application/x-x509-server-cert" to MimeTypeIcon.CERTIFICATE,
    "application/x-x509-user-cert" to MimeTypeIcon.CERTIFICATE,
    "application/ecmascript" to MimeTypeIcon.CODE,
    "application/javascript" to MimeTypeIcon.CODE,
    "application/json" to MimeTypeIcon.CODE,
    "application/typescript" to MimeTypeIcon.CODE,
    "application/xml" to MimeTypeIcon.CODE,
    "application/yaml" to MimeTypeIcon.CODE,
    "application/x-csh" to MimeTypeIcon.CODE,
    "application/x-ecmascript" to MimeTypeIcon.CODE,
    "application/x-javascript" to MimeTypeIcon.CODE,
    "application/x-latex" to MimeTypeIcon.CODE,
    "application/x-perl" to MimeTypeIcon.CODE,
    "application/x-python" to MimeTypeIcon.CODE,
    "application/x-ruby" to MimeTypeIcon.CODE,
    "application/x-sh" to MimeTypeIcon.CODE,
    "application/x-shellscript" to MimeTypeIcon.CODE,
    "application/x-texinfo" to MimeTypeIcon.CODE,
    "application/x-yaml" to MimeTypeIcon.CODE,
    "text/css" to MimeTypeIcon.CODE,
    "text/html" to MimeTypeIcon.CODE,
    "text/ecmascript" to MimeTypeIcon.CODE,
    "text/javascript" to MimeTypeIcon.CODE,
    "text/jscript" to MimeTypeIcon.CODE,
    "text/livescript" to MimeTypeIcon.CODE,
    "text/xml" to MimeTypeIcon.CODE,
    "text/x-asm" to MimeTypeIcon.CODE,
    "text/x-c++hdr" to MimeTypeIcon.CODE,
    "text/x-c++src" to MimeTypeIcon.CODE,
    "text/x-chdr" to MimeTypeIcon.CODE,
    "text/x-csh" to MimeTypeIcon.CODE,
    "text/x-csharp" to MimeTypeIcon.CODE,
    "text/x-csrc" to MimeTypeIcon.CODE,
    "text/x-dsrc" to MimeTypeIcon.CODE,
    "text/x-ecmascript" to MimeTypeIcon.CODE,
    "text/x-haskell" to MimeTypeIcon.CODE,
    "text/x-java" to MimeTypeIcon.CODE,
    "text/x-javascript" to MimeTypeIcon.CODE,
    "text/x-literate-haskell" to MimeTypeIcon.CODE,
    "text/x-pascal" to MimeTypeIcon.CODE,
    "text/x-perl" to MimeTypeIcon.CODE,
    "text/x-python" to MimeTypeIcon.CODE,
    "text/x-ruby" to MimeTypeIcon.CODE,
    "text/x-shellscript" to MimeTypeIcon.CODE,
    "text/x-tcl" to MimeTypeIcon.CODE,
    "text/x-tex" to MimeTypeIcon.CODE,
    "text/x-yaml" to MimeTypeIcon.CODE,
    "text/vcard" to MimeTypeIcon.CONTACT,
    "text/x-vcard" to MimeTypeIcon.CONTACT,
    "inode/directory" to MimeTypeIcon.DIRECTORY,
    "vnd.android.document/directory" to MimeTypeIcon.DIRECTORY,
    "application/rtf" to MimeTypeIcon.DOCUMENT,
    "application/vnd.kde.kword" to MimeTypeIcon.DOCUMENT,
    "application/vnd.oasis.opendocument.text" to MimeTypeIcon.DOCUMENT,
    "application/vnd.oasis.opendocument.text-master" to MimeTypeIcon.DOCUMENT,
    "application/vnd.oasis.opendocument.text-template" to MimeTypeIcon.DOCUMENT,
    "application/vnd.oasis.opendocument.text-web" to MimeTypeIcon.DOCUMENT,
    "application/vnd.stardivision.writer" to MimeTypeIcon.DOCUMENT,
    "application/vnd.stardivision.writer-global" to MimeTypeIcon.DOCUMENT,
    "application/vnd.sun.xml.writer" to MimeTypeIcon.DOCUMENT,
    "application/vnd.sun.xml.writer.global" to MimeTypeIcon.DOCUMENT,
    "application/vnd.sun.xml.writer.template" to MimeTypeIcon.DOCUMENT,
    "application/x-abiword" to MimeTypeIcon.DOCUMENT,
    "application/x-kword" to MimeTypeIcon.DOCUMENT,
    "text/rtf" to MimeTypeIcon.DOCUMENT,
    "application/epub+zip" to MimeTypeIcon.EBOOK,
    "application/vnd.amazon.ebook" to MimeTypeIcon.EBOOK,
    "application/vnd.amazon.mobi8-ebook" to MimeTypeIcon.EBOOK,
    "application/vnd.comicbook-rar" to MimeTypeIcon.EBOOK,
    "application/vnd.comicbook+zip" to MimeTypeIcon.EBOOK,
    "application/x-cbr" to MimeTypeIcon.EBOOK,
    "application/x-cbz" to MimeTypeIcon.EBOOK,
    "application/x-ibooks+zip" to MimeTypeIcon.EBOOK,
    "application/x-mobipocket-ebook" to MimeTypeIcon.EBOOK,
    "application/vnd.ms-outlook" to MimeTypeIcon.EMAIL,
    "message/rfc822" to MimeTypeIcon.EMAIL,
    "application/font-cff" to MimeTypeIcon.FONT,
    "application/font-off" to MimeTypeIcon.FONT,
    "application/font-sfnt" to MimeTypeIcon.FONT,
    "application/font-ttf" to MimeTypeIcon.FONT,
    "application/font-woff" to MimeTypeIcon.FONT,
    "application/vnd.ms-fontobject" to MimeTypeIcon.FONT,
    "application/vnd.ms-opentype" to MimeTypeIcon.FONT,
    "application/x-font" to MimeTypeIcon.FONT,
    "application/x-font-ttf" to MimeTypeIcon.FONT,
    "application/x-font-woff" to MimeTypeIcon.FONT,
    "application/vnd.oasis.opendocument.graphics" to MimeTypeIcon.IMAGE,
    "application/vnd.oasis.opendocument.graphics-template" to MimeTypeIcon.IMAGE,
    "application/vnd.oasis.opendocument.image" to MimeTypeIcon.IMAGE,
    "application/vnd.stardivision.draw" to MimeTypeIcon.IMAGE,
    "application/vnd.sun.xml.draw" to MimeTypeIcon.IMAGE,
    "application/vnd.sun.xml.draw.template" to MimeTypeIcon.IMAGE,
    "application/vnd.visio" to MimeTypeIcon.IMAGE,
    "application/pdf" to MimeTypeIcon.PDF,
    "application/vnd.kde.kpresenter" to MimeTypeIcon.PRESENTATION,
    "application/vnd.oasis.opendocument.presentation" to MimeTypeIcon.PRESENTATION,
    "application/vnd.oasis.opendocument.presentation-template" to MimeTypeIcon.PRESENTATION,
    "application/vnd.stardivision.impress" to MimeTypeIcon.PRESENTATION,
    "application/vnd.sun.xml.impress" to MimeTypeIcon.PRESENTATION,
    "application/vnd.sun.xml.impress.template" to MimeTypeIcon.PRESENTATION,
    "application/x-kpresenter" to MimeTypeIcon.PRESENTATION,
    "application/vnd.kde.kspread" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.oasis.opendocument.spreadsheet" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.oasis.opendocument.spreadsheet-template" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.stardivision.calc" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.sun.xml.calc" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.sun.xml.calc.template" to MimeTypeIcon.SPREADSHEET,
    "application/x-kspread" to MimeTypeIcon.SPREADSHEET,
    "application/vnd.adobe.flash.movie" to MimeTypeIcon.VIDEO,
    "application/x-quicktimeplayer" to MimeTypeIcon.VIDEO,
    "application/x-shockwave-flash" to MimeTypeIcon.VIDEO,
    "application/msword" to MimeTypeIcon.WORD,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to MimeTypeIcon.WORD,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template" to MimeTypeIcon.WORD,
    "application/vnd.ms-excel" to MimeTypeIcon.EXCEL,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to MimeTypeIcon.EXCEL,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template" to MimeTypeIcon.EXCEL,
    "application/vnd.ms-powerpoint" to MimeTypeIcon.POWERPOINT,
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            to MimeTypeIcon.POWERPOINT,
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
            to MimeTypeIcon.POWERPOINT,
    "application/vnd.openxmlformats-officedocument.presentationml.template"
            to MimeTypeIcon.POWERPOINT
).mapKeys { it.key }

private val typeToIconMap = mapOf(
    "audio" to MimeTypeIcon.AUDIO,
    "font" to MimeTypeIcon.FONT,
    "image" to MimeTypeIcon.IMAGE,
    "text" to MimeTypeIcon.TEXT,
    "video" to MimeTypeIcon.VIDEO
)

private val suffixToIconMap = mapOf(
    "json" to MimeTypeIcon.CODE,
    "xml" to MimeTypeIcon.CODE,
    "zip" to MimeTypeIcon.ARCHIVE
)
