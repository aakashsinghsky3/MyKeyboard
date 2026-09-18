package com.example.mykeyboard.theme

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.mykeyboard.R
import com.example.mykeyboard.model.KeyLayoutHelper
import com.example.mykeyboard.model.KeyModel
import com.example.mykeyboard.model.KeyType
import com.example.mykeyboard.model.KeyboardLanguage
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.model.ShiftState

/**
 * Miniature, pixel-faithful keyboard preview used by the theme gallery. It renders with the
 * exact same [KeycapResolver] + [KeycapDrawable] pipeline as the real keyboard, scaled down
 * by lowering the density used to resolve the theme's dp tokens.
 *
 * [interactive] previews react to touches so users can feel the press effect before applying.
 */
class ThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var interactive = false
    /** Show corner hints / suggestion strip (large live preview only). */
    var detailed = false

    private var theme: KeyboardTheme? = null
    private var pressEffect: KeyPressEffect? = null
    private var highContrast = false

    private class PreviewKey(val model: KeyModel, val rect: RectF, val drawable: KeycapDrawable, val spec: KeycapSpec)

    private val keys = ArrayList<PreviewKey>()
    private var background: KeyboardBackgroundDrawable? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val icons = HashMap<Int, Drawable>()
    private var stripHeight = 0f
    private var pressedKey: PreviewKey? = null

    fun setTheme(theme: KeyboardTheme, pressEffect: KeyPressEffect? = null, highContrast: Boolean = false) {
        if (this.theme == theme && this.pressEffect == pressEffect && this.highContrast == highContrast) return
        this.theme = theme
        this.pressEffect = pressEffect
        this.highContrast = highContrast
        rebuild()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    private fun rebuild() {
        val t = theme ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        keys.clear()
        if (w <= 0f || h <= 0f) return
        val density = resources.displayMetrics.density
        // A real phone keyboard is ~392dp wide; scale all dp tokens to this preview.
        val scale = (w / density / 392f).coerceIn(0.25f, 1.2f)
        val tokenDensity = density * scale
        val effect = pressEffect ?: t.pressEffect
        val resolver = KeycapResolver(t, tokenDensity, highContrast, effect)
        val motion = KeyMotion(effect, false)

        background = KeyboardBackgroundDrawable(t.background, tokenDensity).also { it.setBounds(0, 0, width, height) }

        stripHeight = if (detailed) 30f * tokenDensity else 0f
        val padH = 6f * tokenDensity
        val padV = 4f * tokenDensity
        val rows = KeyLayoutHelper.getAlphaRows(false, KeyboardLanguage.ENGLISH, ShiftState.UNSHIFTED)
        val rowH = (h - stripHeight - 2 * padV) / rows.size
        rows.forEachIndexed { ri, row ->
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (w - 2 * padH) / total
            var x = padH
            var charIndex = 0
            row.forEach { k ->
                val kw = unit * k.weight
                if (k.type != KeyType.SPACER) {
                    val role = KeyColorResolver.roleOf(k)
                    val xf = (x + kw / 2f - padH) / (w - 2 * padH)
                    val base = KeyColorResolver.baseColor(t, k, role, ri, rows.size, charIndex, xf)
                    val spec = resolver.resolve(role, base, KeyColorResolver.isWide(k), false, KeyColorResolver.edgeTint(t, xf))
                    val top = stripHeight + padV + ri * rowH
                    val rect = RectF(x, top, x + kw, top + rowH)
                    val d = KeycapDrawable(spec, motion)
                    d.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
                    d.callback = this
                    keys.add(PreviewKey(k, rect, d, spec))
                    if (k.type == KeyType.CHARACTER) charIndex++
                }
                x += kw
            }
        }
        textPaint.typeface = Typeface.create(t.typography.fontFamily, if (t.typography.bold) Typeface.BOLD else Typeface.NORMAL)
    }

    override fun verifyDrawable(who: Drawable): Boolean = keys.any { it.drawable === who } || super.verifyDrawable(who)

    override fun invalidateDrawable(drawable: Drawable) {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme ?: return
        background?.draw(canvas)
        val density = resources.displayMetrics.density
        val scale = (width / density / 392f).coerceIn(0.25f, 1.2f)

        if (stripHeight > 0f) {
            stripPaint.color = t.palette.suggestionBg
            canvas.drawRect(0f, 0f, width.toFloat(), stripHeight, stripPaint)
            textPaint.color = t.palette.suggestionText
            textPaint.textSize = stripHeight * 0.42f
            val cy = stripHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            val words = arrayOf("hello", "Hello", "help")
            for (i in 0..2) canvas.drawText(words[i], width * (0.2f + 0.3f * i), cy, textPaint)
        }

        for (k in keys) {
            k.drawable.draw(canvas)
            val r = k.rect
            val cx = r.centerX()
            val cy = r.centerY() + k.drawable.labelOffsetY
            val rowH = r.height()
            val labelScale = k.drawable.labelScale
            canvas.save()
            if (labelScale != 1f) canvas.scale(labelScale, labelScale, cx, cy)
            when (k.model.type) {
                KeyType.SHIFT -> drawIcon(canvas, R.drawable.ic_shift, cx, cy, rowH * 0.42f, k.spec.textColor)
                KeyType.BACKSPACE -> drawIcon(canvas, R.drawable.ic_backspace, cx, cy, rowH * 0.40f, k.spec.textColor)
                KeyType.ENTER -> drawIcon(canvas, R.drawable.ic_enter, cx, cy, rowH * 0.42f, k.spec.textColor)
                KeyType.SPACE -> if (detailed) drawLabel(canvas, "English", cx, cy, rowH * 0.26f, k.spec.secondaryTextColor)
                KeyType.MODE_CHANGE -> drawLabel(canvas, k.model.primaryText, cx, cy, rowH * 0.30f, k.spec.textColor)
                else -> {
                    drawLabel(canvas, k.model.primaryText, cx, cy, KeyLabelMetrics.letterPx(rowH, density * scale, k.model.primaryText, false, t.typography, 1f), k.spec.textColor)
                    if (detailed && k.model.altText.isNotEmpty()) {
                        textPaint.textAlign = Paint.Align.RIGHT
                        drawLabel(canvas, k.model.altText, r.right - k.spec.gapHPx - rowH * 0.12f, r.top + k.spec.gapVPx + rowH * 0.2f, rowH * 0.19f, k.spec.secondaryTextColor)
                        textPaint.textAlign = Paint.Align.CENTER
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, cy: Float, size: Float, color: Int) {
        textPaint.textSize = size
        textPaint.color = color
        canvas.drawText(text, x, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
    }

    private fun drawIcon(canvas: Canvas, res: Int, cx: Float, cy: Float, size: Float, color: Int) {
        val d = icons.getOrPut(res) { context.getDrawable(res)!!.mutate() }
        d.setTint(color)
        val half = size / 2f
        d.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
        d.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val hit = keys.firstOrNull { it.rect.contains(event.x, event.y) }
                if (hit !== pressedKey) {
                    pressedKey?.drawable?.setPressedState(false)
                    hit?.drawable?.setHotspot(event.x, event.y)
                    hit?.drawable?.setPressedState(true)
                    pressedKey = hit
                }
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressedKey?.drawable?.setPressedState(false)
                pressedKey = null
            }
        }
        return true
    }
}
