package com.example.mykeyboard.theme

import kotlin.math.sqrt

/**
 * Procedural, very light decorative backgrounds. Generated once per size into a flat
 * FloatArray of circles: [x, y, radius, alpha(0..1)] repeated. Drawing ~40–90 circles
 * into a static background display list costs far less than a bitmap and never runs
 * during typing (key presses only invalidate the key views).
 */
object BackgroundPattern {

    fun generate(style: BackgroundStyle, width: Float, height: Float, density: Float, amount: Float = 1f): FloatArray {
        if (width <= 0f || height <= 0f) return FloatArray(0)
        val rnd = Lcg(seed = (width.toInt() * 31 + height.toInt()).toLong() xor style.ordinal.toLong())
        val areaDp = (width / density) * (height / density)
        return when (style) {
            BackgroundStyle.STARFIELD -> {
                val count = (areaDp / 1500f * amount).toInt().coerceIn(20, 140)
                FloatArray(count * 4).also { out ->
                    for (i in 0 until count) {
                        val big = rnd.next() > 0.92f
                        out[i * 4] = rnd.next() * width
                        out[i * 4 + 1] = rnd.next() * height
                        out[i * 4 + 2] = (if (big) 1.3f + rnd.next() * 0.6f else 0.5f + rnd.next() * 0.6f) * density
                        out[i * 4 + 3] = if (big) 0.85f else 0.25f + rnd.next() * 0.45f
                    }
                }
            }
            BackgroundStyle.BUBBLES -> {
                val count = (areaDp / 9000f * amount).toInt().coerceIn(6, 26)
                FloatArray(count * 4).also { out ->
                    for (i in 0 until count) {
                        out[i * 4] = rnd.next() * width
                        out[i * 4 + 1] = rnd.next() * height
                        out[i * 4 + 2] = (10f + rnd.next() * 34f) * density
                        out[i * 4 + 3] = 0.35f + rnd.next() * 0.45f
                    }
                }
            }
            BackgroundStyle.CONFETTI -> {
                val count = (areaDp / 2500f * amount).toInt().coerceIn(12, 80)
                FloatArray(count * 4).also { out ->
                    for (i in 0 until count) {
                        out[i * 4] = rnd.next() * width
                        out[i * 4 + 1] = rnd.next() * height
                        out[i * 4 + 2] = (1.5f + rnd.next() * 2f) * density
                        out[i * 4 + 3] = 0.5f + rnd.next() * 0.4f
                    }
                }
            }
            BackgroundStyle.GRID -> {
                // Dot grid: regular spacing, no randomness.
                val step = 18f * density
                val cols = (width / step).toInt() + 1
                val rows = (height / step).toInt() + 1
                val n = cols * rows
                FloatArray(n * 4).also { out ->
                    var k = 0
                    for (r in 0 until rows) for (c in 0 until cols) {
                        out[k] = c * step + step / 2f
                        out[k + 1] = r * step + step / 2f
                        out[k + 2] = 0.9f * density
                        out[k + 3] = 0.5f
                        k += 4
                    }
                }
            }
            else -> FloatArray(0)
        }
    }

    /** Estimated perceived brightness of a pattern layer, for readability checks. */
    fun coverage(points: FloatArray, width: Float, height: Float): Float {
        var area = 0f
        var i = 0
        while (i < points.size) { val r = points[i + 2]; area += 3.14159f * r * r * points[i + 3]; i += 4 }
        return sqrt((area / (width * height)).coerceIn(0f, 1f))
    }

    private class Lcg(seed: Long) {
        private var state = seed xor 0x5DEECE66DL
        fun next(): Float {
            state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
            return (state ushr 24).toFloat() / (1L shl 24).toFloat()
        }
    }
}
