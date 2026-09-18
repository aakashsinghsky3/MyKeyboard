package com.example.mykeyboard.theme

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Animation settings shared by all keys of one keyboard (resolved once per theme apply). */
class KeyMotion(
    val effect: KeyPressEffect,
    /** Accessibility "reduce motion": state changes jump, no easing/bounce/ripple. */
    val reduceMotion: Boolean
) {
    val downMs = if (reduceMotion) 0L else 45L
    val upMs = if (reduceMotion) 0L else when (effect) { KeyPressEffect.POP -> 140L; else -> 85L }
    val scaleEnabled = !reduceMotion && (effect == KeyPressEffect.SCALE || effect == KeyPressEffect.POP)
    val rippleEnabled = !reduceMotion && effect == KeyPressEffect.RIPPLE
    val brightnessEnabled = effect != KeyPressEffect.NONE
}

/**
 * Draws one keycap from a [KeycapSpec].
 *
 * Performance contract:
 *  - No allocation in [draw] or on press/release (paints, shaders, paths and rect are reused).
 *  - Gradients are rebuilt only when bounds or spec change (layout time).
 *  - Press animation is time-based inside draw(); nothing is posted and input is never waited on.
 *  - Shadows are 2–3 stacked translucent round-rects instead of blur, so they cost the same
 *    on every API level and on hardware layers.
 */
class KeycapDrawable(spec: KeycapSpec, private var motion: KeyMotion) : Drawable() {

    var spec: KeycapSpec = spec
        set(value) {
            if (field !== value) {
                field = value
                rebuildShaders()
                invalidateSelf()
            }
        }

    /** Draws a caps-lock indicator dot (state not conveyed by colour only). */
    var indicatorVisible = false
        set(value) {
            if (field != value) { field = value; invalidateSelf() }
        }

    private val geo = FloatArray(KeycapGeometry.SIZE)
    private val rect = RectF()
    private val path = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private var faceShader: Shader? = null
    private var facePressedShader: Shader? = null
    private var sideShader: Shader? = null
    private var highlightShader: Shader? = null

    private var drawAlpha = 255

    // ---- press animation state ----
    private var target = 0f
    private var fromProgress = 0f
    private var animStart = 0L
    private var animDuration = 0L
    private var hotspotX = 0f
    private var hotspotY = 0f
    private var rippleStart = 0L

    /** Face-centre offset from the view centre, valid after [draw]. Used to move labels. */
    val labelOffsetY: Float get() = geo[8]
    val labelScale: Float get() = geo[9]

