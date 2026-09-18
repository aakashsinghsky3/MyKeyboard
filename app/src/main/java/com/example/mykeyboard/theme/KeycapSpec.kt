package com.example.mykeyboard.theme

import com.example.mykeyboard.model.KeyboardTheme
import kotlin.math.min

/** Where top lighting is drawn on the face. */
enum class HighlightMode { NONE, RIM, GLOSS, DOME }

/**
 * Fully-resolved, pixel-space recipe for one keycap appearance.
 *
 * Instances are immutable and cached by [KeycapResolver]; a keyboard with 40 keys usually
 * needs fewer than 10 distinct specs (rainbow themes need one per distinct colour).
 * Nothing here is recomputed during touch handling.
 */
class KeycapSpec(
    val shape: KeyShape,
    val radiusPx: Float,
    val notchPx: Float,
    val gapHPx: Float,
    val gapVPx: Float,
    val depthPx: Float,
    val pressedDepthPx: Float,
    val skirtXPx: Float,
    val skirtTopPx: Float,
    val skirtBottomPx: Float,
    val faceTop: Int,
    val faceBottom: Int,
    val faceTopPressed: Int,
    val faceBottomPressed: Int,
    val sideTop: Int,
    val sideBottom: Int,
    val highlightMode: HighlightMode,
    val highlightColor: Int,
    val borderColor: Int,
    val borderWidthPx: Float,
    val shadowColor: Int,
    val shadowSpreadPx: Float,
    val shadowOffsetPx: Float,
    val neuLight: Int,
    val neuDark: Int,
    val neuOffsetPx: Float,
    val glowColor: Int,
    val glowIdle: Boolean,
    val glowWidthPx: Float,
    val pressedScale: Float,
    val textColor: Int,
    val secondaryTextColor: Int,
    val indicatorColor: Int
) {
    val has3DBody: Boolean get() = depthPx > 0.5f

    /**
     * Per-size cache of platform gradient objects built by the renderer (stored as Any so this
     * file stays platform-free). Keys of the same size and look share one set of shaders, and
     * swapping specs on Shift never allocates after the first time.
     */
    val rendererCache: HashMap<Long, Any> = HashMap(4)
}

/**
 * Pure geometry shared by the Android drawable and the off-device preview renderer.
 *
 * Writes into a caller-owned FloatArray to avoid allocation per frame.
 * Layout of [out] (size [SIZE]):
 *  0..3  body  l,t,r,b   (silhouette incl. lower edge)
 *  4..7  face  l,t,r,b   (top surface)
 *  8     label dy        (offset of face centre from view centre)
 *  9     scale
 *  10    current depth
 *  11    face radius
 */
object KeycapGeometry {
    const val SIZE = 12

    fun compute(spec: KeycapSpec, width: Float, height: Float, pressProgress: Float, pressScaleEnabled: Boolean, out: FloatArray) {
        val p = pressProgress.coerceIn(0f, 1f)
        val cl = spec.gapHPx
        val ct = spec.gapVPx
        val cr = width - spec.gapHPx
        val cb = height - spec.gapVPx
        val cellH = (cb - ct).coerceAtLeast(1f)

        // Never let the lower edge eat more than 16% of a (small / landscape) key.
        val maxDepth = cellH * 0.16f
        val depth = min(spec.depthPx, maxDepth)
        val pressedDepth = min(spec.pressedDepthPx, depth)
        val cur = depth + (pressedDepth - depth) * p
        val travel = depth - cur

        val skirtTop = min(spec.skirtTopPx, cellH * 0.08f)
        val skirtBottom = min(spec.skirtBottomPx, cellH * 0.10f)

        out[0] = cl
        out[1] = ct + travel
        out[2] = cr
        out[3] = cb
        out[4] = cl + spec.skirtXPx
        out[5] = ct + travel + skirtTop
        out[6] = cr - spec.skirtXPx
        out[7] = cb - cur - skirtBottom
        out[8] = (out[5] + out[7]) / 2f - height / 2f
        out[9] = if (pressScaleEnabled) 1f + (spec.pressedScale - 1f) * p else 1f

        val faceW = out[6] - out[4]
        val faceH = out[7] - out[5]
        out[10] = cur
        out[11] = when (spec.shape) {
            KeyShape.PILL -> min(faceW, faceH) / 2f
            KeyShape.NOTCHED -> 0f
            KeyShape.ROUNDED -> min(spec.radiusPx, min(faceW, faceH) / 2f)
        }
    }
}

/**
 * Turns (theme, style, role, base colour) into a [KeycapSpec]. Results are cached per
 * resolver; create one resolver per applied theme.
 */
