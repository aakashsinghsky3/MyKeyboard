package com.example.mykeyboard.theme

/**
 * Catalogue of reusable button styles. Themes reference one of these as their default and
 * users can swap in any other style that [com.example.mykeyboard.model.KeyboardTheme.isStyleCompatible]
 * accepts. Adding a style is adding data – the renderer needs no changes.
 */
object KeyStyles {

    /** Crisp white keys with a 1dp lower hairline – no motion, tiny brightness press. */
    val SIMPLE_ROUNDED = KeyStyle(
        id = "simple_rounded", displayName = "Rounded",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 6f, wideCornerRadiusDp = 6f,
        depthDp = 1f, pressedDepthDp = 1f, sideDarken = 0.22f,
        pressedBrightness = -0.07f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    val SOFT_ROUNDED = KeyStyle(
        id = "soft_rounded", displayName = "Soft Rounded",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 10f, wideCornerRadiusDp = 12f,
        pressedBrightness = -0.08f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    /** Borderless, no depth, low-key face. */
    val MINIMAL = KeyStyle(
        id = "minimal", displayName = "Minimal",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 8f, wideCornerRadiusDp = 8f,
        pressedBrightness = 0.10f,
        horizontalGapDp = 2.5f, verticalGapDp = 2.5f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    val OUTLINE = KeyStyle(
        id = "outline", displayName = "Outline",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 8f, wideCornerRadiusDp = 10f,
        borderWidthDp = 1.2f, faceAlpha = 0.04f, transparentFace = true,
        pressedBrightness = 0.25f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    /** Keys hover above the chassis on a soft shadow; press = tiny scale. */
    val FLOATING = KeyStyle(
        id = "floating", displayName = "Floating",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 9f, wideCornerRadiusDp = 10f,
        shadowAlpha = 0.22f, shadowSpreadDp = 1.4f, shadowOffsetDp = 1.6f,
        pressedScale = 0.94f, pressedBrightness = 0.05f,
        horizontalGapDp = 3.5f, verticalGapDp = 3.5f,
        defaultPressEffect = KeyPressEffect.SCALE
    )

    val MATERIAL = KeyStyle(
        id = "material", displayName = "Material",
        surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 12f, wideCornerRadiusDp = 20f,
        pressedBrightness = -0.04f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.RIPPLE
    )

    val PILL = KeyStyle(
        id = "pill", displayName = "Pill",
        shape = KeyShape.PILL, surface = SurfaceEffect.FLAT,
        pressedBrightness = -0.08f, pressedScale = 0.95f,
        horizontalGapDp = 3f, verticalGapDp = 3.5f,
        defaultPressEffect = KeyPressEffect.SCALE
    )

    val SOFT_3D = KeyStyle(
        id = "soft_3d", displayName = "Soft 3D",
        surface = SurfaceEffect.SOFT_3D,
        cornerRadiusDp = 10f, wideCornerRadiusDp = 12f,
        depthDp = 2.5f, pressedDepthDp = 0.8f,
        highlight = 0.55f, gradient = 0.06f, sideDarken = 0.16f,
        shadowAlpha = 0.08f, shadowSpreadDp = 1f, shadowOffsetDp = 1f,
        pressedBrightness = -0.05f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    /** Chunky kids keycaps: big radius, thick lower edge, soft bounce. */
    val CHUNKY_ROUNDED = KeyStyle(
        id = "chunky_rounded", displayName = "Chunky",
        surface = SurfaceEffect.SOFT_3D,
        cornerRadiusDp = 12f, wideCornerRadiusDp = 14f,
        depthDp = 4.5f, pressedDepthDp = 1.5f,
        highlight = 0.50f, gradient = 0.08f, sideDarken = 0.30f,
        shadowAlpha = 0.14f, shadowSpreadDp = 1.2f, shadowOffsetDp = 1.2f,
        pressedScale = 0.97f, pressedBrightness = -0.04f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.POP
    )

    val BUBBLE = KeyStyle(
        id = "bubble", displayName = "Bubble",
        surface = SurfaceEffect.BUBBLE,
        cornerRadiusDp = 15f, wideCornerRadiusDp = 18f,
        depthDp = 3f, pressedDepthDp = 1f,
        highlight = 0.85f, gradient = 0.10f, sideDarken = 0.18f,
        shadowAlpha = 0.12f, shadowSpreadDp = 1.2f, shadowOffsetDp = 1.4f,
        pressedScale = 0.95f, pressedBrightness = -0.05f,
        horizontalGapDp = 3f, verticalGapDp = 3f,
        defaultPressEffect = KeyPressEffect.POP
    )

    /** Physical keycap: top face inset in a darker skirt, real travel on press. */
    val MECHANICAL = KeyStyle(
        id = "mechanical", displayName = "Mechanical 3D",
        surface = SurfaceEffect.MECHANICAL,
        cornerRadiusDp = 5f, wideCornerRadiusDp = 6f,
        depthDp = 3.5f, pressedDepthDp = 1f,
        skirtInsetDp = 2.4f,
        highlight = 0.45f, gradient = 0.10f, sideDarken = 0.20f,
        shadowAlpha = 0.20f, shadowSpreadDp = 1.2f, shadowOffsetDp = 1.4f,
        pressedBrightness = -0.04f,
        horizontalGapDp = 2.5f, verticalGapDp = 2.5f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    val RETRO = KeyStyle(
        id = "retro", displayName = "Retro Keyboard",
        surface = SurfaceEffect.MECHANICAL,
        cornerRadiusDp = 3f, wideCornerRadiusDp = 3f,
        depthDp = 4.5f, pressedDepthDp = 1.5f,
        skirtInsetDp = 3f, borderWidthDp = 1f,
        highlight = 0.30f, gradient = 0.06f, sideDarken = 0.28f,
        shadowAlpha = 0.18f, shadowSpreadDp = 1f, shadowOffsetDp = 1.4f,
        pressedBrightness = -0.04f,
        horizontalGapDp = 2.5f, verticalGapDp = 2.5f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    /** Raised dark keycaps with accent glow on the action key and active shift only. */
    val RAISED_GAMING = KeyStyle(
        id = "raised_gaming", displayName = "Raised 3D",
        surface = SurfaceEffect.GAMING,
        cornerRadiusDp = 6f, wideCornerRadiusDp = 7f,
        depthDp = 4f, pressedDepthDp = 1.2f,
        highlight = 0.35f, gradient = 0.10f, sideDarken = 0.55f,
        shadowAlpha = 0.45f, shadowSpreadDp = 1.2f, shadowOffsetDp = 1.4f,
        pressedBrightness = 0.10f,
        horizontalGapDp = 2.8f, verticalGapDp = 2.8f,
        requiresDarkChassis = true,
        defaultPressEffect = KeyPressEffect.GLOW
    )

    val GLASS = KeyStyle(
        id = "glass", displayName = "Glass",
        surface = SurfaceEffect.GLASS,
        cornerRadiusDp = 10f, wideCornerRadiusDp = 12f,
        borderWidthDp = 0.8f, faceAlpha = 0.14f,
        highlight = 0.55f,
        pressedBrightness = 0.20f,
        requiresDarkChassis = true,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    val NEUMORPHIC = KeyStyle(
        id = "neumorphic", displayName = "Neumorphic",
        surface = SurfaceEffect.NEUMORPHIC,
        cornerRadiusDp = 12f, wideCornerRadiusDp = 14f,
        gradient = 0.06f, pressedBrightness = -0.05f,
        horizontalGapDp = 3.5f, verticalGapDp = 3.5f,
        requiresSolidChassis = true,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    /** 8-bit block buttons: stepped corners, hard lower edge. */
    val PIXEL = KeyStyle(
        id = "pixel", displayName = "Pixel",
        shape = KeyShape.NOTCHED, surface = SurfaceEffect.FLAT,
        cornerRadiusDp = 0f, notchDp = 2.5f,
        depthDp = 3f, pressedDepthDp = 0.8f, sideDarken = 0.38f,
        pressedBrightness = 0.08f,
        horizontalGapDp = 2.5f, verticalGapDp = 2.5f,
        defaultPressEffect = KeyPressEffect.SUBTLE
    )

    val ALL: List<KeyStyle> = listOf(
        SIMPLE_ROUNDED, SOFT_ROUNDED, MINIMAL, OUTLINE, FLOATING, MATERIAL, PILL,
        SOFT_3D, CHUNKY_ROUNDED, BUBBLE, MECHANICAL, RETRO, RAISED_GAMING, GLASS, NEUMORPHIC, PIXEL
    )

    fun byId(id: String?): KeyStyle? = ALL.firstOrNull { it.id == id }
}
