package com.example.mykeyboard.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.example.mykeyboard.model.KeyboardTheme
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.sqrt

/**
 * Off-main-thread loading + analysis of the user's keyboard photo, with a process-wide
 * cache so re-creating the input view (rotation, theme change) never decodes twice.
 */
object KeyboardThemeManager {

    class PhotoInfo(val bitmap: Bitmap, val meanLuminance: Float, val busyness: Float)

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var cachedKey: String? = null
    @Volatile private var cached: PhotoInfo? = null

    private fun keyOf(path: String): String {
        val f = File(path)
        return "$path:${f.lastModified()}:${f.length()}"
    }

    /** Returns the cached photo synchronously if already decoded (no disk access on miss). */
    fun peek(path: String?): PhotoInfo? {
        if (path.isNullOrEmpty()) return null
        return if (cachedKey == keyOf(path)) cached else null
    }

    fun loadPhoto(path: String, onLoaded: (PhotoInfo?) -> Unit) {
        peek(path)?.let { onLoaded(it); return }
        executor.execute {
            val info = try { decode(path) } catch (_: Throwable) { null }
            if (info != null) {
                cached = info
                cachedKey = keyOf(path)
            }
            main.post { onLoaded(info) }
        }
    }

    private fun decode(path: String): PhotoInfo? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1080) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null

        // Tiny thumbnail for luminance statistics (done once per photo).
        val thumb = Bitmap.createScaledBitmap(bmp, 24, 12, true)
        var sum = 0.0
        var sumSq = 0.0
        val n = thumb.width * thumb.height
        for (y in 0 until thumb.height) for (x in 0 until thumb.width) {
            val l = ThemeColor.luminance(thumb.getPixel(x, y))
            sum += l; sumSq += l * l
        }
        if (thumb !== bmp) thumb.recycle()
        val mean = sum / n
        val std = sqrt((sumSq / n - mean * mean).coerceAtLeast(0.0))
        return PhotoInfo(bmp, mean.toFloat(), (std * 2.5).toFloat().coerceIn(0f, 1f))
    }

    /**
     * Chooses scrim colour and strength so the strip text and any see-through key styles stay
     * readable over the photo. [userVisibility] is the existing "photo visibility" slider (0.2..1).
     */
    fun scrimFor(theme: KeyboardTheme, photo: PhotoInfo, userVisibility: Float): Pair<Int, Float> {
        val ink = theme.palette.suggestionText
        val scrimColor = if (ThemeColor.isLight(ink)) 0xFF0B1020.toInt() else 0xFFF6F7FA.toInt()
        val userStrength = (1f - userVisibility * 0.7f).coerceIn(0.12f, 0.9f)
        // Approximate the photo as a flat colour with its mean luminance.
        val g = (sqrtLumToChannel(photo.meanLuminance)).coerceIn(0, 255)
        val photoColor = ThemeColor.argb(255, g, g, g)
        var strength = userStrength
        while (strength < 0.9f && ThemeColor.contrast(ink, ThemeColor.blend(photoColor, scrimColor, strength)) < 4.5) {
            strength += 0.05f
        }
        // Busy photos get a little extra calm.
        strength = (strength + photo.busyness * 0.12f).coerceAtMost(0.92f)
        return scrimColor to strength
    }

    private fun sqrtLumToChannel(lum: Float): Int {
        // Inverse of the sRGB transfer for a grey with the given relative luminance.
        val v = if (lum <= 0.0031308f) lum * 12.92f else (1.055f * Math.pow(lum.toDouble(), 1 / 2.4).toFloat() - 0.055f)
        return (v * 255f).toInt()
    }
}
