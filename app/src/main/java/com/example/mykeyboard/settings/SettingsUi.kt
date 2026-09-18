package com.example.mykeyboard.settings

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.mykeyboard.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

/**
 * Small design system for the settings app (charcoal + amber, Material 3 structure).
 *
 * Every screen is composed from these pieces so spacing, shapes, typography and colours stay
 * identical everywhere. Colours come from resources, so light / dark switch automatically.
 */
class SettingsUi(val context: Context) {

    val density = context.resources.displayMetrics.density
    fun dp(v: Int): Int = (v * density).roundToInt()
    fun dpf(v: Float): Float = v * density

    // ---- colour tokens ----------------------------------------------------------------------
    private fun c(res: Int) = ContextCompat.getColor(context, res)
    val background get() = c(R.color.ui_background)
    val surface get() = c(R.color.ui_surface)
    val surfaceVariant get() = c(R.color.ui_surface_variant)
    val surfaceHigh get() = c(R.color.ui_surface_high)
    val onSurface get() = c(R.color.ui_on_surface)
    val onSurfaceVariant get() = c(R.color.ui_on_surface_variant)
    val outline get() = c(R.color.ui_outline)
    val primary get() = c(R.color.ui_primary)
    val onPrimary get() = c(R.color.ui_on_primary)
    val primaryContainer get() = c(R.color.ui_primary_container)
    val onPrimaryContainer get() = c(R.color.ui_on_primary_container)
    val accentInk get() = c(R.color.ui_accent_ink)
    val success get() = c(R.color.ui_success)
    val danger get() = c(R.color.ui_danger)
    val charcoal get() = c(R.color.brand_charcoal)
    val offWhite get() = c(R.color.brand_offwhite)

    // ---- typography -------------------------------------------------------------------------
    enum class Type(val sp: Float, val family: String, val style: Int, val spacing: Float = 0f) {
        HEADLINE(28f, "sans-serif-medium", Typeface.NORMAL, -0.01f),
        TITLE(20f, "sans-serif-medium", Typeface.NORMAL),
        TITLE_SMALL(16f, "sans-serif-medium", Typeface.NORMAL),
        BODY(15f, "sans-serif", Typeface.NORMAL),
        BODY_SMALL(13.5f, "sans-serif", Typeface.NORMAL),
        LABEL(13f, "sans-serif-medium", Typeface.NORMAL, 0.02f),
        OVERLINE(12f, "sans-serif-medium", Typeface.NORMAL, 0.08f)
    }

