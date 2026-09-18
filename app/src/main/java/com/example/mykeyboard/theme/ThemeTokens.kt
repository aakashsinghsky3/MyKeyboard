package com.example.mykeyboard.theme

/*
 * Design-token vocabulary for the keyboard theme engine.
 * Everything here is plain data: the renderer reads these values, it never branches on a
 * theme name.
 */

enum class ThemeCategory(val displayName: String) {
    SIMPLE("Simple"),
    PROFESSIONAL("Professional"),
    AESTHETIC("Soft & Aesthetic"),
    COLORFUL("Colorful"),
    FUN("Fun & Kids"),
    PREMIUM_3D("Premium 3D"),
    GAMING("Gaming"),
    CUSTOM("Custom")
}

/** Gallery filter chips. A theme can appear under several filters. */
enum class GalleryFilter(val displayName: String) {
    FEATURED("Featured"),
    SIMPLE("Simple"),
    PROFESSIONAL("Professional"),
    DARK("Dark"),
    COLORFUL("Colorful"),
    FUN("Fun"),
    THREE_D("3D"),
    GAMING("Gaming"),
    FAVORITES("Favorites"),
    CUSTOM("Custom")
}

/** How the key face is lit. Interpreted by [KeycapPainter]; purely data-driven. */
enum class SurfaceEffect {
    FLAT,        // single colour, no lighting
    MATTE_3D,    // faint vertical gradient + thin top rim light
    SOFT_3D,     // softer gradient, rounder rim light
    GLOSSY_3D,   // strong specular band on the upper face
    MECHANICAL,  // separate keycap top face inset inside a darker skirt
    BUBBLE,      // glossy dome highlight, very round
    NEUMORPHIC,  // light/dark twin shadows, face matches chassis
    GLASS,       // translucent face, bright hairline border, faked sheen (no blur)
    GAMING       // dark matte face with controlled accent glow on highlighted keys
}

/** Outline geometry of a key. */
enum class KeyShape {
    ROUNDED,     // corner radius from the style
    PILL,        // radius = half of the shorter side
    NOTCHED      // square with stepped "pixel" corners
}

/**
 * Visual press feedback, matched to the theme personality. Purely cosmetic: text is
 * committed on ACTION_DOWN before any of this is drawn. 3D styles additionally sink by
 * their depth tokens regardless of the effect chosen here.
 */
enum class KeyPressEffect(val id: String, val displayName: String) {
    NONE("none", "None"),
    SUBTLE("subtle", "Subtle"),   // small brightness change (simple themes)
    RIPPLE("ripple", "Ripple"),   // quick radial wash from the touch point (material)
    SCALE("scale", "Scale"),      // tiny scale-down (floating keys)
    POP("pop", "Pop"),            // soft bounce: scale down, overshoot back (kids)
    GLOW("glow", "Glow");         // accent glow ring (gaming)

    companion object {
        fun fromId(id: String?): KeyPressEffect? = entries.firstOrNull { it.id == id }
    }
}

/** Per-key colour distribution, computed once per layout build. */
enum class ColorStrategy {
    SINGLE,
    ALTERNATING,
    GRADIENT_BY_ROW,
    RAINBOW_BY_KEY,
    ACCENT_VOWELS,
    ACCENT_SPECIAL_ONLY,
    PALETTE_SHUFFLE    // deterministic pseudo-random pick from the theme palette
}

enum class BackgroundStyle {
    SOLID,
    VERTICAL_GRADIENT,
    DIAGONAL_GRADIENT,
    STARFIELD,       // lightweight procedural dots, computed once per size
    BUBBLES,
    GRID,
    CONFETTI
}

/** Semantic key role used to pick colours and treatment. */
enum class KeyRole {
    CHARACTER,
    FUNCTION,   // backspace, symbols, globe, comma/period
    SHIFT,
    ACTION,     // enter
    SPACE,
    EMOJI
}

/**
 * Geometry + material of a keycap, independent from colour. Themes ship a default style
 * and users may swap in another compatible one.
 *
 * All sizes are in dp and converted once when resolved.
 */
data class KeyStyle(
    val id: String,
    val displayName: String,
    val shape: KeyShape = KeyShape.ROUNDED,
    val surface: SurfaceEffect = SurfaceEffect.FLAT,
    val cornerRadiusDp: Float = 7f,
    /** Corner radius used for wide keys (space, enter, shift…). Null = same as [cornerRadiusDp]. */
    val wideCornerRadiusDp: Float? = null,
    val depthDp: Float = 0f,
    val pressedDepthDp: Float = 0f,
    val borderWidthDp: Float = 0f,
    /** 0..1 strength of top lighting. */
    val highlight: Float = 0f,
    /** 0..1 strength of face gradient (top lighter, bottom darker). */
    val gradient: Float = 0f,
    /** How much darker the side / lower edge is than the face (0..1). */
    val sideDarken: Float = 0.30f,
    val shadowAlpha: Float = 0f,
    val shadowSpreadDp: Float = 0f,
    val shadowOffsetDp: Float = 0f,
    /** Mechanical keycaps: how far the top face is inset inside the skirt. */
    val skirtInsetDp: Float = 0f,
    val pressedScale: Float = 1f,
    /** Face brightness change while pressed (-1..1). */
    val pressedBrightness: Float = -0.08f,
    val horizontalGapDp: Float = 3f,
    val verticalGapDp: Float = 2.5f,
    /** Face opacity (glass). */
    val faceAlpha: Float = 1f,
    /** Notch size for [KeyShape.NOTCHED]. */
    val notchDp: Float = 3f,
    /** Styles that only read well on dark chassis (glass, gaming glow). */
    val requiresDarkChassis: Boolean = false,
    /** Neumorphic faces blend with the chassis, so they need a mid-tone, non-image background. */
    val requiresSolidChassis: Boolean = false,
    /** Minimal/outline styles have almost no face fill: text must contrast with the chassis. */
    val transparentFace: Boolean = false,
    val defaultPressEffect: KeyPressEffect = KeyPressEffect.SUBTLE
) {
    val is3D: Boolean get() = depthDp > 0f
}

/** Suggestion strip treatment. */
enum class StripStyle {
    CHIPS,   // candidates sit on rounded chips
    PLAIN    // text only, thin dividers – quieter, for simple/professional themes
}

data class Typography(
    /** Relative to the key row height. */
    val letterScale: Float = 1f,
    /** A system family name: "sans-serif", "sans-serif-medium", "sans-serif-condensed". */
    val fontFamily: String = "sans-serif",
    val bold: Boolean = true,
    /** Light/rounded fun themes may nudge letter-spacing; kept tiny for readability. */
    val letterSpacing: Float = 0f,
    val allCapsLabels: Boolean = false
)

data class BackgroundSpec(
    val style: BackgroundStyle = BackgroundStyle.SOLID,
    val startColor: Int,
    val endColor: Int = startColor,
    /** Decoration colour for procedural patterns. */
    val patternColor: Int = 0,
    val patternDensity: Float = 1f
)
