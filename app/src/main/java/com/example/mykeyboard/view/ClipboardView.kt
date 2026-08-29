package com.example.mykeyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.mykeyboard.R
import com.example.mykeyboard.engine.ClipboardHistoryManager
import com.example.mykeyboard.engine.ClipboardItem
import com.example.mykeyboard.model.KeyboardTheme

class ClipboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface ClipboardListener {
        fun onClipSelected(text: String)
        fun onCloseClipboard()
    }

    private var listener: ClipboardListener? = null
    private var currentTheme: KeyboardTheme = KeyboardTheme.MATERIAL_DARK
    private val clipboardManager = ClipboardHistoryManager(context)

    private val headerLayout: LinearLayout
    private val itemsContainer: LinearLayout
    private val scrollView: ScrollView

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(245))
        setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))

        // 1. Header (Title, Clear Unpinned, Close)
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(38)).apply {
                bottomMargin = dpToPx(6)
            }
        }
        setupHeader()
        addView(headerLayout)

        // 2. Scrollable Clips List
        scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            isVerticalScrollBarEnabled = false
        }
        itemsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(itemsContainer)
        addView(scrollView)

        refreshClips()
    }

    fun setClipboardListener(listener: ClipboardListener) {
        this.listener = listener
    }

    fun applyTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        setBackgroundColor(theme.backgroundColor)
        setupHeader()
        refreshClips()
    }

    private fun setupHeader() {
        headerLayout.removeAllViews()

        // Title
        val titleTv = TextView(context).apply {
            text = "Clipboard History"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(currentTheme.textColorPrimary)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerLayout.addView(titleTv)

        // Clear Unpinned Button
        val clearBtn = TextView(context).apply {
            text = "Clear All"
            textSize = 12f
            setTextColor(currentTheme.textColorSecondary)
            val pad = dpToPx(6)
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                clipboardManager.clearUnpinned()
                refreshClips()
            }
        }
        headerLayout.addView(clearBtn)

        // Close / Back button
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_clear)
            setColorFilter(currentTheme.textColorPrimary)
            val pad = dpToPx(8)
            setPadding(pad, pad, pad, pad)
            val size = dpToPx(38)
            layoutParams = LayoutParams(size, size).apply { marginStart = dpToPx(8) }
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(19).toFloat()
                setColor(currentTheme.keySpecialColor)
            }
            background = bg
            setOnClickListener {
                listener?.onCloseClipboard()
            }
        }
        headerLayout.addView(closeBtn)
    }

    fun refreshClips() {
        itemsContainer.removeAllViews()
        val clips = clipboardManager.getAllClips()

        if (clips.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "No clips in history yet.\nCopied text will automatically appear here for 1-tap pasting."
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(currentTheme.textColorSecondary)
                setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
            }
            itemsContainer.addView(emptyTv)
            return
        }

        clips.forEach { clip ->
            val card = createClipCard(clip)
            itemsContainer.addView(card)
        }
    }

    private fun createClipCard(clip: ClipboardItem): View {
        val cardLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = dpToPx(10)
            setPadding(pad, pad, pad, pad)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(6)
            }

            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(10).toFloat()
                setColor(currentTheme.keyNormalColor)
                if (clip.isPinned) {
                    setStroke(dpToPx(1), currentTheme.keyActionColor)
                }
            }
            background = bg
        }

        // Clip Text & Time
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val textTv = TextView(context).apply {
            text = clip.text
            textSize = 13f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(currentTheme.textColorPrimary)
        }
        textContainer.addView(textTv)

        val timeTv = TextView(context).apply {
            text = formatTime(clip.timestamp)
            textSize = 10f
            setTextColor(currentTheme.textColorSecondary)
            setPadding(0, dpToPx(2), 0, 0)
        }
        textContainer.addView(timeTv)

        cardLayout.addView(textContainer)

        // 1-Tap Paste on click
        cardLayout.setOnClickListener {
            listener?.onClipSelected(clip.text)
        }

        // Actions: Pin and Delete
        val pinBtn = ImageView(context).apply {
            setImageResource(if (clip.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin)
            val iconTint = if (clip.isPinned) currentTheme.keyActionColor else currentTheme.textColorSecondary
            setColorFilter(iconTint)
            val pad = dpToPx(6)
            setPadding(pad, pad, pad, pad)
            val size = dpToPx(30)
            layoutParams = LayoutParams(size, size).apply { marginStart = dpToPx(4) }
            setOnClickListener {
                clipboardManager.togglePin(clip.id)
                refreshClips()
            }
        }
        cardLayout.addView(pinBtn)

        val deleteBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_delete)
            setColorFilter(currentTheme.textColorSecondary)
            val pad = dpToPx(6)
            setPadding(pad, pad, pad, pad)
            val size = dpToPx(30)
            layoutParams = LayoutParams(size, size).apply { marginStart = dpToPx(4) }
            setOnClickListener {
                clipboardManager.deleteClip(clip.id)
                refreshClips()
            }
        }
        cardLayout.addView(deleteBtn)

        return cardLayout
    }

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
