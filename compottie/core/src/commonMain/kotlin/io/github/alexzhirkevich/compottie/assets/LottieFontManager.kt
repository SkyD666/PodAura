package io.github.alexzhirkevich.compottie.assets

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.Font

/**
 * Used to load animation fonts. If manager returns null then glyphs or default platform font
 * will be used
 * */
@Stable
public interface LottieFontManager {

    /**
     * Load [font] by requirements
     * */
    public suspend fun font(font: LottieFontSpec): Font?
}

internal object EmptyFontManager : LottieFontManager {
    override suspend fun font(font: LottieFontSpec): Font? {
        return null
    }
}