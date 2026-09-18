package com.example.mykeyboard.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure-Kotlin ARGB colour math used by the theme engine.
 *
 * Deliberately free of android.* imports so theme resolution can be unit-tested on the JVM
 * and reused by the off-device preview renderer. All work here happens when a theme is
 * resolved (theme change / layout rebuild), never inside a touch handler.
 */
object ThemeColor {

    fun hex(value: String): Int {
        val s = value.removePrefix("#")
        val v = s.toLong(16)
        return when (s.length) {
            6 -> (0xFF000000 or v).toInt()
            8 -> v.toInt()
            else -> throw IllegalArgumentException("Bad colour: $value")
        }
    }

    fun alpha(c: Int) = (c ushr 24) and 0xFF
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun withAlpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    fun withAlpha(c: Int, fraction: Float): Int = withAlpha(c, (fraction * 255).roundToInt())

    /** Linear blend from [a] to [b]; ratio 0 = a, 1 = b. Alpha is blended too. */
    fun blend(a: Int, b: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val inv = 1f - t
        return argb(
            (alpha(a) * inv + alpha(b) * t).roundToInt(),
            (red(a) * inv + red(b) * t).roundToInt(),
            (green(a) * inv + green(b) * t).roundToInt(),
            (blue(a) * inv + blue(b) * t).roundToInt()
        )
    }

    fun lighten(c: Int, amount: Float): Int = withAlpha(blend(c, 0xFFFFFFFF.toInt(), amount), alpha(c))
    fun darken(c: Int, amount: Float): Int = withAlpha(blend(c, 0xFF000000.toInt(), amount), alpha(c))

    /** Positive = brighter, negative = darker. */
    fun shift(c: Int, amount: Float): Int = if (amount >= 0f) lighten(c, amount) else darken(c, -amount)

    /** Composites a translucent [top] colour over an opaque [bottom] colour. */
    fun over(top: Int, bottom: Int): Int {
        val a = alpha(top) / 255f
        return withAlpha(blend(bottom, withAlpha(top, 255), a), 255)
    }

    private fun channel(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG relative luminance (0..1). */
    fun luminance(c: Int): Double = 0.2126 * channel(red(c)) + 0.7152 * channel(green(c)) + 0.0722 * channel(blue(c))

    /** WCAG contrast ratio (1..21). Both colours treated as opaque. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    fun isLight(c: Int): Boolean = luminance(c) > 0.40

    /**
     * Returns [preferred] if it reaches [minContrast] against [background]; otherwise the
     * better of a near-white / near-black ink. Used for per-key automatic label colours.
     */
    fun readableOn(background: Int, preferred: Int, minContrast: Double = 4.5): Int {
        val bg = withAlpha(background, 255)
        if (contrast(withAlpha(preferred, 255), bg) >= minContrast) return preferred
        val light = 0xFFFFFFFF.toInt()
        val dark = 0xFF1A1C22.toInt()
        return if (contrast(light, bg) >= contrast(dark, bg)) light else dark
    }
}
