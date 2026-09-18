package com.example.mykeyboard.theme

import com.example.mykeyboard.model.KeyModel
import com.example.mykeyboard.model.KeyType
import com.example.mykeyboard.model.KeyboardTheme

/**
 * Assigns semantic role and base colour to every key of a layout according to the theme's
 * [ColorStrategy]. Runs once per layout build (mode / language / theme change), never per touch.
 */
object KeyColorResolver {

    private val VOWELS = setOf(
        "a", "e", "i", "o", "u",
        "अ", "आ", "इ", "ई", "उ", "ऊ", "ए", "ऐ", "ओ", "औ", "ऋ",
        "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ"
    )

    fun roleOf(key: KeyModel): KeyRole = when (key.type) {
        KeyType.CHARACTER -> KeyRole.CHARACTER
        KeyType.SHIFT -> KeyRole.SHIFT
        KeyType.ENTER -> KeyRole.ACTION
        KeyType.SPACE -> KeyRole.SPACE
        KeyType.EMOJI -> KeyRole.EMOJI
        else -> KeyRole.FUNCTION
    }

    fun isWide(key: KeyModel): Boolean = key.weight >= 1.4f || key.type == KeyType.SPACE

    /**
     * Base (face) colour for a key. [row]/[col] are indices within the rendered layout (col counts
     * character keys only); [xFraction] is the key centre across the keyboard width (0..1).
     */
    fun baseColor(theme: KeyboardTheme, key: KeyModel, role: KeyRole, row: Int, rowCount: Int, col: Int, xFraction: Float): Int {
        val pal = theme.palette
        val roleColor = when (role) {
            KeyRole.ACTION -> pal.keyAction
            KeyRole.SPACE -> pal.keySpace
            KeyRole.EMOJI -> if (pal.emojiKeyAccent) ThemeColor.blend(pal.keyFunction, pal.accent, 0.25f) else pal.keyFunction
            KeyRole.FUNCTION, KeyRole.SHIFT -> pal.keyFunction
            KeyRole.CHARACTER -> pal.keySurface
        }
        val colors = pal.strategyColors
        if (colors.isEmpty()) return roleColor

        return when (theme.colorStrategy) {
            ColorStrategy.SINGLE -> roleColor
            ColorStrategy.ACCENT_SPECIAL_ONLY ->
                if (role == KeyRole.FUNCTION || role == KeyRole.SHIFT) colors[0] else roleColor
            ColorStrategy.ACCENT_VOWELS ->
                if (role == KeyRole.CHARACTER && key.primaryText.lowercase() in VOWELS) colors[0] else roleColor
            ColorStrategy.ALTERNATING ->
                if (role == KeyRole.CHARACTER && (row + col) % 2 == 1) colors[0] else roleColor
            ColorStrategy.GRADIENT_BY_ROW ->
                if (role == KeyRole.CHARACTER) sample(colors, if (rowCount <= 1) 0f else row / (rowCount - 1f)) else roleColor
            ColorStrategy.RAINBOW_BY_KEY -> {
                if (role != KeyRole.CHARACTER) roleColor
                else {
                    // Controlled diagonal sweep: mostly left→right, nudged by row.
                    val x = xFraction
                    val y = if (rowCount <= 1) 0f else row / (rowCount - 1f)
                    sample(colors, (x * 0.78f + y * 0.22f).coerceIn(0f, 1f))
                }
            }
            ColorStrategy.PALETTE_SHUFFLE -> {
                // Number row stays calm so the colourful letters carry the personality.
                if (role != KeyRole.CHARACTER || key.primaryText.all { it.isDigit() }) roleColor
                else {
                    // Deterministic, and never the same colour as the left neighbour.
                    var idx = Math.floorMod(key.primaryText.hashCode() * 31 + row * 7, colors.size)
                    if (idx == (lastShuffleIdx[0]) && lastShuffleRow[0] == row) idx = (idx + 1) % colors.size
                    lastShuffleIdx[0] = idx; lastShuffleRow[0] = row
                    colors[idx]
                }
            }
        }
    }

    // Single-threaded layout build only (main thread); tiny state avoids adjacent duplicates.
    private val lastShuffleIdx = intArrayOf(-1)
    private val lastShuffleRow = intArrayOf(-1)

    /** Lower-edge tint for gaming under-glow (0 = none). */
    fun edgeTint(theme: KeyboardTheme, xFraction: Float): Int {
        if (theme.keyStyle.surface != SurfaceEffect.GAMING) return 0
        val colors = theme.palette.strategyColors
        if (colors.isEmpty()) return 0
        // Quantise to 12 steps so the spec cache stays small.
        val q = (xFraction.coerceIn(0f, 1f) * 12f).toInt() / 12f
        return sample(colors, q)
    }

    fun sample(colors: List<Int>, t: Float): Int {
        if (colors.size == 1) return colors[0]
        val pos = t.coerceIn(0f, 1f) * (colors.size - 1)
        val i = pos.toInt().coerceAtMost(colors.size - 2)
        return ThemeColor.blend(colors[i], colors[i + 1], pos - i)
    }
}
