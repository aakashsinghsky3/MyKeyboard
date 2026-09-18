package com.example.mykeyboard.theme

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Thin vertical divider on the leading edge of a plain-style suggestion candidate. */
class StripDividerDrawable(color: Int, private val widthPx: Float, private val heightPx: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val cy = b.exactCenterY()
        canvas.drawRect(b.left.toFloat(), cy - heightPx / 2f, b.left + widthPx, cy + heightPx / 2f, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
