package com.example.mykeyboard.theme

import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.model.ThemePalette
import com.example.mykeyboard.theme.ThemeColor.hex

/**
 * The curated first set of 12 themes. Each one differs in button style, surface, depth,
 * spacing, typography, strip treatment and press effect – not only in colour.
 *
 * Pure data; no rendering logic lives here.
 */
object BuiltInThemes {

    // ---------------------------------------------------------------------------------------
    // SIMPLE
    // ---------------------------------------------------------------------------------------

    val CLEAN_WHITE = KeyboardTheme(
        id = "clean_white",
        displayName = "Minimal Light",
        category = ThemeCategory.SIMPLE,
        isDark = false,
        featured = true,
        description = "Crisp white keys, quiet everyday typing",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#EEF0F3"), hex("#E6E8EC")),
        palette = ThemePalette(
            keySurface = hex("#FFFFFF"),
            keyFunction = hex("#D3D7DE"),
            keyAction = hex("#1F1F1F"),
            keyText = hex("#1C1F24"),
            keyTextSecondary = hex("#80868F"),
            actionText = hex("#FFFFFF"),
            keyBottom = hex("#B9BEC7"),
            shadow = hex("#3A4150"),
            suggestionBg = hex("#EEF0F3"),
            suggestionText = hex("#1C1F24"),
            chipBg = hex("#FFFFFF"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#1C1F24"),
            ripple = hex("#C9CED6")
        ),
        keyStyle = KeyStyles.SIMPLE_ROUNDED,
        typography = Typography(fontFamily = "sans-serif", bold = false, letterScale = 1.02f),
        stripStyle = StripStyle.PLAIN
    )

    val SOFT_GRAY = KeyboardTheme(
        id = "soft_gray",
        displayName = "Soft Gray",
        category = ThemeCategory.SIMPLE,
        isDark = false,
        description = "Neutral, calm and easy on the eyes",
        background = BackgroundSpec(BackgroundStyle.SOLID, hex("#D0D3D8")),
        palette = ThemePalette(
            keySurface = hex("#E8EAED"),
            keyFunction = hex("#BCC0C7"),
            keyAction = hex("#56657A"),
            keyText = hex("#23272D"),
            keyTextSecondary = hex("#6B727C"),
            actionText = hex("#FFFFFF"),
            shadow = hex("#2E333A"),
            suggestionBg = hex("#D0D3D8"),
            suggestionText = hex("#23272D"),
            chipBg = hex("#E1E3E7"),
            popupBg = hex("#F3F4F6"),
            popupText = hex("#23272D"),
            ripple = hex("#AEB3BA")
        ),
        keyStyle = KeyStyles.SOFT_ROUNDED,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    /** Keeps the legacy default id so existing installs land here. */
    val AMOLED_MINIMAL = KeyboardTheme(
        id = "amoled_midnight",
        displayName = "AMOLED Minimal",
        category = ThemeCategory.SIMPLE,
        isDark = true,
        featured = true,
        description = "True black, soft 3D keys for night use",
        background = BackgroundSpec(BackgroundStyle.SOLID, hex("#000000")),
        palette = ThemePalette(
            keySurface = hex("#161616"),
            keyFunction = hex("#0B0B0B"),
            keyAction = hex("#EDEDED"),
            keySpace = hex("#161616"),
            keyText = hex("#F2F2F2"),
            keyTextSecondary = hex("#7C7C7C"),
            actionText = hex("#000000"),
            accent = hex("#EDEDED"),
            suggestionBg = hex("#000000"),
            suggestionText = hex("#EDEDED"),
            chipBg = hex("#111111"),
            popupBg = hex("#242424"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#2E2E2E")
        ),
        keyStyle = KeyStyles.SOFT_3D,
        typography = Typography(fontFamily = "sans-serif", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    // ---------------------------------------------------------------------------------------
    // PROFESSIONAL
    // ---------------------------------------------------------------------------------------

    val GRAPHITE = KeyboardTheme(
        id = "graphite",
        displayName = "Graphite",
        category = ThemeCategory.PROFESSIONAL,
        isDark = true,
        featured = false,
        description = "Floating graphite keys with a steel-blue accent",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#2A2C31"), hex("#222428")),
        palette = ThemePalette(
            keySurface = hex("#3C3F46"),
            keyFunction = hex("#303238"),
            keyAction = hex("#6E8BB8"),
            keyText = hex("#EDEFF2"),
            keyTextSecondary = hex("#9CA2AC"),
            actionText = hex("#FFFFFF"),
            shadow = hex("#000000"),
            suggestionBg = hex("#26282D"),
            suggestionText = hex("#E4E7EB"),
            chipBg = hex("#33363C"),
            popupBg = hex("#474B53"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#555A63")
        ),
        keyStyle = KeyStyles.FLOATING,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    val EXECUTIVE_NAVY = KeyboardTheme(
        id = "executive_navy",
        displayName = "Executive Navy",
        category = ThemeCategory.PROFESSIONAL,
        isDark = true,
        description = "Deep navy with hairline borders and a muted gold Enter",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#15223B"), hex("#101A2F")),
        palette = ThemePalette(
            keySurface = hex("#213050"),
            keyFunction = hex("#1A2742"),
            keyAction = hex("#C8A45C"),
            keyText = hex("#EEF2F8"),
            keyTextSecondary = hex("#93A2BD"),
            actionText = hex("#14213A"),
            accent = hex("#C8A45C"),
            keyBorder = hex("#2E4068"),
            shadow = hex("#050A14"),
            suggestionBg = hex("#131F36"),
            suggestionText = hex("#E6EBF4"),
            chipBg = hex("#1C2A47"),
            popupBg = hex("#2A3B60"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#34486F")
        ),
        keyStyle = KeyStyles.SOFT_ROUNDED.copy(borderWidthDp = 0.8f, cornerRadiusDp = 9f, wideCornerRadiusDp = 10f),
        typography = Typography(fontFamily = "sans-serif", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    // ---------------------------------------------------------------------------------------
    // SOFT / COLORFUL
    // ---------------------------------------------------------------------------------------

    val LAVENDER = KeyboardTheme(
        id = "lavender",
        displayName = "Lavender",
        category = ThemeCategory.AESTHETIC,
        isDark = false,
        featured = false,
        description = "Soft lilac with gentle depth",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#F2EDFA"), hex("#E7DFF5")),
        palette = ThemePalette(
            keySurface = hex("#FFFFFF"),
            keyFunction = hex("#E2D7F4"),
            keyAction = hex("#8565D0"),
            keyText = hex("#3A2F57"),
            keyTextSecondary = hex("#9788B5"),
            actionText = hex("#FFFFFF"),
            keyBottom = hex("#D5C9EA"),
            shadow = hex("#4B347F"),
            suggestionBg = hex("#EFE8F9"),
            suggestionText = hex("#3A2F57"),
            chipBg = hex("#FFFFFF"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#3A2F57"),
            ripple = hex("#D9CCF0")
        ),
        keyStyle = KeyStyles.SOFT_3D,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false)
    )

    val PASTEL_RAINBOW = KeyboardTheme(
        id = "pastel_rainbow",
        displayName = "Pastel Rainbow",
        category = ThemeCategory.COLORFUL,
        isDark = false,
        featured = false,
        description = "A gentle rainbow sweep across the keys",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#FCFAF6"), hex("#F4F0EA")),
        palette = ThemePalette(
            keySurface = hex("#FFFFFF"),
            keyFunction = hex("#ECE7EF"),
            keyAction = hex("#6A5FE8"),
            keyText = hex("#2E2A3A"),
            keyTextSecondary = hex("#6F6A7C"),
            actionText = hex("#FFFFFF"),
            shadow = hex("#6B5E7A"),
            suggestionBg = hex("#FAF7F2"),
            suggestionText = hex("#2E2A3A"),
            chipBg = hex("#FFFFFF"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#2E2A3A"),
            ripple = hex("#E4DDEA"),
            strategyColors = listOf(
                hex("#FFB8B8"), hex("#FFD3A8"), hex("#FFEDA8"), hex("#C9EFB8"),
                hex("#B8E4F4"), hex("#C8CBFF"), hex("#E5C6F4")
            )
        ),
        keyStyle = KeyStyles.SOFT_3D.copy(depthDp = 2f, highlight = 0.45f, sideDarken = 0.12f),
        colorStrategy = ColorStrategy.RAINBOW_BY_KEY,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false)
    )

    // ---------------------------------------------------------------------------------------
    // FUN / KIDS
    // ---------------------------------------------------------------------------------------

    val CANDY_BUBBLE = KeyboardTheme(
        id = "candy_bubble",
        displayName = "Candy Bubble",
        category = ThemeCategory.FUN,
        isDark = false,
        featured = true,
        description = "Glossy candy buttons that bounce",
        background = BackgroundSpec(
            BackgroundStyle.BUBBLES, hex("#FFE8F2"), hex("#F1E4FF"),
            patternColor = ThemeColor.withAlpha(hex("#FFFFFF"), 0.55f)
        ),
        palette = ThemePalette(
            keySurface = hex("#FFFFFF"),
            keyFunction = hex("#FFFFFF"),
            keyAction = hex("#E94A8A"),
            keyText = hex("#3A2342"),
            keyTextSecondary = hex("#8B6C92"),
            actionText = hex("#FFFFFF"),
            accent = hex("#E94A8A"),
            shadow = hex("#7A3A6E"),
            suggestionBg = ThemeColor.withAlpha(hex("#FFFFFF"), 0.55f),
            suggestionText = hex("#3A2342"),
            chipBg = hex("#FFFFFF"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#3A2342"),
            ripple = hex("#FFC2DA"),
            strategyColors = listOf(hex("#FFA6C9"), hex("#FFD57E"), hex("#A3DBFF"), hex("#B9EDA9"), hex("#D4B9FF")),
            emojiKeyAccent = true
        ),
        keyStyle = KeyStyles.BUBBLE,
        colorStrategy = ColorStrategy.PALETTE_SHUFFLE,
        typography = Typography(fontFamily = "sans-serif", bold = true, letterScale = 1.02f)
    )

    val SPACE = KeyboardTheme(
        id = "space",
        displayName = "Space",
        category = ThemeCategory.FUN,
        isDark = true,
        featured = false,
        description = "Starry night sky with planet-blue accents",
        background = BackgroundSpec(
            BackgroundStyle.STARFIELD, hex("#0F0C2E"), hex("#211857"),
            patternColor = hex("#FFFFFF")
        ),
        palette = ThemePalette(
            keySurface = hex("#2A2463"),
            keyFunction = hex("#3F3290"),
            keyAction = hex("#5CC8F5"),
            keyText = hex("#F4F1FF"),
            keyTextSecondary = hex("#B3A7EE"),
            actionText = hex("#0F0C2E"),
            accent = hex("#5CC8F5"),
            shadow = hex("#05031A"),
            suggestionBg = ThemeColor.withAlpha(hex("#0F0C2E"), 0.6f),
            suggestionText = hex("#F4F1FF"),
            chipBg = hex("#2A2463"),
            popupBg = hex("#3A3285"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#4A3FA0"),
            strategyColors = listOf(hex("#3B3A9A")),
            emojiKeyAccent = true
        ),
        keyStyle = KeyStyles.CHUNKY_ROUNDED.copy(depthDp = 3.5f, sideDarken = 0.45f),
        colorStrategy = ColorStrategy.ACCENT_VOWELS,
        typography = Typography(fontFamily = "sans-serif", bold = true)
    )

    // ---------------------------------------------------------------------------------------
    // PREMIUM 3D
    // ---------------------------------------------------------------------------------------

    val WHITE_MECHANICAL = KeyboardTheme(
        id = "white_mechanical",
        displayName = "White Mechanical",
        category = ThemeCategory.PREMIUM_3D,
        isDark = false,
        featured = true,
        description = "Off-white desktop keycaps with real travel",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#E3E5E9"), hex("#D6D9DE")),
        palette = ThemePalette(
            keySurface = hex("#FAFAFB"),
            keyFunction = hex("#E4E6EA"),
            keyAction = hex("#3F66C9"),
            keyText = hex("#24272D"),
            keyTextSecondary = hex("#7D838D"),
            actionText = hex("#FFFFFF"),
            keyBottom = hex("#B5BAC3"),
            shadow = hex("#2B3140"),
            suggestionBg = hex("#E3E5E9"),
            suggestionText = hex("#24272D"),
            chipBg = hex("#F4F5F7"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#24272D"),
            ripple = hex("#C8CCD3")
        ),
        keyStyle = KeyStyles.MECHANICAL,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    val BLACK_MECHANICAL = KeyboardTheme(
        id = "black_mechanical",
        displayName = "Black Mechanical",
        category = ThemeCategory.PREMIUM_3D,
        isDark = true,
        description = "Charcoal chassis, dark keycaps, amber Enter",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#1C1D20"), hex("#141518")),
        palette = ThemePalette(
            keySurface = hex("#2E3034"),
            keyFunction = hex("#25272A"),
            keyAction = hex("#B8894A"),
            keyText = hex("#F1F2F4"),
            keyTextSecondary = hex("#999DA5"),
            actionText = hex("#17181B"),
            accent = hex("#B8894A"),
            keyBottom = hex("#0E0F11"),
            shadow = hex("#000000"),
            suggestionBg = hex("#18191C"),
            suggestionText = hex("#E9EAEC"),
            chipBg = hex("#26282B"),
            popupBg = hex("#3A3C41"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#44474D")
        ),
        keyStyle = KeyStyles.MECHANICAL.copy(highlight = 0.55f, sideDarken = 0.55f),
        typography = Typography(fontFamily = "sans-serif-medium", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    // ---------------------------------------------------------------------------------------
    // GAMING
    // ---------------------------------------------------------------------------------------

    val RGB_GAMING = KeyboardTheme(
        id = "rgb_gaming",
        displayName = "RGB Gaming",
        category = ThemeCategory.GAMING,
        isDark = true,
        featured = true,
        description = "Raised black keys with a soft RGB under-glow",
        background = BackgroundSpec(BackgroundStyle.DIAGONAL_GRADIENT, hex("#0A0B0F"), hex("#12141B")),
        palette = ThemePalette(
            keySurface = hex("#1C1E25"),
            keyFunction = hex("#16181E"),
            keyAction = hex("#22D3EE"),
            keyText = hex("#E9EDF5"),
            keyTextSecondary = hex("#7E8799"),
            actionText = hex("#051218"),
            accent = hex("#22D3EE"),
            shadow = hex("#000000"),
            suggestionBg = hex("#0C0D12"),
            suggestionText = hex("#E9EDF5"),
            chipBg = hex("#171920"),
            popupBg = hex("#262933"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#2E3340"),
            // Under-glow sweep for the lower edges (controlled, left→right).
            strategyColors = listOf(hex("#22D3EE"), hex("#6D7CFF"), hex("#B15CFF"), hex("#FF4D9D"))
        ),
        keyStyle = KeyStyles.RAISED_GAMING,
        typography = Typography(fontFamily = "sans-serif-condensed", bold = true, letterScale = 1.04f),
        stripStyle = StripStyle.PLAIN
    )


    // ---------------------------------------------------------------------------------------
    // SIGNATURE + CLASSIC SET
    // ---------------------------------------------------------------------------------------

    /** Brand theme: charcoal keys floating on a charcoal chassis with amber actions. */
    val SIGNATURE = KeyboardTheme(
        id = "signature_amber",
        displayName = "Charcoal Amber",
        category = ThemeCategory.PROFESSIONAL,
        isDark = true,
        featured = true,
        description = "The signature look: charcoal keys, amber actions",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#1C1C1C"), hex("#141414")),
        palette = ThemePalette(
            keySurface = hex("#2B2B2B"),
            keyFunction = hex("#212121"),
            keyAction = hex("#FFB020"),
            keyText = hex("#F5F5F5"),
            keyTextSecondary = hex("#A39E98"),
            actionText = hex("#151515"),
            accent = hex("#FFB020"),
            shadow = hex("#000000"),
            suggestionBg = hex("#171717"),
            suggestionText = hex("#F0EEEC"),
            chipBg = hex("#242424"),
            popupBg = hex("#333333"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#3D3D3D")
        ),
        keyStyle = KeyStyles.FLOATING.copy(cornerRadiusDp = 8f, wideCornerRadiusDp = 9f),
        typography = Typography(fontFamily = "sans-serif-medium", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    val MIDNIGHT = KeyboardTheme(
        id = "midnight_dark",
        displayName = "Midnight Dark",
        category = ThemeCategory.SIMPLE,
        isDark = true,
        featured = true,
        description = "Deep night tones with a warm accent",
        background = BackgroundSpec(BackgroundStyle.VERTICAL_GRADIENT, hex("#15171C"), hex("#101216")),
        palette = ThemePalette(
            keySurface = hex("#262A33"),
            keyFunction = hex("#1C1F26"),
            keyAction = hex("#E9B44C"),
            keyText = hex("#ECEEF2"),
            keyTextSecondary = hex("#8B919C"),
            actionText = hex("#15171C"),
            shadow = hex("#000000"),
            suggestionBg = hex("#13151A"),
            suggestionText = hex("#E6E8EC"),
            chipBg = hex("#20232A"),
            popupBg = hex("#30343E"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#3A3F4A")
        ),
        keyStyle = KeyStyles.SOFT_ROUNDED.copy(cornerRadiusDp = 9f, wideCornerRadiusDp = 10f),
        typography = Typography(fontFamily = "sans-serif", bold = false),
        stripStyle = StripStyle.PLAIN
    )

    val OCEAN = KeyboardTheme(
        id = "ocean",
        displayName = "Ocean",
        category = ThemeCategory.COLORFUL,
        isDark = true,
        featured = true,
        description = "Deep sea teal with gentle bubbles",
        background = BackgroundSpec(
            BackgroundStyle.BUBBLES, hex("#0C3E4A"), hex("#072833"),
            patternColor = ThemeColor.withAlpha(hex("#FFFFFF"), 0.07f)
        ),
        palette = ThemePalette(
            keySurface = hex("#135567"),
            keyFunction = hex("#0E4555"),
            keyAction = hex("#34C6B8"),
            keyText = hex("#EAF8F9"),
            keyTextSecondary = hex("#93CBD1"),
            actionText = hex("#062A30"),
            shadow = hex("#021418"),
            suggestionBg = ThemeColor.withAlpha(hex("#072833"), 0.7f),
            suggestionText = hex("#EAF8F9"),
            chipBg = hex("#0F4A59"),
            popupBg = hex("#1A6477"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#1F6E80")
        ),
        keyStyle = KeyStyles.SOFT_3D.copy(depthDp = 2f, sideDarken = 0.32f, highlight = 0.35f),
        typography = Typography(fontFamily = "sans-serif-medium", bold = false)
    )

    val PURPLE = KeyboardTheme(
        id = "purple",
        displayName = "Purple",
        category = ThemeCategory.COLORFUL,
        isDark = true,
        featured = true,
        description = "Rich plum with a soft violet glow on Enter",
        background = BackgroundSpec(BackgroundStyle.DIAGONAL_GRADIENT, hex("#251535"), hex("#170D22")),
        palette = ThemePalette(
            keySurface = hex("#3A2553"),
            keyFunction = hex("#2D1C42"),
            keyAction = hex("#B38BFF"),
            keyText = hex("#F5EDFF"),
            keyTextSecondary = hex("#B9A3D6"),
            actionText = hex("#1A0F26"),
            shadow = hex("#0A0410"),
            suggestionBg = hex("#1E1129"),
            suggestionText = hex("#F2E9FF"),
            chipBg = hex("#301E45"),
            popupBg = hex("#4A3068"),
            popupText = hex("#FFFFFF"),
            ripple = hex("#553A78")
        ),
        keyStyle = KeyStyles.MATERIAL,
        typography = Typography(fontFamily = "sans-serif-medium", bold = false)
    )

    val CLASSIC = KeyboardTheme(
        id = "classic",
        displayName = "Classic",
        category = ThemeCategory.SIMPLE,
        isDark = false,
        featured = true,
        description = "The familiar phone keyboard, crisp and neutral",
        background = BackgroundSpec(BackgroundStyle.SOLID, hex("#D2D5DA")),
        palette = ThemePalette(
            keySurface = hex("#FFFFFF"),
            keyFunction = hex("#ADB3BC"),
            keyAction = hex("#3C4350"),
            keyText = hex("#111418"),
            keyTextSecondary = hex("#6C727B"),
            actionText = hex("#FFFFFF"),
            keyBottom = hex("#8F959E"),
            shadow = hex("#2A2F36"),
            suggestionBg = hex("#D2D5DA"),
            suggestionText = hex("#111418"),
            chipBg = hex("#E4E6EA"),
            popupBg = hex("#FFFFFF"),
            popupText = hex("#111418"),
            ripple = hex("#BFC4CB")
        ),
        keyStyle = KeyStyles.SIMPLE_ROUNDED.copy(cornerRadiusDp = 5f, wideCornerRadiusDp = 5f),
        typography = Typography(fontFamily = "sans-serif", bold = false, letterScale = 1.03f),
        stripStyle = StripStyle.PLAIN
    )

    // ---------------------------------------------------------------------------------------

    val ALL: List<KeyboardTheme> = listOf(
        SIGNATURE, CLEAN_WHITE, MIDNIGHT, AMOLED_MINIMAL, OCEAN, PURPLE, CLASSIC,
        SOFT_GRAY,
        GRAPHITE, EXECUTIVE_NAVY,
        LAVENDER, PASTEL_RAINBOW,
        CANDY_BUBBLE, SPACE,
        WHITE_MECHANICAL, BLACK_MECHANICAL,
        RGB_GAMING
    )

    val DEFAULT_LIGHT: KeyboardTheme get() = LAVENDER
    val DEFAULT_DARK: KeyboardTheme get() = AMOLED_MINIMAL
    val DEFAULT: KeyboardTheme get() = AMOLED_MINIMAL

    /** Pre-theme-engine ids mapped to their closest curated successor. */
    private val LEGACY_IDS = mapOf(
        "material_dark" to "graphite",
        "gboard_peach" to "lavender",
        "cyber_neon" to "rgb_gaming",
        "emerald_green" to "graphite",
        "royal_purple" to "space",
        "pastel_lavender" to "lavender",
        "sunset_glow" to "black_mechanical",
        "royal_gold" to "black_mechanical",
        "snow_light" to "clean_white"
    )

    fun byId(id: String?): KeyboardTheme? {
        if (id == null) return null
        val resolved = LEGACY_IDS[id] ?: id
        return ALL.firstOrNull { it.id == resolved }
    }

    fun matches(theme: KeyboardTheme, filter: GalleryFilter): Boolean = when (filter) {
        GalleryFilter.FEATURED -> theme.featured
        GalleryFilter.SIMPLE -> theme.category == ThemeCategory.SIMPLE
        GalleryFilter.PROFESSIONAL -> theme.category == ThemeCategory.PROFESSIONAL
        GalleryFilter.DARK -> theme.isDark
        GalleryFilter.COLORFUL -> theme.category == ThemeCategory.COLORFUL || theme.category == ThemeCategory.AESTHETIC
        GalleryFilter.FUN -> theme.category == ThemeCategory.FUN
        GalleryFilter.THREE_D -> theme.keyStyle.depthDp >= 2f
        GalleryFilter.GAMING -> theme.category == ThemeCategory.GAMING
        GalleryFilter.CUSTOM -> theme.isCustom
        GalleryFilter.FAVORITES -> false // resolved by the repository
    }
}
