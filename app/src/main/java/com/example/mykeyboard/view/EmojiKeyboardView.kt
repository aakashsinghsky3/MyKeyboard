package com.example.mykeyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mykeyboard.R
import com.example.mykeyboard.model.EmojiCategory
import com.example.mykeyboard.model.EmojiData
import com.example.mykeyboard.model.KeyboardTheme

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
    }

    private var listener: EmojiListener? = null
    private var currentTheme: KeyboardTheme = KeyboardTheme.MATERIAL_DARK
    private var selectedCategoryIndex = 0

    private val scrollCategories: HorizontalScrollView
    private val categoryBar: LinearLayout
    private val viewPager: ViewPager2
    private val bottomBar: LinearLayout
    private var pagerAdapter: EmojiPagerAdapter? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(0, 0, 0, dpToPx(6))

        // 1. Category Bar
        scrollCategories = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(40))
            isHorizontalScrollBarEnabled = false
        }
        categoryBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
        }
        scrollCategories.addView(categoryBar)
        addView(scrollCategories)

        // 2. Swipeable Emoji ViewPager2
        viewPager = ViewPager2(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(165))
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
        addView(viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedCategoryIndex = position
                buildCategoryTabs()
                val tab = categoryBar.getChildAt(position)
                if (tab != null) {
                    scrollCategories.smoothScrollTo(tab.left - dpToPx(40), 0)
                }
            }
        })

        val initialBottomPad = getCalculatedBottomPadding()

        // 3. Bottom Bar (ABC, Space, Backspace)
        bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48) + initialBottomPad)
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), initialBottomPad)
        }
        setupBottomBar()
        addView(bottomBar)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val navInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            val finalBottom = maxOf(navInsets.bottom, getCalculatedBottomPadding())
            bottomBar.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48) + finalBottom)
            bottomBar.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), finalBottom)
            windowInsets
        }

        setupPagerAdapter()
        buildCategoryTabs()
        applyTheme(currentTheme)
    }

    fun setEmojiListener(listener: EmojiListener) {
        this.listener = listener
    }

    fun updateFixedContentHeight(targetContentHeight: Int) {
        val categoryH = dpToPx(40)
        val bottomBarBtnH = dpToPx(48)
        val availablePagerHeight = maxOf(dpToPx(120), targetContentHeight - categoryH - bottomBarBtnH)
        viewPager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, availablePagerHeight)
    }

    private fun setupPagerAdapter() {
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        pagerAdapter = EmojiPagerAdapter(
            categories = EmojiData.categories,
            theme = currentTheme,
            isTablet = isTablet,
            isLandscape = isLandscape,
            onEmojiClick = { emoji ->
                listener?.onEmojiSelected(emoji)
            }
        )
        viewPager.adapter = pagerAdapter
    }

    fun applyTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        setBackgroundColor(theme.backgroundColor)

        categoryBar.setBackgroundColor(theme.suggestionBgColor)
        buildCategoryTabs()

        bottomBar.setBackgroundColor(theme.backgroundColor)
        setupBottomBar()

        setupPagerAdapter()
        viewPager.setCurrentItem(selectedCategoryIndex, false)
    }

    private fun buildCategoryTabs() {
        categoryBar.removeAllViews()

        EmojiData.categories.forEachIndexed { index, category ->
            val isSelected = index == selectedCategoryIndex

            val tab = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                val pad = dpToPx(12)
                setPadding(pad, dpToPx(6), pad, dpToPx(6))

                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    if (isSelected) {
                        setColor(currentTheme.keyActionColor)
                    } else {
                        setColor(Color.TRANSPARENT)
                    }
                }
                background = bg

                val icon = ImageView(context).apply {
                    setImageResource(category.iconResId)
                    val iconColor = if (isSelected) currentTheme.actionTextColor else currentTheme.textColorSecondary
                    setColorFilter(iconColor)
                    layoutParams = LayoutParams(dpToPx(20), dpToPx(20))
                }
                addView(icon)

                setOnClickListener {
                    selectedCategoryIndex = index
                    viewPager.setCurrentItem(index, true)
                    buildCategoryTabs()
                }
            }

            categoryBar.addView(tab)
        }
    }

    private fun setupBottomBar() {
        bottomBar.removeAllViews()

        // ABC Key
        val abcBtn = TextView(context).apply {
            text = "ABC"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(currentTheme.textColorPrimary)
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(currentTheme.keySpecialColor)
            }
            background = bg
            layoutParams = LayoutParams(dpToPx(64), LayoutParams.MATCH_PARENT).apply {
                marginEnd = dpToPx(8)
            }
            setOnClickListener {
                listener?.onBackToAlpha()
            }
        }
        bottomBar.addView(abcBtn)

        // Space key
        val spaceBtn = TextView(context).apply {
            text = "Space"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(currentTheme.textColorSecondary)
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(currentTheme.keySpaceColor)
            }
            background = bg
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f).apply {
                marginEnd = dpToPx(8)
            }
            setOnClickListener {
                listener?.onSpace()
            }
        }
        bottomBar.addView(spaceBtn)

        // Backspace Key
        val delBtn = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(currentTheme.keySpecialColor)
            }
            background = bg
            layoutParams = LayoutParams(dpToPx(64), LayoutParams.MATCH_PARENT)

            val delIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_backspace)
                setColorFilter(currentTheme.textColorPrimary)
                layoutParams = LayoutParams(dpToPx(20), dpToPx(20))
            }
            addView(delIcon)

            setOnClickListener {
                listener?.onBackspace()
            }
        }
        bottomBar.addView(delBtn)
    }

    private inner class EmojiPagerAdapter(
        private val categories: List<EmojiCategory>,
        private val theme: KeyboardTheme,
        private val isTablet: Boolean,
        private val isLandscape: Boolean,
        private val onEmojiClick: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(val gridView: GridView) : RecyclerView.ViewHolder(gridView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val cols = if (isTablet) 14 else if (isLandscape) 12 else 8
            val grid = GridView(parent.context).apply {
                numColumns = cols
                gravity = Gravity.CENTER
                stretchMode = GridView.STRETCH_COLUMN_WIDTH
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
                verticalSpacing = dpToPx(2)
                horizontalSpacing = dpToPx(2)
            }
            return PageViewHolder(grid)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val category = categories[position]
            holder.gridView.adapter = EmojiCategoryAdapter(category.emojis, theme, onEmojiClick)
        }

        override fun getItemCount(): Int = categories.size
    }

    private inner class EmojiCategoryAdapter(
        private val emojis: List<String>,
        private val theme: KeyboardTheme,
        private val onEmojiClick: (String) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = emojis.size

        override fun getItem(position: Int): String = emojis[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val textView: TextView = if (convertView is TextView) {
                convertView
            } else {
                TextView(context).apply {
                    gravity = Gravity.CENTER
                    val size = dpToPx(40)
                    layoutParams = ViewGroup.LayoutParams(size, size)
                }
            }

            val emoji = getItem(position)
            textView.text = emoji

            val isTextBadge = emoji.contains("Anshika", ignoreCase = true) ||
                              emoji.contains("Akriti", ignoreCase = true) ||
                              emoji.any { it in 'a'..'z' || it in 'A'..'Z' || it.code in 0x1D400..0x1D7FF }

            if (isTextBadge) {
                textView.setSingleLine(true)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                textView.setTextColor(theme.actionTextColor)
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(theme.keyActionColor)
                }
                textView.background = bg
                val padH = dpToPx(4)
                textView.setPadding(padH, dpToPx(4), padH, dpToPx(4))
            } else {
                textView.setSingleLine(false)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                textView.setTypeface(android.graphics.Typeface.DEFAULT)
                textView.setTextColor(theme.textColorPrimary)
                textView.background = null
                textView.setPadding(0, dpToPx(2), 0, dpToPx(2))
            }

            textView.setOnClickListener {
                onEmojiClick(emoji)
            }

            return textView
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun getCalculatedBottomPadding(): Int {
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val sysNavHeight = getNavigationBarHeight()
        val minPad = if (isTablet) dpToPx(56) else dpToPx(48)
        return maxOf(sysNavHeight, minPad) + dpToPx(8)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
