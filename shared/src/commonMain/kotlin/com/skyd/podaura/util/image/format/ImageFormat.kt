package com.skyd.podaura.util.image.format

enum class ImageFormat {
    JPG,
    PNG,
    APNG,
    GIF,
    WEBP,
    BMP,
    HEIF,
    HEIC,
    SVG,
    UNDEFINED;

    override fun toString(): String {
        return when (this) {
            JPG -> ".jpg"
            PNG -> ".png"
            APNG -> ".apng"
            GIF -> ".gif"
            WEBP -> ".webp"
            BMP -> ".bmp"
            HEIF -> ".heif"
            HEIC -> ".heic"
            SVG -> ".svg"
            UNDEFINED -> ""
        }
    }

    fun toMimeType(): String {
        return when (this) {
            JPG -> "image/jpg"
            PNG -> "image/png"
            APNG -> "image/apng"
            GIF -> "image/gif"
            WEBP -> "image/webp"
            BMP -> "image/bmp"
            HEIF -> "image/heif"
            HEIC -> "image/heic"
            SVG -> "image/svg+xml"
            UNDEFINED -> "image/*"
        }
    }

    companion object {
        fun fromMimeType(mimeType: String): ImageFormat {
            return when (mimeType) {
                "image/jpg" -> JPG
                "image/apng" -> APNG
                "image/png" -> PNG
                "image/gif" -> GIF
                "image/webp" -> WEBP
                "image/bmp" -> BMP
                "image/heif" -> HEIF
                "image/heic" -> HEIC
                "image/svg+xml" -> SVG
                else -> UNDEFINED
            }
        }
    }
}