    fun setMotion(m: KeyMotion) { motion = m }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        var pressed = false
        for (s in state) if (s == android.R.attr.state_pressed) { pressed = true; break }
        return setPressedState(pressed)
    }

    override fun setHotspot(x: Float, y: Float) {
        hotspotX = x
        hotspotY = y
    }

    /** Starts the press/release animation from wherever the key currently is. */
    fun setPressedState(pressed: Boolean): Boolean {
        val newTarget = if (pressed) 1f else 0f
        if (newTarget == target) return false
        val now = SystemClock.uptimeMillis()
        fromProgress = currentProgress(now)
        target = newTarget
        animStart = now
        animDuration = if (pressed) motion.downMs else motion.upMs
        if (pressed && motion.rippleEnabled) rippleStart = now
        invalidateSelf()
        return true
    }

    private fun ease(t: Float): Float = 1f - (1f - t) * (1f - t)

    private fun currentProgress(now: Long): Float {
        if (animDuration <= 0L) return target
        val t = ((now - animStart).toFloat() / animDuration).coerceIn(0f, 1f)
        return fromProgress + (target - fromProgress) * ease(t)
    }

    /** POP releases with a small overshoot on scale only (depth never overshoots). */
    private fun scaleProgress(now: Long, p: Float): Float {
        if (motion.effect != KeyPressEffect.POP || target != 0f || animDuration <= 0L) return p
        val t = ((now - animStart).toFloat() / animDuration).coerceIn(0f, 1f)
        if (t >= 1f) return 0f
        // Negative progress = key briefly grows ~1–2% before settling.
        return p - 0.35f * kotlin.math.sin(t * Math.PI.toFloat()) * t
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        rebuildShaders()
    }

    private class ShaderSet(val face: Shader?, val facePressed: Shader?, val side: Shader?, val highlight: Shader?)

    private fun rebuildShaders() {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val s = spec
        val sizeKey = (b.width().toLong() shl 32) or b.height().toLong()
        val set = s.rendererCache.getOrPut(sizeKey) { createShaders(s, b.width().toFloat(), b.height().toFloat()) } as ShaderSet
        faceShader = set.face
        facePressedShader = set.facePressed
        sideShader = set.side
        highlightShader = set.highlight
    }

    private fun createShaders(s: KeycapSpec, w: Float, h: Float): ShaderSet {
        KeycapGeometry.compute(s, w, h, 0f, false, geo)
        val faceHeight = max(1f, geo[7] - geo[5])
        val bodyH = max(1f, geo[3] - geo[1])
        val transparentWhite = s.highlightColor and 0x00FFFFFF
        return ShaderSet(
            face = if (s.faceTop != s.faceBottom) LinearGradient(0f, 0f, 0f, faceHeight, s.faceTop, s.faceBottom, Shader.TileMode.CLAMP) else null,
            facePressed = if (s.faceTopPressed != s.faceBottomPressed) LinearGradient(0f, 0f, 0f, faceHeight, s.faceTopPressed, s.faceBottomPressed, Shader.TileMode.CLAMP) else null,
            side = if (s.sideTop != s.sideBottom) LinearGradient(0f, 0f, 0f, bodyH, s.sideTop, s.sideBottom, Shader.TileMode.CLAMP) else null,
            highlight = when (s.highlightMode) {
                HighlightMode.NONE -> null
                HighlightMode.RIM -> LinearGradient(0f, 0f, 0f, faceHeight * 0.38f, s.highlightColor, transparentWhite, Shader.TileMode.CLAMP)
                HighlightMode.GLOSS -> LinearGradient(0f, 0f, 0f, faceHeight * 0.5f, s.highlightColor, ThemeColor.withAlpha(s.highlightColor, ThemeColor.alpha(s.highlightColor) / 6), Shader.TileMode.CLAMP)
                HighlightMode.DOME -> LinearGradient(0f, 0f, 0f, faceHeight * 0.42f, s.highlightColor, transparentWhite, Shader.TileMode.CLAMP)
            }
        )
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val s = spec
        val now = SystemClock.uptimeMillis()
        val p = currentProgress(now)
        KeycapGeometry.compute(s, w, h, p, false, geo)
        // Depth follows the true progress; only scale may overshoot (POP).
        if (motion.scaleEnabled) geo[9] = 1f + (s.pressedScale - 1f) * scaleProgress(now, p)

        val save = canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        val scale = geo[9]
        if (scale != 1f) canvas.scale(scale, scale, w / 2f, (geo[5] + geo[7]) / 2f)

        val radius = geo[11]
        val bodyRadius = radius + s.skirtXPx * 0.6f

        // 1. Neumorphic twin shadows
        if (s.neuDark != 0) {
            val o = s.neuOffsetPx * (1f - p * 0.7f)
            layeredShadow(canvas, geo[0] + o, geo[1] + o, geo[2] + o, geo[3] + o, bodyRadius, s.neuDark, s.neuOffsetPx)
            layeredShadow(canvas, geo[0] - o, geo[1] - o, geo[2] - o, geo[3] - o, bodyRadius, s.neuLight, s.neuOffsetPx)
        }

        // 2. Drop shadow (shrinks as the key sinks)
        if (s.shadowColor != 0) {
            val dy = s.shadowOffsetPx * (1f - p * 0.6f)
            layeredShadow(canvas, geo[0], geo[1] + dy, geo[2], geo[3] + dy, bodyRadius, s.shadowColor, s.shadowSpreadPx * (1f - p * 0.5f))
        }

        // 3. Glow ring
        if (s.glowColor != 0) {
            val strength = if (s.glowIdle) 0.55f + 0.45f * p else if (motion.effect == KeyPressEffect.GLOW) p else 0f
            if (strength > 0.01f) {
                val baseA = ThemeColor.alpha(s.glowColor)
                for (i in 1..3) {
                    stroke.shader = null
                    stroke.strokeWidth = s.glowWidthPx * i / 1.5f
                    stroke.color = ThemeColor.withAlpha(s.glowColor, (baseA * strength * (0.55f / i) * drawAlpha / 255f).toInt())
                    val e = s.glowWidthPx * (i - 1) * 0.4f
                    shape(canvas, geo[0] - e, geo[1] - e, geo[2] + e, geo[3] + e, bodyRadius + e, stroke)
                }
            }
        }

        // 4. Body / lower edge
        if (s.has3DBody) {
            fill.color = applyAlpha(s.sideTop)
            fill.shader = sideShader
            if (sideShader != null) {
                canvas.save(); canvas.translate(0f, geo[1])
                fill.alpha = drawAlpha
                shape(canvas, geo[0], 0f, geo[2], geo[3] - geo[1], bodyRadius, fill)
                canvas.restore()
            } else {
                shape(canvas, geo[0], geo[1], geo[2], geo[3], bodyRadius, fill)
            }
            fill.shader = null
        }

        // 5. Face
        canvas.save()
        canvas.translate(0f, geo[5])
        val fl = geo[4]; val fr = geo[6]; val fh = geo[7] - geo[5]
        drawFace(canvas, fl, fr, fh, radius, faceShader, s.faceTop)
        if (p > 0.01f && motion.brightnessEnabled) {
            val a = drawAlpha
            drawAlpha = (a * p).toInt()
            drawFace(canvas, fl, fr, fh, radius, facePressedShader, s.faceTopPressed)
            drawAlpha = a
        }

        // 6. Highlight
        highlightShader?.let { hs ->
            fill.shader = hs
            fill.color = 0xFFFFFFFF.toInt()
            fill.alpha = drawAlpha
            val fw = fr - fl
            when (s.highlightMode) {
                HighlightMode.RIM -> shape(canvas, fl, 0f, fr, fh, radius, fill)
                HighlightMode.GLOSS -> {
                    val inset = min(fw, fh) * 0.06f
                    shape(canvas, fl + inset, inset, fr - inset, fh * 0.5f, max(0f, radius - inset), fill)
                }
                HighlightMode.DOME -> {
                    val ix = fw * 0.14f
                    shape(canvas, fl + ix, fh * 0.07f, fr - ix, fh * 0.42f, fh * 0.2f, fill)
                }
                HighlightMode.NONE -> Unit
            }
            fill.shader = null
        }

        // 7. Ripple (clipped to the face)
        if (motion.rippleEnabled && rippleStart > 0L) {
            val t = ((now - rippleStart) / 260f).coerceIn(0f, 1f)
            if (t < 1f) {
                val maxR = hypot(fr - fl, fh)
                val a = ((1f - t) * 0.22f * (if (target > 0f) 1f else 0.7f) * drawAlpha).toInt()
                fill.color = ThemeColor.withAlpha(s.textColor, a)
                canvas.save()
                path.reset()
                rect.set(fl, 0f, fr, fh)
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
                canvas.clipPath(path)
                canvas.drawCircle(hotspotX - b.left, hotspotY - b.top - geo[5], maxR * (0.25f + 0.75f * ease(t)), fill)
                canvas.restore()
            } else {
                rippleStart = 0L
            }
        }

        // 8. Border
        if (s.borderWidthPx > 0f && s.borderColor != 0) {
            stroke.shader = null
            stroke.strokeWidth = s.borderWidthPx
            stroke.color = applyAlpha(s.borderColor)
            val hw = s.borderWidthPx / 2f
            shape(canvas, fl + hw, hw, fr - hw, fh - hw, max(0f, radius - hw), stroke)
        }

        // 9. Caps-lock "LED" dot in the top-right corner (shape cue, not just colour)
        if (indicatorVisible) {
            val r = max(2f, min(fh, fr - fl) * 0.065f)
            fill.color = applyAlpha(s.indicatorColor)
            canvas.drawCircle(fr - r * 2.6f, r * 2.6f, r, fill)
        }
        canvas.restore()

        canvas.restoreToCount(save)

        // Keep animating until settled. No handler posts; the view system drives frames.
        val rippleRunning = rippleStart > 0L
        if ((animDuration > 0L && now - animStart < animDuration) || rippleRunning) invalidateSelf()
    }

    private fun drawFace(canvas: Canvas, fl: Float, fr: Float, fh: Float, radius: Float, shader: Shader?, solid: Int) {
        fill.shader = shader
        fill.color = if (shader == null) applyAlpha(solid) else 0xFFFFFFFF.toInt()
        if (shader != null) fill.alpha = drawAlpha
        shape(canvas, fl, 0f, fr, fh, radius, fill)
        fill.shader = null
    }

    private fun layeredShadow(canvas: Canvas, l: Float, t: Float, r: Float, bt: Float, radius: Float, color: Int, spread: Float) {
        val baseA = ThemeColor.alpha(color)
        val weights = SHADOW_WEIGHTS
        fill.shader = null
        for (i in weights.indices) {
            val e = spread * (i + 1) / weights.size
            fill.color = ThemeColor.withAlpha(color, (baseA * weights[i] * drawAlpha / 255f).toInt())
            shape(canvas, l - e, t - e * 0.5f, r + e, bt + e, radius + e, fill)
        }
    }

    private fun shape(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, paint: Paint) {
        if (r <= l || b <= t) return
        if (spec.shape == KeyShape.NOTCHED) {
            val n = min(spec.notchPx, min(r - l, b - t) / 4f)
            path.reset()
            path.moveTo(l + n, t); path.lineTo(r - n, t); path.lineTo(r - n, t + n); path.lineTo(r, t + n)
            path.lineTo(r, b - n); path.lineTo(r - n, b - n); path.lineTo(r - n, b); path.lineTo(l + n, b)
            path.lineTo(l + n, b - n); path.lineTo(l, b - n); path.lineTo(l, t + n); path.lineTo(l + n, t + n)
            path.close()
            canvas.drawPath(path, paint)
        } else {
            rect.set(l, t, r, b)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    private fun applyAlpha(color: Int): Int =
        if (drawAlpha == 255) color else ThemeColor.withAlpha(color, ThemeColor.alpha(color) * drawAlpha / 255)

    override fun setAlpha(alpha: Int) { drawAlpha = alpha; invalidateSelf() }
    override fun getAlpha(): Int = drawAlpha
    override fun setColorFilter(colorFilter: ColorFilter?) { fill.colorFilter = colorFilter; stroke.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        val SHADOW_WEIGHTS = floatArrayOf(0.50f, 0.30f, 0.20f)
    }
}
