package com.example.mykeyboard.theme

/**
 * Responsive label sizing derived from the actual row height, so labels scale with the
 * keyboard-height setting, landscape and tablets instead of using fixed sp values.
 * Returned sizes are in px.
 */
object KeyLabelMetrics {

    fun isDevanagari(text: String): Boolean {
        for (ch in text) if (ch in 'ऀ'..'ॿ') return true
        return false
    }

    fun letterPx(rowHeightPx: Float, density: Float, text: String, hasAlt: Boolean, typography: Typography, userScale: Float): Float {
        var size = rowHeightPx * 0.45f * typography.letterScale * userScale
        if (hasAlt) size *= 0.90f
        // Devanagari glyphs carry matras above/below; keep them inside the keycap.
        if (isDevanagari(text)) size *= 0.84f
        return size.coerceIn(13f * density, 32f * density * userScale)
    }

    fun altHintPx(rowHeightPx: Float, density: Float, userScale: Float): Float =
        (rowHeightPx * 0.21f * userScale).coerceIn(8.5f * density, 13f * density)

    fun functionTextPx(rowHeightPx: Float, density: Float, text: String, userScale: Float): Float {
        var size = rowHeightPx * 0.31f * userScale
        if (text.length > 4) size *= 0.85f
        if (isDevanagari(text)) size *= 0.92f
        return size.coerceIn(11f * density, 18f * density)
    }

    fun spaceLabelPx(rowHeightPx: Float, density: Float, userScale: Float): Float =
        (rowHeightPx * 0.26f * userScale).coerceIn(10f * density, 15f * density)

    /** Square icon box for shift / backspace / enter / globe. */
    fun iconPx(rowHeightPx: Float, density: Float, userScale: Float): Float =
        (rowHeightPx * 0.47f * userScale).coerceIn(16f * density, 28f * density)
}
