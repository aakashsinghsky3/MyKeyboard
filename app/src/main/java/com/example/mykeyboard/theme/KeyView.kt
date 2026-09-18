package com.example.mykeyboard.theme

import android.content.Context
import android.graphics.Canvas
import android.widget.FrameLayout

/**
 * Key container: owns a [KeycapDrawable] background and moves its label children together
 * with the keycap face (depth travel + press scale) inside dispatchDraw. No animators,
 * no allocation – the drawable's time-based animation drives redraws.
 *
 * The whole view remains the touch target, so hitboxes stay edge-to-edge.
 */
class KeyView(context: Context, val keycap: KeycapDrawable) : FrameLayout(context) {

    init {
        background = keycap
        isMotionEventSplittingEnabled = true
        // Children render in our draw pass; keep them from clipping when the face scales up (POP).
        clipChildren = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        val dy = keycap.labelOffsetY
        val scale = keycap.labelScale
        if (dy == 0f && scale == 1f) {
            super.dispatchDraw(canvas)
            return
        }
        val save = canvas.save()
        if (scale != 1f) canvas.scale(scale, scale, width / 2f, height / 2f + dy)
        canvas.translate(0f, dy)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }
}
