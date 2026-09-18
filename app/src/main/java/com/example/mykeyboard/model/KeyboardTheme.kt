package com.example.mykeyboard.model

import com.example.mykeyboard.theme.BackgroundSpec
import com.example.mykeyboard.theme.BuiltInThemes
import com.example.mykeyboard.theme.ColorStrategy
import com.example.mykeyboard.theme.KeyPressEffect
import com.example.mykeyboard.theme.KeyStyle
import com.example.mykeyboard.theme.StripStyle
import com.example.mykeyboard.theme.ThemeCategory
import com.example.mykeyboard.theme.ThemeColor
import com.example.mykeyboard.theme.Typography

/**
 * Colour tokens of a theme. Per-role key colours; the renderer derives lower edges,
 * highlights and pressed shades from these unless explicitly overridden.
 */
data class ThemePalette(
    val keySurface: Int,
    val keyFunction: Int,
    val keyAction: Int,
    val keySpace: Int = keySurface,
    val keyText: Int,
    val keyTextSecondary: Int,
    val actionText: Int,
    val accent: Int = keyAction,
    /** Optional explicit lower-edge colour for 3D styles (0 = derive from face). */
    val keyBottom: Int = 0,
    val keyBorder: Int = 0,
    val shadow: Int = 0xFF000000.toInt(),
    val suggestionBg: Int,
    val suggestionText: Int,
    val chipBg: Int = keyFunction,
    val popupBg: Int,
    val popupText: Int,
    val ripple: Int,
    /** Extra palette used by colourful [ColorStrategy] values. */
    val strategyColors: List<Int> = emptyList(),
    /** Emoji key uses the accent colour (fun themes). */
    val emojiKeyAccent: Boolean = false
)

/**
 * A complete, immutable visual theme.
 *
 * Replaces the old enum. The legacy colour property names (backgroundColor, keyNormalColor…)
 * are kept as read-only aliases so EmojiKeyboardView / ClipboardView / the IME service keep
 * working unchanged.
 */
data class KeyboardTheme(
    val id: String,
    val displayName: String,
    val category: ThemeCategory,
    val isDark: Boolean,
    val featured: Boolean = false,
    val background: BackgroundSpec,
    val palette: ThemePalette,
    val keyStyle: KeyStyle,
    val colorStrategy: ColorStrategy = ColorStrategy.SINGLE,
    val pressEffect: KeyPressEffect = keyStyle.defaultPressEffect,
    val typography: Typography = Typography(),
    val stripStyle: StripStyle = StripStyle.CHIPS,
    val description: String = "",
    val isCustom: Boolean = false
) {
    // ---- Legacy aliases (pre-theme-engine API) ----
    val backgroundColor: Int get() = background.startColor
    val keyNormalColor: Int get() = palette.keySurface
    val keySpecialColor: Int get() = palette.keyFunction
    val keyActionColor: Int get() = palette.keyAction
    val keySpaceColor: Int get() = palette.keySpace
    val textColorPrimary: Int get() = palette.keyText
    val textColorSecondary: Int get() = palette.keyTextSecondary
    val actionTextColor: Int get() = palette.actionText
    val suggestionBgColor: Int get() = palette.suggestionBg
    val suggestionTextColor: Int get() = palette.suggestionText
    val popupBgColor: Int get() = palette.popupBg
    val popupTextColor: Int get() = palette.popupText
    val rippleColor: Int get() = palette.ripple

    /** Average chassis colour, used for nav bar tint and contrast checks. */
    val chassisColor: Int get() = ThemeColor.blend(background.startColor, background.endColor, 0.5f)

    /** Returns a copy using [style] if it stays readable with this theme, otherwise this theme. */
    fun withKeyStyle(style: KeyStyle?): KeyboardTheme {
        if (style == null || style.id == keyStyle.id) return this
        if (!isStyleCompatible(style)) return this
        return copy(keyStyle = style, pressEffect = style.defaultPressEffect)
    }

    /** Replaces the action/accent colour, keeping labels readable. 0 = unchanged. */
    fun withAccent(accent: Int): KeyboardTheme {
        if (accent == 0 || accent == palette.keyAction) return this
        val ink = if (ThemeColor.contrast(0xFF151515.toInt(), accent) >= ThemeColor.contrast(0xFFFFFFFF.toInt(), accent)) 0xFF151515.toInt() else 0xFFFFFFFF.toInt()
        return copy(palette = palette.copy(keyAction = accent, accent = accent, actionText = ink))
    }

    /** Scales visual key gaps (never the touch targets). */
    fun withKeySpacing(scale: Float): KeyboardTheme {
        if (scale == 1f) return this
        return copy(keyStyle = keyStyle.copy(
            horizontalGapDp = (keyStyle.horizontalGapDp * scale).coerceIn(1f, 5f),
            verticalGapDp = (keyStyle.verticalGapDp * scale).coerceIn(1f, 5f)
        ))
    }

    fun isStyleCompatible(style: KeyStyle): Boolean {
        val chassis = chassisColor
        if (style.requiresDarkChassis && ThemeColor.isLight(chassis)) return false
        if (style.requiresSolidChassis) {
            // Neumorphism only reads when key faces are close to the chassis tone.
            if (ThemeColor.contrast(palette.keySurface, chassis) > 1.6) return false
            if (colorStrategy != ColorStrategy.SINGLE) return false
        }
        if (style.transparentFace && ThemeColor.contrast(palette.keyText, chassis) < 4.5) return false
        return true
    }

    companion object {
        val DEFAULT: KeyboardTheme get() = BuiltInThemes.DEFAULT
        /** Kept for source compatibility with existing callers. */
        val AMOLED_MIDNIGHT: KeyboardTheme get() = BuiltInThemes.DEFAULT

        fun fromId(id: String?): KeyboardTheme = BuiltInThemes.byId(id) ?: BuiltInThemes.DEFAULT
    }
}
