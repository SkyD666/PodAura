package io.github.alexzhirkevich.compottie

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver

internal actual fun mockFontFamilyResolver(): FontFamily.Resolver = createFontFamilyResolver()