class KeycapResolver(
    private val theme: KeyboardTheme,
    private val density: Float,
    private val highContrast: Boolean = false,
    private val pressEffect: KeyPressEffect = theme.pressEffect
) {
    private data class SpecKey(val role: KeyRole, val baseColor: Int, val wide: Boolean, val active: Boolean, val edgeTint: Int)

    private val cache = HashMap<SpecKey, KeycapSpec>()
    private val style = theme.keyStyle
    private val chassis = theme.chassisColor

    /** Called while building a layout (and for pre-resolving shift states) – not per touch. */
    fun resolve(role: KeyRole, baseColor: Int, wide: Boolean, active: Boolean = false, edgeTint: Int = 0): KeycapSpec {
        val key = SpecKey(role, baseColor, wide, active, edgeTint)
        return cache.getOrPut(key) { build(role, baseColor, wide, active, edgeTint) }
    }

    private fun dp(v: Float) = v * density

    private fun build(role: KeyRole, baseColor: Int, wide: Boolean, active: Boolean, edgeTint: Int): KeycapSpec {
        val s = style
        val pal = theme.palette
        val effect = s.surface

        // ---- Face colour -------------------------------------------------------------
        val opaqueBase = ThemeColor.withAlpha(baseColor, 255)
        val face: Int = when {
            effect == SurfaceEffect.GLASS -> ThemeColor.withAlpha(ThemeColor.lighten(opaqueBase, 0.30f), s.faceAlpha)
            s.transparentFace -> ThemeColor.withAlpha(opaqueBase, s.faceAlpha)
            else -> opaqueBase
        }
        // What the eye actually sees (for contrast decisions).
        val perceivedFace = if (ThemeColor.alpha(face) < 255) ThemeColor.over(face, chassis) else face
        val lightFace = ThemeColor.isLight(perceivedFace)

        val g = s.gradient
        val (faceTop, faceBottom) = when (effect) {
            SurfaceEffect.FLAT -> face to face
            // Real keycaps have a slightly concave "dish": darker at the top edge.
            SurfaceEffect.MECHANICAL -> ThemeColor.shift(face, -g * 0.35f) to ThemeColor.shift(face, g * 0.35f)
            SurfaceEffect.NEUMORPHIC -> ThemeColor.shift(face, g * 0.6f) to ThemeColor.shift(face, -g * 0.6f)
            else -> ThemeColor.shift(face, g * 0.5f) to ThemeColor.shift(face, -g * 0.5f)
        }

        // Dark faces cannot visibly darken further when pressed – brighten instead.
        var pb = s.pressedBrightness
        if (pb < 0f && ThemeColor.luminance(perceivedFace) < 0.06) pb = -pb * 0.9f
        val faceTopPressed = ThemeColor.shift(faceTop, pb)
        val faceBottomPressed = ThemeColor.shift(faceBottom, pb)

        // ---- Lower edge / skirt --------------------------------------------------------
        var sideBase = if (pal.keyBottom != 0 && role != KeyRole.ACTION && !active) {
            // Explicit edge colour keeps its relation to light/dark faces.
            if (opaqueBase == pal.keySurface) pal.keyBottom else ThemeColor.darken(opaqueBase, s.sideDarken)
        } else {
            ThemeColor.darken(opaqueBase, s.sideDarken)
        }
        if (edgeTint != 0 && role != KeyRole.ACTION && !active) sideBase = ThemeColor.blend(sideBase, edgeTint, 0.55f)
        val sideTop: Int
        val sideBottom: Int
        if (effect == SurfaceEffect.MECHANICAL) {
            sideTop = ThemeColor.blend(opaqueBase, sideBase, 0.45f)
            sideBottom = sideBase
        } else {
            sideTop = sideBase
            sideBottom = ThemeColor.darken(sideBase, 0.06f)
        }

        // ---- Highlight -----------------------------------------------------------------
        val highlightMode = when (effect) {
            SurfaceEffect.FLAT, SurfaceEffect.NEUMORPHIC -> HighlightMode.NONE
            SurfaceEffect.MATTE_3D, SurfaceEffect.SOFT_3D, SurfaceEffect.MECHANICAL, SurfaceEffect.GAMING -> HighlightMode.RIM
            SurfaceEffect.GLOSSY_3D, SurfaceEffect.GLASS -> HighlightMode.GLOSS
            SurfaceEffect.BUBBLE -> HighlightMode.DOME
        }
        val highlightAlpha = s.highlight * when (highlightMode) {
            HighlightMode.NONE -> 0f
            HighlightMode.RIM -> if (lightFace) 0.70f else 0.20f
            HighlightMode.GLOSS -> if (lightFace) 0.55f else 0.30f
            HighlightMode.DOME -> if (lightFace) 0.75f else 0.40f
        }
        val highlightColor = ThemeColor.withAlpha(0xFFFFFFFF.toInt(), highlightAlpha.coerceIn(0f, 1f))

        // ---- Border --------------------------------------------------------------------
        var borderWidth = dp(s.borderWidthDp)
        var borderColor = when {
            borderWidth <= 0f -> 0
            effect == SurfaceEffect.GLASS -> ThemeColor.withAlpha(0xFFFFFFFF.toInt(), 0.30f)
            s.transparentFace -> ThemeColor.withAlpha(pal.keyText, 0.38f)
            pal.keyBorder != 0 -> pal.keyBorder
            else -> ThemeColor.shift(opaqueBase, if (lightFace) -0.22f else 0.16f)
        }
        if (highContrast) {
            borderWidth = maxOf(borderWidth, dp(1.5f))
            borderColor = ThemeColor.withAlpha(ThemeColor.readableOn(perceivedFace, pal.keyText, 7.0), 0.75f)
        }

        // ---- Shadow, neumorphism, glow ---------------------------------------------------
        val chassisLight = ThemeColor.isLight(chassis)
        val shadowAlpha = (s.shadowAlpha * if (chassisLight) 1f else 1.5f).coerceIn(0f, 0.85f)
        val shadowColor = if (shadowAlpha > 0f) ThemeColor.withAlpha(pal.shadow, shadowAlpha) else 0

        val neuLight: Int
        val neuDark: Int
        if (effect == SurfaceEffect.NEUMORPHIC) {
            neuLight = ThemeColor.withAlpha(ThemeColor.lighten(chassis, if (chassisLight) 0.85f else 0.10f), if (chassisLight) 0.95f else 0.55f)
            neuDark = ThemeColor.withAlpha(ThemeColor.darken(chassis, if (chassisLight) 0.20f else 0.55f), 0.75f)
        } else {
            neuLight = 0; neuDark = 0
        }

        val glowColor = if (effect == SurfaceEffect.GAMING && (role == KeyRole.ACTION || active)) {
            ThemeColor.withAlpha(pal.accent, 0.55f)
        } else if (pressEffect == KeyPressEffect.GLOW) {
            ThemeColor.withAlpha(pal.accent, 0.45f)
        } else 0
        val glowIdle = effect == SurfaceEffect.GAMING && (role == KeyRole.ACTION || active)

        // ---- Labels ----------------------------------------------------------------------
        val preferredText = if (role == KeyRole.ACTION || active) pal.actionText else pal.keyText
        val minContrast = if (role == KeyRole.ACTION || active) 3.0 else 4.5
        val text = ThemeColor.readableOn(perceivedFace, preferredText, if (highContrast) 7.0 else minContrast)
        val secondaryCandidate = if (role == KeyRole.ACTION || active) ThemeColor.blend(text, perceivedFace, 0.30f) else pal.keyTextSecondary
        val secondary = if (ThemeColor.contrast(ThemeColor.withAlpha(secondaryCandidate, 255), perceivedFace) >= 3.0) {
            secondaryCandidate
        } else {
            ThemeColor.blend(text, perceivedFace, 0.30f)
        }

        val radius = dp(if (wide) (s.wideCornerRadiusDp ?: s.cornerRadiusDp) else s.cornerRadiusDp)
        val skirtX = dp(s.skirtInsetDp)

        return KeycapSpec(
            shape = s.shape,
            radiusPx = radius,
            notchPx = dp(s.notchDp),
            gapHPx = dp(s.horizontalGapDp),
            gapVPx = dp(s.verticalGapDp),
            depthPx = dp(s.depthDp),
            pressedDepthPx = dp(s.pressedDepthDp),
            skirtXPx = skirtX,
            skirtTopPx = skirtX * 0.55f,
            skirtBottomPx = skirtX * 1.1f,
            faceTop = faceTop,
            faceBottom = faceBottom,
            faceTopPressed = faceTopPressed,
            faceBottomPressed = faceBottomPressed,
            sideTop = sideTop,
            sideBottom = sideBottom,
            highlightMode = highlightMode,
            highlightColor = highlightColor,
            borderColor = borderColor,
            borderWidthPx = borderWidth,
            shadowColor = shadowColor,
            shadowSpreadPx = dp(s.shadowSpreadDp),
            shadowOffsetPx = dp(s.shadowOffsetDp),
            neuLight = neuLight,
            neuDark = neuDark,
            neuOffsetPx = dp(2f),
            glowColor = glowColor,
            glowIdle = glowIdle,
            glowWidthPx = dp(2.5f),
            pressedScale = s.pressedScale,
            textColor = text,
            secondaryTextColor = secondary,
            indicatorColor = text
        )
    }
}
