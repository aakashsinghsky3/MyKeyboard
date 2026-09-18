package com.example.mykeyboard.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mykeyboard.R
import com.example.mykeyboard.model.EmojiData
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.theme.KeyMotion
import com.example.mykeyboard.theme.KeyRole
import com.example.mykeyboard.theme.KeyView
import com.example.mykeyboard.theme.KeycapDrawable
import com.example.mykeyboard.theme.KeycapResolver
import com.example.mykeyboard.theme.ThemeColor
import com.example.mykeyboard.utils.EmojiVariantsHelper
import com.example.mykeyboard.utils.KeyboardPreferences
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Emoji panel: one continuous, vertically scrolling grid with compact section headers,
 * a slim category bar that follows the scroll position, recents + favourites stored locally,
 * long-press skin tones / favourite toggle, and an entry point to offline emoji search.
 *
 * Sizing adapts to width (columns), orientation, tablet, keyboard height setting and the
 * navigation inset: it aims for ~5–6 visible rows on a phone without shrinking emojis.
 */
class EmojiKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface EmojiListener {
        fun onEmojiSelected(emoji: String)
        fun onBackToAlpha()
        fun onBackspace()
        fun onSpace()
        fun onSearchRequested() {}
    }

    private var listener: EmojiListener? = null
    private val prefs = KeyboardPreferences(context)
    private var currentTheme: KeyboardTheme = KeyboardTheme.DEFAULT
    private val density = context.resources.displayMetrics.density

    private val categoryScroll: HorizontalScrollView
    private val categoryBar: LinearLayout
    private val recycler: RecyclerView
    private val layoutManager: GridLayoutManager
    private val bottomBar: LinearLayout
    private val adapter = EmojiAdapter()

    // ---- sizing ----
    private var spanCount = 8
    private var cellHeightPx = dp(44)
    private var emojiTextPx = dp(26).toFloat()
    private var headerHeightPx = dp(26)
    private var keyboardHeightHint = 0
    private var bottomInset = 0

    // ---- data (flat list for the adapter) ----
    private val rows = ArrayList<Any>()          // String (emoji) | Header | Hint
    private val tabOfRow = ArrayList<Int>()
    private val tabStart = IntArray(EmojiData.tabs.size)
    private var selectedTab = -1
    private var pendingRebuild = false

    private class Header(val title: String)
    private class Hint(val text: String)

    private val tabIcons = ArrayList<ImageView>()
    private val tabBackgrounds = ArrayList<GradientDrawable>()

    private var popup: PopupWindow? = null

    // ---- horizontal swipe between categories (the grid itself scrolls vertically) ----
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swiping = false
    private var velocityTracker: android.view.VelocityTracker? = null

    private val categorySwipe = object : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = e.x
                    swipeStartY = e.y
                    swiping = false
                    velocityTracker?.recycle()
                    velocityTracker = android.view.VelocityTracker.obtain().apply { addMovement(e) }
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(e)
                    val dx = e.x - swipeStartX
                    val dy = e.y - swipeStartY
                    // Light threshold: horizontal movement cleanly takes over with zero hesitation.
                    if (!swiping && abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.25f) {
                        swiping = true
                        dismissPopup()
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            velocityTracker?.addMovement(e)
            val dx = e.x - swipeStartX
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val limit = dp(140).toFloat()
                    recycler.translationX = (dx * 0.75f).coerceIn(-limit, limit)
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val swipeThreshold = dp(36).toFloat()

                    val targetDelta = when {
                        vx < -700f || (dx < -swipeThreshold && vx < 200f) -> 1  // Swipe left -> next tab
                        vx > 700f || (dx > swipeThreshold && vx > -200f) -> -1 // Swipe right -> prev tab
                        else -> 0
                    }

                    if (targetDelta != 0) {
                        stepCategory(targetDelta)
                    } else {
                        recycler.animate().translationX(0f).setDuration(120)
                            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    }
                    velocityTracker?.recycle()
                    velocityTracker = null
                    swiping = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    recycler.animate().translationX(0f).setDuration(120)
                        .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    velocityTracker?.recycle()
                    velocityTracker = null
                    swiping = false
                }
            }
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

    /** Moves one category left/right and jumps the grid to its first section with crisp snap. */
    private fun stepCategory(delta: Int) {
        if (!EmojiData.isLoaded || rows.isEmpty()) return
        val target = (selectedTab + delta).coerceIn(0, EmojiData.tabs.lastIndex)
        if (target == selectedTab) {
            recycler.animate().translationX(0f).setDuration(120)
                .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            return
        }
        layoutManager.scrollToPositionWithOffset(tabStart[target], 0)
        highlightTab(target, scrollBar = true)
        recycler.animate().cancel()
        // Quick, crisp slide-in from swipe direction
        recycler.translationX = (if (delta > 0) dp(32) else -dp(32)).toFloat()
        recycler.animate().translationX(0f).setDuration(130)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 1. Category bar (compact: 38dp)
        categoryScroll = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(38))
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        categoryBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        categoryScroll.addView(categoryBar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(categoryScroll)

        // 2. Emoji grid (takes all remaining height)
        layoutManager = GridLayoutManager(context, spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    if (position < rows.size && rows[position] is String) 1 else spanCount
            }
        }
        recycler = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(200))
            this.layoutManager = this@EmojiKeyboardView.layoutManager
            adapter = this@EmojiKeyboardView.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(spanCount * 3)
            recycledViewPool.setMaxRecycledViews(TYPE_EMOJI, 96)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            setPadding(dp(4), dp(2), dp(4), dp(6))
            clipToPadding = false
            addOnItemTouchListener(categorySwipe)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val first = this@EmojiKeyboardView.layoutManager.findFirstVisibleItemPosition()
                    if (first in tabOfRow.indices) highlightTab(tabOfRow[first], scrollBar = true)
                    dismissPopup()
                }
            })
        }
        addView(recycler)

        // 3. Bottom bar (ABC · space · backspace) using the theme's keycaps
        bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(46))
            setPadding(dp(6), 0, dp(6), 0)
        }
        addView(bottomBar)

        buildCategoryBar()
        EmojiData.ensureLoaded(context) { rebuildRows(keepPosition = false) }
    }

    fun setEmojiListener(listener: EmojiListener) {
        this.listener = listener
    }

    /** Called by the keyboard with its current content height and bottom (navigation) inset. */
    fun updateFixedContentHeight(targetContentHeight: Int, bottomPad: Int) {
        keyboardHeightHint = targetContentHeight
        bottomInset = bottomPad
        requestLayoutForSize(width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) post { requestLayoutForSize(w) }
    }

    private fun requestLayoutForSize(widthPx: Int) {
        val cfg = resources.configuration
        val isTablet = cfg.smallestScreenWidthDp >= 600
        val isLandscape = cfg.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val widthDp = widthPx / density

        // Columns from width and a comfortable minimum cell, within sensible bounds.
        val minCellDp = if (isTablet) 50f else 43f
        val (minCols, maxCols) = when {
            isTablet -> 10 to 16
            isLandscape -> 10 to 16
            else -> 8 to 10
        }
        val newSpan = (widthDp / minCellDp).toInt().coerceIn(minCols, maxCols)
        val cellW = (widthPx - dp(8)) / newSpan.toFloat()
        cellHeightPx = cellW.coerceAtMost(dp(if (isTablet) 56 else 48).toFloat()).coerceAtLeast(dp(38).toFloat()).roundToInt()
        emojiTextPx = (cellHeightPx * 0.60f).coerceIn(dp(20).toFloat(), dp(34).toFloat())
        headerHeightPx = dp(26)

        // Height: aim for ~5.5 rows on phones (6.5 on tablets) – the half row hints at scrolling –
        // but never exceed a share of the screen, and never shrink below the letter keyboard.
        val screenH = resources.displayMetrics.heightPixels
        val chrome = dp(38) + dp(46) + dp(8)
        val targetRows = when {
            isTablet -> 6.5f
            isLandscape -> 3.5f
            else -> 5.5f
        }
        val heightScale = prefs.heightScale
        val desiredGrid = (targetRows * cellHeightPx * heightScale).roundToInt()
        val maxTotal = (screenH * if (isLandscape) 0.55f else 0.48f).roundToInt()
        val minTotal = keyboardHeightHint
        val total = (desiredGrid + chrome).coerceAtMost(maxTotal).coerceAtLeast(minTotal)
        val gridH = total - chrome

        recycler.layoutParams = (recycler.layoutParams as LayoutParams).apply { height = gridH }
        bottomBar.layoutParams = (bottomBar.layoutParams as LayoutParams).apply { height = dp(46) + bottomInset }
        bottomBar.setPadding(dp(6), dp(2), dp(6), bottomInset + dp(2))

        if (newSpan != spanCount) {
            spanCount = newSpan
            layoutManager.spanCount = newSpan
            recycler.setItemViewCacheSize(newSpan * 3)
        }
        adapter.notifyDataSetChanged()
    }

    fun applyTheme(theme: KeyboardTheme, resolver: KeycapResolver? = null, motion: KeyMotion? = null) {
        currentTheme = theme
        // Keep the emoji surface clean: no keycaps behind emojis, chassis shows through.
        setBackgroundColor(Color.TRANSPARENT)
        categoryScroll.setBackgroundColor(Color.TRANSPARENT)
        buildCategoryBar()
        setupBottomBar(resolver ?: KeycapResolver(theme, density), motion ?: KeyMotion(theme.pressEffect, false))
        adapter.notifyDataSetChanged()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // A swipe interrupted by the keyboard closing must not leave the grid off-centre.
        recycler.animate().cancel()
        recycler.translationX = 0f
        swiping = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != View.VISIBLE) {
            dismissPopup()
        } else {
            // Re-opening always starts centred, even if a swipe was cut short last time.
            recycler.translationX = 0f
            if (pendingRebuild && EmojiData.isLoaded) rebuildRows(keepPosition = false)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------------------------

    private fun rebuildRows(keepPosition: Boolean) {
        pendingRebuild = false
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        rows.clear()
        tabOfRow.clear()
        EmojiData.tabs.forEachIndexed { tabIndex, tab ->
            tabStart[tabIndex] = rows.size
            when (tab.id) {
                EmojiData.TAB_RECENT -> {
                    val recent = prefs.recentEmojis
                    addHeader(if (recent.size >= 8) "Recently used" else "Popular", tabIndex)
                    val list = if (recent.size >= 8) recent else (recent + EmojiData.POPULAR.filter { it !in recent })
                    list.take(spanCount * 4).forEach { addEmoji(it, tabIndex) }
                }
                EmojiData.TAB_FAVORITES -> {
                    addHeader("Favorites", tabIndex)
                    val favs = prefs.favoriteEmojis
                    if (favs.isEmpty()) {
                        rows.add(Hint("Long-press any emoji and choose ☆ to add it here"))
                        tabOfRow.add(tabIndex)
                    } else favs.forEach { addEmoji(it, tabIndex) }
                }
                else -> EmojiData.sections(tab.id).forEach { section ->
                    addHeader(section.header, tabIndex)
                    section.items.forEach { addEmoji(it.emoji, tabIndex) }
                }
            }
        }
        adapter.notifyDataSetChanged()
        if (keepPosition && firstVisible > 0) layoutManager.scrollToPositionWithOffset(firstVisible, 0)
        if (selectedTab < 0) highlightTab(0, scrollBar = false)
    }

    private fun addHeader(title: String, tab: Int) { rows.add(Header(title)); tabOfRow.add(tab) }
    private fun addEmoji(e: String, tab: Int) { rows.add(e); tabOfRow.add(tab) }

    private fun onEmojiPicked(emoji: String) {
        listener?.onEmojiSelected(emoji)
        prefs.recordRecentEmoji(emoji)
        // Stored immediately; the visible grid refreshes next time the panel opens so cells never
        // shift under the finger during rapid taps.
        pendingRebuild = true
    }

    // ---------------------------------------------------------------------------------------
    // Category bar
    // ---------------------------------------------------------------------------------------

    private fun buildCategoryBar() {
        categoryBar.removeAllViews()
        tabIcons.clear()
        tabBackgrounds.clear()
        val pal = currentTheme.palette
        val chassis = currentTheme.chassisColor

        // Search entry
        categoryBar.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(ThemeColor.readableOn(chassis, pal.keyTextSecondary, 3.0))
            contentDescription = "Search emoji"
            val p = dp(7)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(32)).apply { marginEnd = dp(2) }
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(pal.chipBg) }
            setOnClickListener { listener?.onSearchRequested() }
        })

        EmojiData.tabs.forEachIndexed { index, tab ->
            val bg = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.TRANSPARENT) }
            val icon = ImageView(context).apply {
                setImageResource(tab.iconResId)
                contentDescription = tab.title
                val p = dp(6)
                setPadding(p, p, p, p)
                background = bg
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
                setOnClickListener {
                    dismissPopup()
                    layoutManager.scrollToPositionWithOffset(tabStart[index], 0)
                    highlightTab(index, scrollBar = false)
                }
            }
            tabIcons.add(icon)
            tabBackgrounds.add(bg)
            categoryBar.addView(icon)
        }
        val keep = selectedTab.coerceAtLeast(0)
        selectedTab = -1
        highlightTab(keep, scrollBar = false)
    }

    /** Updates existing tab views in place (no allocation while scrolling). */
    private fun highlightTab(index: Int, scrollBar: Boolean) {
        if (index == selectedTab || index !in tabIcons.indices) return
        selectedTab = index
        val pal = currentTheme.palette
        val chassis = currentTheme.chassisColor
        val filled = pal.emojiKeyAccent
        val accentInk = ThemeColor.readableOn(chassis, pal.accent, 3.0)
        val idleInk = ThemeColor.readableOn(chassis, pal.keyTextSecondary, 3.0)
        tabIcons.forEachIndexed { i, icon ->
            val selected = i == index
            tabBackgrounds[i].setColor(
                when {
                    !selected -> Color.TRANSPARENT
                    filled -> pal.keyAction
                    else -> ThemeColor.withAlpha(accentInk, 0.18f)
                }
            )
            icon.setColorFilter(
                when {
                    !selected -> idleInk
                    filled -> ThemeColor.readableOn(pal.keyAction, pal.actionText, 3.0)
                    else -> accentInk
                }
            )
            icon.isSelected = selected
        }
        if (scrollBar) {
            val v = tabIcons[index]
            val scrollX = (v.left + v.width / 2 - categoryScroll.width / 2).coerceAtLeast(0)
            categoryScroll.smoothScrollTo(scrollX, 0)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Bottom bar
    // ---------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBottomBar(resolver: KeycapResolver, motion: KeyMotion) {
        bottomBar.removeAllViews()
        val pal = currentTheme.palette
        val textPx = dp(14).toFloat()

        fun key(role: KeyRole, base: Int, weight: Float, fixedWidth: Int): Pair<KeyView, com.example.mykeyboard.theme.KeycapSpec> {
            val spec = resolver.resolve(role, base, true)
            val v = KeyView(context, KeycapDrawable(spec, motion)).apply {
                layoutParams = if (fixedWidth > 0) LinearLayout.LayoutParams(fixedWidth, LayoutParams.MATCH_PARENT)
                else LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
                tag = spec
            }
            return Pair(v, spec)
        }

        val (abc, abcSpec) = key(KeyRole.FUNCTION, pal.keyFunction, 0f, dp(72))
        abc.apply {
            addView(TextView(context).apply {
                text = "ABC"
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx)
                typeface = Typeface.create(currentTheme.typography.fontFamily, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(abcSpec.textColor)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            })
            contentDescription = "Back to letters"
            setOnClickListener { listener?.onBackToAlpha() }
        }
        bottomBar.addView(abc)

        val (space, spaceSpec) = key(KeyRole.SPACE, pal.keySpace, 1f, 0)
        space.apply {
            addView(TextView(context).apply {
                text = "space"
                setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(13).toFloat())
                gravity = Gravity.CENTER
                setTextColor(spaceSpec.secondaryTextColor)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            })
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { v.isPressed = true; listener?.onSpace() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.isPressed = false
                }
                true
            }
        }
        bottomBar.addView(space)

        val handler = Handler(Looper.getMainLooper())
        var held = false
        val repeat = object : Runnable {
            override fun run() {
                if (held) { listener?.onBackspace(); handler.postDelayed(this, 50) }
            }
        }
        val (del, delSpec) = key(KeyRole.FUNCTION, pal.keyFunction, 0f, dp(72))
        del.apply {
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_backspace)
                setColorFilter(delSpec.textColor)
                layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
            })
            contentDescription = "Delete"
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        held = true
                        listener?.onBackspace()
                        handler.postDelayed(repeat, 350)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        held = false
                        handler.removeCallbacks(repeat)
                    }
                }
                true
            }
        }
        bottomBar.addView(del)
    }

    // ---------------------------------------------------------------------------------------
    // Long-press popup: skin tones + favourite
    // ---------------------------------------------------------------------------------------

    private fun showEmojiPopup(anchor: View, emoji: String) {
        dismissPopup()
        val pal = currentTheme.palette
        val variants: List<String> = EmojiData.item(emoji)?.skins?.let { skins ->
            listOf(EmojiData.stripSkinTone(emoji).let { base -> EmojiData.item(base)?.emoji ?: base }) + skins
        } ?: EmojiVariantsHelper.getVariants(emoji)

        val cell = dp(46)
        val perRow = 6
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            val p = dp(6)
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(pal.popupBg)
                setStroke(dp(1), ThemeColor.withAlpha(pal.popupText, 0.12f))
            }
            elevation = dp(6).toFloat()
        }

        if (variants.size > 1) {
            variants.chunked(perRow).forEach { chunk ->
                val row = LinearLayout(context).apply { orientation = HORIZONTAL }
                chunk.forEach { v ->
                    row.addView(TextView(context).apply {
                        text = v
                        gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, emojiTextPx)
                        layoutParams = LinearLayout.LayoutParams(cell, cell)
                        background = pressedBackground(if (v == emoji) ThemeColor.withAlpha(pal.accent, 0.22f) else Color.TRANSPARENT)
                        setOnClickListener {
                            dismissPopup()
                            onEmojiPicked(v)
                        }
                    })
                }
                content.addView(row)
            }
        }

        val isFav = emoji in prefs.favoriteEmojis
        content.addView(TextView(context).apply {
            text = if (isFav) "★  Remove from favorites" else "☆  Add to favorites"
            textSize = 13.5f
            setTextColor(pal.popupText)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
                if (variants.size > 1) topMargin = dp(4)
            }
            minWidth = dp(200)
            background = pressedBackground(ThemeColor.withAlpha(pal.popupText, 0.06f))
            setOnClickListener {
                prefs.toggleFavoriteEmoji(emoji)
                dismissPopup()
                pendingRebuild = true
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first in tabOfRow.indices && tabOfRow[first] == EmojiData.tabs.lastIndex) rebuildRows(keepPosition = true)
            }
        })

        content.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val pw = content.measuredWidth
        val ph = content.measuredHeight
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val screenW = resources.displayMetrics.widthPixels
        val margin = dp(8)
        val x = (loc[0] + anchor.width / 2 - pw / 2).coerceIn(margin, (screenW - pw - margin).coerceAtLeast(margin))
        var y = loc[1] - ph - dp(6)
        if (y < margin) y = loc[1] + anchor.height + dp(6)

        // Non-focusable: an IME must never take window focus from the app it is typing into.
        popup = PopupWindow(content, pw, ph, false).apply {
            isOutsideTouchable = true
            isClippingEnabled = false
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setTouchInterceptor { _, ev ->
                if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) { dismissPopup(); true } else false
            }
            try { showAtLocation(this@EmojiKeyboardView, Gravity.NO_GRAVITY, x, y) } catch (_: Exception) {}
        }
    }

    private fun dismissPopup() {
        try { popup?.dismiss() } catch (_: Exception) {}
        popup = null
    }

    private fun pressedBackground(idle: Int): StateListDrawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(ThemeColor.withAlpha(currentTheme.palette.keyText, 0.14f))
        })
        addState(intArrayOf(), GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(idle)
        })
    }

    // ---------------------------------------------------------------------------------------
    // Adapter
    // ---------------------------------------------------------------------------------------

    private inner class EmojiHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

    private inner class EmojiAdapter : RecyclerView.Adapter<EmojiHolder>() {

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int = when (rows[position]) {
            is String -> TYPE_EMOJI
            is Header -> TYPE_HEADER
            else -> TYPE_HINT
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiHolder {
            val tv = TextView(parent.context).apply {
                includeFontPadding = false
                maxLines = 1
            }
            val holder = EmojiHolder(tv)
            when (viewType) {
                TYPE_EMOJI -> {
                    tv.gravity = Gravity.CENTER
                    // Created once per holder; binding only swaps text.
                    tv.background = pressedBackground(Color.TRANSPARENT)
                    tv.setOnClickListener {
                        val pos = holder.bindingAdapterPosition
                        (rows.getOrNull(pos) as? String)?.let { onEmojiPicked(it) }
                    }
                    tv.setOnLongClickListener {
                        val pos = holder.bindingAdapterPosition
                        (rows.getOrNull(pos) as? String)?.let { showEmojiPopup(tv, it) }
                        true
                    }
                }
                TYPE_HEADER -> {
                    tv.gravity = Gravity.CENTER_VERTICAL
                    tv.setPadding(dp(10), dp(6), 0, 0)
                    tv.letterSpacing = 0.04f
                    tv.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
                else -> {
                    tv.gravity = Gravity.CENTER
                    tv.maxLines = 2
                    tv.setPadding(dp(16), 0, dp(16), 0)
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: EmojiHolder, position: Int) {
            val tv = holder.tv
            val pal = currentTheme.palette
            when (val row = rows[position]) {
                is String -> {
                    if (tv.layoutParams?.height != cellHeightPx) {
                        tv.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeightPx)
                    }
                    if (tv.textSize != emojiTextPx) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, emojiTextPx)
                    tv.text = row
                }
                is Header -> {
                    if (tv.layoutParams?.height != headerHeightPx) {
                        tv.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, headerHeightPx)
                    }
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    tv.setTextColor(ThemeColor.readableOn(currentTheme.chassisColor, pal.keyTextSecondary, 3.0))
                    tv.text = row.title
                }
                is Hint -> {
                    tv.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeightPx * 2)
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    tv.setTextColor(ThemeColor.readableOn(currentTheme.chassisColor, pal.keyTextSecondary, 3.0))
                    tv.text = row.text
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()

    private companion object {
        const val TYPE_EMOJI = 0
        const val TYPE_HEADER = 1
        const val TYPE_HINT = 2
    }
}
