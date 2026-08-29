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
import androidx.core.content.ContextCompat
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

    private val categoryBar: LinearLayout
    private val emojiGrid: GridView
    private val bottomBar: LinearLayout
    private val emojiAdapter: EmojiAdapter

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(0, 0, 0, dpToPx(6))

        // 1. Category Bar
        val scrollCategories = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(38))
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

        // 2. Emoji Grid (Responsive columns for tablets and foldables)
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val cols = if (isTablet) 14 else if (isLandscape) 12 else 8

        emojiGrid = GridView(context).apply {
            numColumns = cols
            gravity = Gravity.CENTER
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(155))
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            verticalSpacing = dpToPx(2)
            horizontalSpacing = dpToPx(2)
        }
        emojiAdapter = EmojiAdapter()
        emojiGrid.adapter = emojiAdapter
        addView(emojiGrid)

        // 3. Bottom Bar (ABC, Space, Backspace)
        bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(10))
        }
        setupBottomBar()
        addView(bottomBar)

        buildCategoryTabs()
        applyTheme(currentTheme)
    }

    fun setEmojiListener(listener: EmojiListener) {
        this.listener = listener
    }

    fun applyTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        setBackgroundColor(theme.backgroundColor)

        categoryBar.setBackgroundColor(theme.suggestionBgColor)
        buildCategoryTabs()

        bottomBar.setBackgroundColor(theme.backgroundColor)
        setupBottomBar()

        emojiAdapter.notifyDataSetChanged()
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
                    buildCategoryTabs()
                    emojiAdapter.updateCategory(EmojiData.categories[index])
                    emojiGrid.smoothScrollToPosition(0)
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

    private inner class EmojiAdapter : BaseAdapter() {
        private var currentCategory: EmojiCategory = EmojiData.categories[0]

        fun updateCategory(category: EmojiCategory) {
            currentCategory = category
            notifyDataSetChanged()
        }

        override fun getCount(): Int = currentCategory.emojis.size

        override fun getItem(position: Int): String = currentCategory.emojis[position]

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

            if (emoji.length > 2) {
                textView.setSingleLine(true)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                textView.setTextColor(currentTheme.actionTextColor)
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(currentTheme.keyActionColor)
                }
                textView.background = bg
                val padH = dpToPx(2)
                textView.setPadding(padH, dpToPx(4), padH, dpToPx(4))
            } else {
                textView.setSingleLine(false)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                textView.setTypeface(android.graphics.Typeface.DEFAULT)
                textView.setTextColor(currentTheme.textColorPrimary)
                textView.background = null
                textView.setPadding(0, dpToPx(2), 0, dpToPx(2))
            }

            textView.setOnClickListener {
                listener?.onEmojiSelected(emoji)
            }

            return textView
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
