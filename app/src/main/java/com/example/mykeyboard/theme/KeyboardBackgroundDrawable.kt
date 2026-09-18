package com.example.mykeyboard.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Keyboard chassis: solid / gradient / light procedural pattern, or a user photo with an
 * automatically chosen readability scrim. Recorded into the view's static display list;
 * key presses never redraw it.
 */
class KeyboardBackgroundDrawable(
    private val spec: BackgroundSpec,
    private val density: Float,
    private val photo: Bitmap? = null,
    /** 0..1 – how strongly the photo is dimmed/lightened. */
    private val scrimStrength: Float = 0.5f,
    /** Scrim colour (dark for light-text themes, light for dark-text themes). */
    private val scrimColor: Int = 0xFF0F172A.toInt()
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint()
    private val matrix = Matrix()
    private var shader: Shader? = null
    private var points = FloatArray(0)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        shader = when {
            spec.startColor == spec.endColor -> null
            spec.style == BackgroundStyle.DIAGONAL_GRADIENT -> LinearGradient(0f, 0f, w, h, spec.startColor, spec.endColor, Shader.TileMode.CLAMP)
            else -> LinearGradient(0f, 0f, 0f, h, spec.startColor, spec.endColor, Shader.TileMode.CLAMP)
        }
        points = if (photo == null) BackgroundPattern.generate(spec.style, w, h, density, spec.patternDensity) else FloatArray(0)
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val bmp = photo
        if (bmp != null && !bmp.isRecycled) {
            val scale = maxOf(w / bmp.width, h / bmp.height)
            matrix.setScale(scale, scale)
            matrix.postTranslate(b.left + (w - bmp.width * scale) / 2f, b.top + (h - bmp.height * scale) / 2f)
            canvas.save()
            canvas.clipRect(b)
            paint.shader = null
            paint.alpha = 255
            canvas.drawBitmap(bmp, matrix, paint)
            canvas.restore()
            scrimPaint.color = ThemeColor.withAlpha(scrimColor, scrimStrength)
            canvas.drawRect(b, scrimPaint)
            return
        }
        paint.shader = shader
        paint.color = spec.startColor
        canvas.drawRect(b, paint)
        paint.shader = null
        if (points.isNotEmpty() && spec.patternColor != 0) {
            val baseA = ThemeColor.alpha(spec.patternColor)
            var i = 0
            while (i < points.size) {
                dotPaint.color = ThemeColor.withAlpha(spec.patternColor, (baseA * points[i + 3]).toInt())
                canvas.drawCircle(b.left + points[i], b.top + points[i + 1], points[i + 2], dotPaint)
                i += 4
            }
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
