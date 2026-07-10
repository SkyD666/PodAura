package io.github.alexzhirkevich.compottie.assets

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

public class LottieFontSpec internal constructor(
    public val family: String,
    public val name: String,
    public val style: FontStyle,
    public val weight: FontWeight,
    public val path: String?,
    public val origin: FontOrigin,
    public val ascent: Float
) {
    public enum class FontOrigin {
        /**
         * Font from assets or file system
         * */
        Local,

        /**
         * Css font from url
         *
         * Example: [https://fonts.googleapis.com/css2?family=Poppins:wght@700&display=swap](https://fonts.googleapis.com/css2?family=Poppins:wght@700&display=swap)
         * */
        CssUrl,


        ScriptUrl,

        /**
         * .ttf font from url
         *
         * Example: [https://fonts.gstatic.com/s/ubuntu/v15/4iCp6KVjbNBYlgoKejZftWyI.ttf](https://fonts.gstatic.com/s/ubuntu/v15/4iCp6KVjbNBYlgoKejZftWyI.ttf)
         * */
        FontUrl,

        /**
         * Font from unknown source
         *
         * Example: [https://fonts.gstatic.com/s/ubuntu/v15/4iCp6KVjbNBYlgoKejZftWyI.ttf](https://fonts.gstatic.com/s/ubuntu/v15/4iCp6KVjbNBYlgoKejZftWyI.ttf)
         * */
        Unknown
    }
}