    fun text(value: CharSequence, type: Type, color: Int = onSurface): TextView = TextView(context).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, type.sp)
        typeface = Typeface.create(type.family, type.style)
        letterSpacing = type.spacing
        setTextColor(color)
        includeFontPadding = false
        setLineSpacing(dpf(2f), 1f)
    }

    // ---- motion -----------------------------------------------------------------------------
    /** Material "emphasized decelerate" curve. */
    val emphasized = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    // ---- containers -------------------------------------------------------------------------
    fun lp(w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = top; bottomMargin = bottom }

    /** A scrolling page with standard gutters. Returns (scroll, column). */
    fun page(): Pair<ScrollView, LinearLayout> {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return scroll to column
    }

    fun pageHeader(title: String, subtitle: String): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(12), dp(4), dp(8))
        addView(text(title, Type.HEADLINE))
        addView(text(subtitle, Type.BODY_SMALL, onSurfaceVariant), lp(top = dp(6)))
    }

    fun sectionLabel(title: String): TextView = text(title.uppercase(), Type.OVERLINE, accentInk).apply {
        setPadding(dp(8), dp(24), dp(8), dp(10))
    }

    /** Rounded surface card; add rows to the returned column. */
    fun card(padding: Int = 0): Pair<MaterialCardView, LinearLayout> {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val card = MaterialCardView(context).apply {
            radius = dpf(22f)
            cardElevation = 0f
            setCardBackgroundColor(surface)
            strokeWidth = dp(1)
            setStrokeColor(outline)
            addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        return card to column
    }

    fun rounded(color: Int, radiusDp: Float, strokeColor: Int = 0, strokeDp: Float = 0f): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dpf(radiusDp)
        setColor(color)
        if (strokeDp > 0f) setStroke(dpf(strokeDp).roundToInt().coerceAtLeast(1), strokeColor)
    }

    /** Ripple that respects the rounded shape of [content]. */
    fun ripple(content: Drawable?, radiusDp: Float): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(withAlpha(onSurface, 0.10f)), content, rounded(Color.WHITE, radiusDp))

    fun withAlpha(color: Int, fraction: Float): Int = (color and 0x00FFFFFF) or ((fraction * 255).roundToInt().coerceIn(0, 255) shl 24)

    fun divider(insetStartDp: Int = 72): View = View(context).apply {
        setBackgroundColor(outline)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply { marginStart = dp(insetStartDp); marginEnd = dp(16) }
    }

    /** 40dp tonal icon tile used at the start of rows. */
    fun iconTile(iconRes: Int, tint: Int = onPrimaryContainer, fill: Int = primaryContainer, sizeDp: Int = 40): ImageView = ImageView(context).apply {
        setImageResource(iconRes)
        setColorFilter(tint)
        val p = dp(sizeDp) / 4
        setPadding(p, p, p, p)
        background = rounded(fill, sizeDp * 0.32f)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    // ---- rows -------------------------------------------------------------------------------

    class Row(val root: LinearLayout, val title: TextView, val subtitle: TextView?)

    fun row(iconRes: Int?, title: String, subtitle: String? = null, trailing: View? = null, onClick: (() -> Unit)? = null): Row {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(if (subtitle != null) 72 else 60)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        iconRes?.let { root.addView(iconTile(it), LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(16) }) }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleView = text(title, Type.TITLE_SMALL).apply { maxLines = 2 }
        texts.addView(titleView)
        val subtitleView = subtitle?.let { text(it, Type.BODY_SMALL, onSurfaceVariant).also { tv -> texts.addView(tv, lp(top = dp(3))) } }
        root.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        trailing?.let { root.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(12) }) }
        if (onClick != null) {
            root.isClickable = true
            root.isFocusable = true
            root.background = ripple(null, 0f)
            root.setOnClickListener { onClick() }
        }
        return Row(root, titleView, subtitleView)
    }

    fun chevron(): ImageView = ImageView(context).apply {
        setImageResource(R.drawable.ic_chevron_right)
        setColorFilter(onSurfaceVariant)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun switchRow(iconRes: Int?, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit): Pair<Row, MaterialSwitch> {
        val sw = MaterialSwitch(context).apply {
            isChecked = checked
            contentDescription = title
            setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        }
        val row = row(iconRes, title, subtitle, sw) { sw.toggle() }
        return row to sw
    }

    /** A labelled slider: title + live value on one line, slider underneath. */
    fun sliderBlock(
        iconRes: Int?, title: String, from: Float, to: Float, step: Float, value: Float,
        format: (Float) -> String, onLive: ((Float) -> Unit)? = null, onCommit: (Float) -> Unit
    ): Pair<View, Slider> {
        val valueText = text(format(value), Type.LABEL, accentInk)
        val header = row(iconRes, title, null, valueText).root.apply { setPadding(dp(16), dp(12), dp(16), 0); minimumHeight = dp(52) }
        val slider = Slider(context).apply {
            valueFrom = from
            valueTo = to
            stepSize = step
            // Material Slider throws if value isn't on a step (older builds saved e.g. 88%).
            this.value = (if (step > 0f) from + ((value - from) / step).roundToInt() * step else value).coerceIn(from, to)
            labelBehavior = LabelFormatter.LABEL_GONE
            thumbTintList = ColorStateList.valueOf(primary)
            trackActiveTintList = ColorStateList.valueOf(primary)
            trackInactiveTintList = ColorStateList.valueOf(surfaceHigh)
            haloTintList = ColorStateList.valueOf(withAlpha(primary, 0.2f))
            tickActiveTintList = ColorStateList.valueOf(withAlpha(onPrimary, 0.5f))
            tickInactiveTintList = ColorStateList.valueOf(withAlpha(onSurfaceVariant, 0.4f))
            contentDescription = title
            addOnChangeListener { _, v, fromUser ->
                valueText.text = format(v)
                if (fromUser) onLive?.invoke(v)
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) { onCommit(slider.value) }
            })
        }
        val block = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            val startInset = if (iconRes != null) dp(64) else dp(8)
            addView(slider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = startInset; marginEnd = dp(8); bottomMargin = dp(8)
            })
        }
        return block to slider
    }

    /**
     * Segmented control with a sliding amber indicator. [onSelect] fires only on user taps.
     * Returns the view and a setter to change selection programmatically.
     */
    fun segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit): Pair<View, (Int) -> Unit> {
        var current = selected.coerceIn(0, options.lastIndex)
        val container = FrameLayout(context).apply {
            background = rounded(surfaceVariant, 16f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val indicator = View(context).apply { background = rounded(primary, 12f) }
        container.addView(indicator, FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT))
        val labels = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        container.addView(labels, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(40)))
        val views = options.mapIndexed { i, label ->
            text(label, Type.LABEL, if (i == current) onPrimary else onSurface).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(4), 0, dp(4), 0)
                isClickable = true
                isFocusable = true
                background = ripple(null, 12f)
                contentDescription = label
                labels.addView(this, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            }
        }
        fun place(animate: Boolean) {
            val w = labels.width / options.size
            if (w <= 0) return
            val lpInd = indicator.layoutParams
            if (lpInd.width != w) { lpInd.width = w; indicator.layoutParams = lpInd }
            val target = (w * current).toFloat()
            if (animate) indicator.animate().translationX(target).setDuration(220).setInterpolator(emphasized).start()
            else indicator.translationX = target
            views.forEachIndexed { i, tv ->
                tv.setTextColor(if (i == current) onPrimary else onSurface)
                tv.isSelected = i == current
            }
        }
        labels.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ -> if (r - l != or - ol) place(false) }
        views.forEachIndexed { i, tv ->
            tv.setOnClickListener {
                if (i != current) {
                    current = i
                    place(true)
                    onSelect(i)
                }
            }
        }
        val setter: (Int) -> Unit = { i -> current = i.coerceIn(0, options.lastIndex); place(true) }
        return container to setter
    }

    /** Horizontally scrolling pill chips. */
    fun chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), 0, dp(4), 0) }
        val views = ArrayList<TextView>()
        fun style(tv: TextView, on: Boolean) {
            tv.setTextColor(if (on) onPrimary else onSurface)
            tv.background = ripple(if (on) rounded(primary, 18f) else rounded(surface, 18f, outline, 1f), 18f)
            tv.isSelected = on
        }
        options.forEachIndexed { i, label ->
            val tv = text(label, Type.LABEL).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), 0, dp(16), 0)
                minWidth = dp(48)
                isClickable = true
            }
            style(tv, i == selected)
            tv.setOnClickListener {
                views.forEachIndexed { j, v -> style(v, j == i) }
                onSelect(i)
            }
            views.add(tv)
            row.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)).apply { marginEnd = dp(8) })
        }
        return HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(row)
        }
    }

    // ---- buttons ----------------------------------------------------------------------------
    enum class ButtonKind { FILLED, TONAL, OUTLINED, TEXT, DANGER }

    fun button(label: String, kind: ButtonKind = ButtonKind.FILLED, iconRes: Int? = null, onClick: () -> Unit): TextView =
        text(label, Type.LABEL).apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), 0, dp(20), 0)
            minHeight = dp(44)
            minWidth = dp(64)
            val (fill, ink, stroke) = when (kind) {
                ButtonKind.FILLED -> Triple(primary, onPrimary, 0)
                ButtonKind.TONAL -> Triple(primaryContainer, onPrimaryContainer, 0)
                ButtonKind.OUTLINED -> Triple(Color.TRANSPARENT, onSurface, outline)
                ButtonKind.TEXT -> Triple(Color.TRANSPARENT, accentInk, 0)
                ButtonKind.DANGER -> Triple(Color.TRANSPARENT, danger, withAlpha(danger, 0.5f))
            }
            setTextColor(ink)
            background = ripple(rounded(fill, 22f, stroke, if (stroke != 0) 1f else 0f), 22f)
            iconRes?.let {
                val d = ContextCompat.getDrawable(context, it)?.mutate()
                d?.setTint(ink)
                d?.setBounds(0, 0, dp(18), dp(18))
                setCompoundDrawables(d, null, null, null)
                compoundDrawablePadding = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    fun pill(label: String, fill: Int, ink: Int): TextView = text(label, Type.LABEL, ink).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(5), dp(12), dp(5))
        background = rounded(fill, 14f)
    }

    /** Adds [child] to [column] with the standard gap below. */
    fun add(column: LinearLayout, child: View, gapBelowDp: Int = 12) {
        column.addView(child, lp(bottom = dp(gapBelowDp)))
    }

    /** Subtle entrance: fade + 12dp rise. Honours the system animator scale automatically. */
    fun enter(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = dpf(12f)
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(260).setInterpolator(emphasized).start()
    }

    /** Numeric animation helper for small counters. */
    fun animateInt(from: Int, to: Int, onValue: (Int) -> Unit) {
        ValueAnimator.ofInt(from, to).apply {
            duration = 400
            interpolator = emphasized
            addUpdateListener { onValue(it.animatedValue as Int) }
            start()
        }
    }
}
