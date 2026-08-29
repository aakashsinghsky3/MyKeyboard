package com.example.mykeyboard.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.mykeyboard.R
import com.example.mykeyboard.model.KeyLayoutHelper
import com.example.mykeyboard.model.KeyModel
import com.example.mykeyboard.model.KeyType
import com.example.mykeyboard.model.KeyboardMode
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.model.ShiftState
import com.example.mykeyboard.utils.KeyboardPreferences
import kotlin.math.abs

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface KeyboardActionListener {
        fun onTextKey(text: String)
        fun onBackspace()
        fun onSpace()
        fun onEnter(actionId: Int)
        fun onOpenEmoji()
        fun onOpenSettings()
        fun onMoveCursor(offset: Int)
        fun onPasteClipboard(text: String)
    }

    private var actionListener: KeyboardActionListener? = null
    private val preferences = KeyboardPreferences(context)
    private var currentTheme: KeyboardTheme = preferences.theme

    private var keyboardMode: KeyboardMode = KeyboardMode.ALPHA
    private var shiftState: ShiftState = ShiftState.UNSHIFTED
    private var lastShiftPressTime: Long = 0L

    private var imeOptions: Int = EditorInfo.IME_ACTION_DONE
    private var actionLabel: String? = null

    // UI containers
    private val suggestionContainer: HorizontalScrollView
    private val suggestionLayout: LinearLayout
    private val keyboardContainer: FrameLayout
    private val rowsLayout: LinearLayout
    private var emojiKeyboardView: EmojiKeyboardView? = null

    // Popup window for key preview & accents
    private var popupWindow: PopupWindow? = null
    private var popupTextView: TextView? = null

    // Accents popup
    private var accentsPopupWindow: PopupWindow? = null
    private var accentsContainer: LinearLayout? = null
    private var activeAccentIndex: Int = -1
    private var currentPopupChars: List<String> = emptyList()

    // Backspace repeat handler
    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceHeld = false
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceHeld) {
                performHapticFeedback()
                performAudioFeedback()
                actionListener?.onBackspace()
                handler.postDelayed(this, 50)
            }
        }
    }

    // Audio & Haptic services
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Common word dictionary for suggestions
    private val commonWords = listOf(
        "the", "and", "you", "that", "was", "for", "are", "with", "his", "they",
        "this", "have", "from", "one", "had", "word", "but", "not", "what", "all",
        "were", "when", "your", "can", "said", "there", "use", "each", "which", "she",
        "how", "their", "will", "other", "about", "many", "then", "them", "these", "some",
        "keyboard", "android", "awesome", "thanks", "great", "hello", "please", "yes", "good", "love"
    )

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundColor(currentTheme.backgroundColor)

        // 1. Suggestion & Quick Actions Bar
        suggestionContainer = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(42))
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(currentTheme.suggestionBgColor)
        }
        suggestionLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
        }
        suggestionContainer.addView(suggestionLayout)
        addView(suggestionContainer)

        // 2. Keyboard & Emoji Container
        keyboardContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        rowsLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(3), dpToPx(5), dpToPx(3), dpToPx(6))
        }
        keyboardContainer.addView(rowsLayout)
        addView(keyboardContainer)

        initKeyPopup()
        initAccentsPopup()
        renderSuggestions("")
        renderKeyboardLayout()
    }

    fun setActionListener(listener: KeyboardActionListener) {
        this.actionListener = listener
    }

    fun setImeOptions(options: Int, actionLabel: CharSequence?) {
        this.imeOptions = options
        this.actionLabel = actionLabel?.toString()
        if (keyboardMode != KeyboardMode.EMOJI) {
            renderKeyboardLayout()
        }
    }

    fun applyTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        setBackgroundColor(theme.backgroundColor)
        suggestionContainer.setBackgroundColor(theme.suggestionBgColor)
        emojiKeyboardView?.applyTheme(theme)
        renderSuggestions("")
        renderKeyboardLayout()
    }

    fun updateSuggestions(prefix: String) {
        renderSuggestions(prefix)
    }

    fun setShiftState(state: ShiftState) {
        if (this.shiftState != state) {
            this.shiftState = state
            if (keyboardMode == KeyboardMode.ALPHA) {
                renderKeyboardLayout()
            }
        }
    }

    fun resetToAlpha() {
        keyboardMode = KeyboardMode.ALPHA
        shiftState = ShiftState.UNSHIFTED
        showAlphaKeyboard()
    }

    // ---------------------------------------------------------------------------------------------
    // Suggestion Bar Rendering
    // ---------------------------------------------------------------------------------------------
    private fun renderSuggestions(prefix: String) {
        suggestionLayout.removeAllViews()

        // 1. Settings shortcut icon
        val settingsBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(currentTheme.textColorSecondary)
            val pad = dpToPx(7)
            setPadding(pad, pad, pad, pad)
            val size = dpToPx(34)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dpToPx(4) }
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(17).toFloat()
                setColor(currentTheme.keySpecialColor)
            }
            background = bg
            setOnClickListener {
                performHapticFeedback()
                actionListener?.onOpenSettings()
            }
        }
        suggestionLayout.addView(settingsBtn)

        // 2. Clipboard Quick Paste button if clipboard has text
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clipText.isNullOrEmpty()) {
            val clipBtn = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(currentTheme.keyActionColor)
                }
                background = bg
                setPadding(dpToPx(8), dpToPx(4), dpToPx(10), dpToPx(4))
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dpToPx(30)).apply {
                    marginEnd = dpToPx(6)
                }

                val pasteIcon = ImageView(context).apply {
                    setImageResource(R.drawable.ic_paste)
                    setColorFilter(currentTheme.actionTextColor)
                    layoutParams = LayoutParams(dpToPx(14), dpToPx(14)).apply { marginEnd = dpToPx(4) }
                }
                addView(pasteIcon)

                val pasteTv = TextView(context).apply {
                    text = if (clipText.length > 12) clipText.take(12) + "…" else clipText
                    textSize = 12f
                    setTextColor(currentTheme.actionTextColor)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(pasteTv)

                setOnClickListener {
                    performHapticFeedback()
                    actionListener?.onPasteClipboard(clipText)
                }
            }
            suggestionLayout.addView(clipBtn)
        }

        // 3. Word Predictions or Quick Punctuation
        val suggestions = if (prefix.length >= 2) {
            val matches = commonWords.filter { it.startsWith(prefix, ignoreCase = true) }.take(4)
            if (matches.isEmpty()) listOf(prefix, prefix.uppercase(), prefix.replaceFirstChar { it.uppercase() })
            else matches
        } else {
            listOf(".", ",", "?", "!", "-", "@", "#", "\"", "'", ":", ";", ")")
        }

        suggestions.forEach { word ->
            val chip = TextView(context).apply {
                text = word
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(currentTheme.suggestionTextColor)
                val padH = dpToPx(12)
                setPadding(padH, dpToPx(4), padH, dpToPx(4))
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(currentTheme.keySpecialColor)
                }
                background = bg
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dpToPx(30)).apply {
                    marginEnd = dpToPx(5)
                }
                setOnClickListener {
                    performHapticFeedback()
                    performAudioFeedback()
                    actionListener?.onTextKey(if (prefix.length >= 2) "$word " else word)
                }
            }
            suggestionLayout.addView(chip)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Keyboard Layout Rendering
    // ---------------------------------------------------------------------------------------------
    private fun renderKeyboardLayout() {
        rowsLayout.removeAllViews()

        val rows = when (keyboardMode) {
            KeyboardMode.ALPHA -> KeyLayoutHelper.getAlphaRows(preferences.isNumberRowEnabled)
            KeyboardMode.SYMBOLS_1 -> KeyLayoutHelper.getSymbols1Rows()
            KeyboardMode.SYMBOLS_2 -> KeyLayoutHelper.getSymbols2Rows()
            KeyboardMode.EMOJI -> emptyList()
        }

        val baseRowHeight = dpToPx(48)
        val scaledRowHeight = (baseRowHeight * preferences.heightScale).toInt()

        rows.forEach { keyRow ->
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, scaledRowHeight).apply {
                    bottomMargin = dpToPx(4)
                }
            }

            keyRow.forEach { keyModel ->
                val keyView = createKeyView(keyModel, scaledRowHeight)
                rowLayout.addView(keyView)
            }

            rowsLayout.addView(rowLayout)
        }
    }

    private fun createKeyView(key: KeyModel, rowHeight: Int): View {
        val keyLayout = FrameLayout(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight).apply {
                marginStart = dpToPx(2)
                marginEnd = dpToPx(2)
            }
        }

        // Key background drawable
        val bgDrawable = GradientDrawable().apply {
            cornerRadius = dpToPx(7).toFloat()
            val color = when (key.type) {
                KeyType.ENTER -> currentTheme.keyActionColor
                KeyType.SPACE -> currentTheme.keySpaceColor
                KeyType.SHIFT -> {
                    if (shiftState != ShiftState.UNSHIFTED) currentTheme.keyActionColor
                    else currentTheme.keySpecialColor
                }
                KeyType.BACKSPACE, KeyType.MODE_CHANGE, KeyType.EMOJI, KeyType.COMMA, KeyType.PERIOD -> currentTheme.keySpecialColor
                else -> currentTheme.keyNormalColor
            }
            setColor(color)
        }
        keyLayout.background = bgDrawable

        // Content
        when (key.type) {
            KeyType.SHIFT -> {
                val shiftIcon = ImageView(context).apply {
                    val res = if (shiftState == ShiftState.CAPS_LOCKED) R.drawable.ic_capslock else R.drawable.ic_shift
                    setImageResource(res)
                    val tint = if (shiftState != ShiftState.UNSHIFTED) currentTheme.actionTextColor else currentTheme.textColorPrimary
                    setColorFilter(tint)
                    val pad = dpToPx(11)
                    setPadding(pad, pad, pad, pad)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(shiftIcon)
            }
            KeyType.BACKSPACE -> {
                val delIcon = ImageView(context).apply {
                    setImageResource(R.drawable.ic_backspace)
                    setColorFilter(currentTheme.textColorPrimary)
                    val pad = dpToPx(11)
                    setPadding(pad, pad, pad, pad)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(delIcon)
            }
            KeyType.ENTER -> {
                val (enterIconRes, enterText) = getActionInfo()
                if (enterIconRes != 0) {
                    val enterIcon = ImageView(context).apply {
                        setImageResource(enterIconRes)
                        setColorFilter(currentTheme.actionTextColor)
                        val pad = dpToPx(11)
                        setPadding(pad, pad, pad, pad)
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    keyLayout.addView(enterIcon)
                } else {
                    val enterTv = TextView(context).apply {
                        text = enterText
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setTextColor(currentTheme.actionTextColor)
                        typeface = Typeface.DEFAULT_BOLD
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    keyLayout.addView(enterTv)
                }
            }
            KeyType.SPACE -> {
                val spaceTv = TextView(context).apply {
                    text = "English"
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(currentTheme.textColorSecondary)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(spaceTv)
            }
            KeyType.EMOJI -> {
                val emojiTv = TextView(context).apply {
                    text = "😀"
                    textSize = 18f
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(emojiTv)
            }
            KeyType.MODE_CHANGE -> {
                val modeTv = TextView(context).apply {
                    text = key.primaryText
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(currentTheme.textColorPrimary)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(modeTv)
            }
            else -> {
                // Character Key with Optional Top-Right Alt Character
                val mainTv = TextView(context).apply {
                    val charText = if (shiftState != ShiftState.UNSHIFTED) key.shiftText else key.primaryText
                    text = charText
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(currentTheme.textColorPrimary)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                keyLayout.addView(mainTv)

                if (key.altText.isNotEmpty() && keyboardMode == KeyboardMode.ALPHA) {
                    val altTv = TextView(context).apply {
                        text = key.altText
                        textSize = 9f
                        gravity = Gravity.END or Gravity.TOP
                        setTextColor(currentTheme.textColorSecondary)
                        setPadding(0, dpToPx(3), dpToPx(4), 0)
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    keyLayout.addView(altTv)
                }
            }
        }

        // Attach Touch & Gesture Listeners
        attachTouchListener(keyLayout, key)

        return keyLayout
    }

    private fun getActionInfo(): Pair<Int, String> {
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_SEARCH -> Pair(R.drawable.ic_search, "Search")
            EditorInfo.IME_ACTION_SEND -> Pair(R.drawable.ic_send, "Send")
            EditorInfo.IME_ACTION_GO -> Pair(R.drawable.ic_next, "Go")
            EditorInfo.IME_ACTION_NEXT -> Pair(R.drawable.ic_next, "Next")
            EditorInfo.IME_ACTION_DONE -> Pair(R.drawable.ic_done, "Done")
            else -> Pair(R.drawable.ic_enter, actionLabel ?: "Enter")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Touch & Gesture Handling
    // ---------------------------------------------------------------------------------------------
    private fun attachTouchListener(view: View, key: KeyModel) {
        var downX = 0f
        var isCursorSliding = false
        var lastCursorMoveX = 0f
        var isLongPressHandled = false
        val longPressRunnable = Runnable {
            if (key.popupChars.isNotEmpty() && preferences.isPopupEnabled) {
                isLongPressHandled = true
                dismissPopup()
                showAccentsPopup(view, key.popupChars)
            }
        }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    lastCursorMoveX = event.rawX
                    isCursorSliding = false
                    isLongPressHandled = false

                    performHapticFeedback()
                    performAudioFeedback()
                    animateKeyPress(v, true)

                    if (key.type == KeyType.BACKSPACE) {
                        isBackspaceHeld = true
                        actionListener?.onBackspace()
                        handler.postDelayed(backspaceRepeatRunnable, 350)
                    } else if (key.type == KeyType.CHARACTER || key.type == KeyType.COMMA || key.type == KeyType.PERIOD) {
                        if (preferences.isPopupEnabled) {
                            val text = if (shiftState != ShiftState.UNSHIFTED) key.shiftText else key.primaryText
                            showKeyPopup(v, text)
                        }
                        handler.postDelayed(longPressRunnable, 350)
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX

                    // Spacebar Cursor Slider
                    if (key.type == KeyType.SPACE && abs(dx) > dpToPx(12)) {
                        isCursorSliding = true
                        val diff = event.rawX - lastCursorMoveX
                        val step = dpToPx(14)
                        if (abs(diff) >= step) {
                            val offset = if (diff > 0) 1 else -1
                            actionListener?.onMoveCursor(offset)
                            performHapticFeedback()
                            lastCursorMoveX = event.rawX
                        }
                    }

                    // Accents popup navigation
                    if (isLongPressHandled && accentsPopupWindow?.isShowing == true) {
                        handleAccentsMove(event.rawX)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animateKeyPress(v, false)
                    handler.removeCallbacks(longPressRunnable)
                    handler.removeCallbacks(backspaceRepeatRunnable)
                    isBackspaceHeld = false
                    dismissPopup()

                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        if (isLongPressHandled && accentsPopupWindow?.isShowing == true) {
                            if (activeAccentIndex in currentPopupChars.indices) {
                                val accent = currentPopupChars[activeAccentIndex]
                                actionListener?.onTextKey(accent)
                            }
                            dismissAccentsPopup()
                        } else if (!isCursorSliding && !isLongPressHandled) {
                            handleKeyClick(key)
                        }
                    } else {
                        dismissAccentsPopup()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun handleKeyClick(key: KeyModel) {
        when (key.type) {
            KeyType.CHARACTER, KeyType.COMMA, KeyType.PERIOD -> {
                val text = if (shiftState != ShiftState.UNSHIFTED) key.shiftText else key.primaryText
                actionListener?.onTextKey(text)
                if (shiftState == ShiftState.SHIFTED_ONCE) {
                    shiftState = ShiftState.UNSHIFTED
                    renderKeyboardLayout()
                }
            }
            KeyType.SPACE -> {
                actionListener?.onSpace()
            }
            KeyType.SHIFT -> {
                val now = System.currentTimeMillis()
                if (now - lastShiftPressTime < 300) {
                    // Double tap for CAPS LOCK
                    shiftState = if (shiftState == ShiftState.CAPS_LOCKED) ShiftState.UNSHIFTED else ShiftState.CAPS_LOCKED
                } else {
                    shiftState = when (shiftState) {
                        ShiftState.UNSHIFTED -> ShiftState.SHIFTED_ONCE
                        ShiftState.SHIFTED_ONCE -> ShiftState.UNSHIFTED
                        ShiftState.CAPS_LOCKED -> ShiftState.UNSHIFTED
                    }
                }
                lastShiftPressTime = now
                renderKeyboardLayout()
            }
            KeyType.MODE_CHANGE -> {
                keyboardMode = when (key.primaryText) {
                    "?123" -> KeyboardMode.SYMBOLS_1
                    "=\\<" -> KeyboardMode.SYMBOLS_2
                    "ABC" -> KeyboardMode.ALPHA
                    else -> KeyboardMode.ALPHA
                }
                renderKeyboardLayout()
            }
            KeyType.EMOJI -> {
                showEmojiKeyboard()
            }
            KeyType.ENTER -> {
                val action = imeOptions and EditorInfo.IME_MASK_ACTION
                actionListener?.onEnter(action)
            }
            KeyType.SETTINGS -> {
                actionListener?.onOpenSettings()
            }
            KeyType.BACKSPACE -> {
                // Already handled in ACTION_DOWN for instant responsiveness
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Key Press Animation & Popup
    // ---------------------------------------------------------------------------------------------
    private fun animateKeyPress(view: View, isPressed: Boolean) {
        val scale = if (isPressed) 0.92f else 1.0f
        val animX = ObjectAnimator.ofFloat(view, "scaleX", scale).apply { duration = 80 }
        val animY = ObjectAnimator.ofFloat(view, "scaleY", scale).apply { duration = 80 }
        AnimatorSet().apply {
            playTogether(animX, animY)
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    private fun initKeyPopup() {
        val popupView = TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            val pad = dpToPx(8)
            setPadding(pad, pad, pad, pad)
        }
        popupTextView = popupView
        popupWindow = PopupWindow(
            popupView,
            dpToPx(56),
            dpToPx(64)
        ).apply {
            isTouchable = false
            animationStyle = android.R.style.Animation_Toast
        }
    }

    private fun showKeyPopup(anchor: View, text: String) {
        popupTextView?.let { tv ->
            tv.text = text
            tv.setTextColor(currentTheme.popupTextColor)
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(currentTheme.popupBgColor)
                setStroke(dpToPx(1), currentTheme.rippleColor)
            }
            tv.background = bg

            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val x = location[0] + (anchor.width - dpToPx(56)) / 2
            val y = location[1] - dpToPx(68)

            try {
                popupWindow?.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
            } catch (_: Exception) {}
        }
    }

    private fun dismissPopup() {
        try {
            if (popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
            }
        } catch (_: Exception) {}
    }

    private fun initAccentsPopup() {
        accentsContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            val pad = dpToPx(4)
            setPadding(pad, pad, pad, pad)
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(currentTheme.popupBgColor)
                setStroke(dpToPx(1), currentTheme.rippleColor)
            }
            background = bg
        }
        accentsPopupWindow = PopupWindow(
            accentsContainer,
            LayoutParams.WRAP_CONTENT,
            dpToPx(52)
        ).apply {
            isTouchable = false
        }
    }

    private fun showAccentsPopup(anchor: View, chars: List<String>) {
        currentPopupChars = chars
        activeAccentIndex = -1
        accentsContainer?.removeAllViews()

        chars.forEachIndexed { index, char ->
            val tv = TextView(context).apply {
                text = char
                textSize = 20f
                gravity = Gravity.CENTER
                val size = dpToPx(42)
                layoutParams = LayoutParams(size, size)
                setTextColor(currentTheme.popupTextColor)
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(Color.TRANSPARENT)
                }
                background = bg
            }
            accentsContainer?.addView(tv)
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val popupWidth = chars.size * dpToPx(42) + dpToPx(8)
        var x = location[0] + anchor.width / 2 - popupWidth / 2
        if (x < dpToPx(8)) x = dpToPx(8)
        val y = location[1] - dpToPx(58)

        try {
            accentsPopupWindow?.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        } catch (_: Exception) {}
    }

    private fun handleAccentsMove(rawX: Float) {
        val container = accentsContainer ?: return
        val count = container.childCount
        if (count == 0) return

        for (i in 0 until count) {
            val child = container.getChildAt(i) as? TextView ?: continue
            val loc = IntArray(2)
            child.getLocationOnScreen(loc)
            val left = loc[0]
            val right = left + child.width

            if (rawX in left.toFloat()..right.toFloat()) {
                if (activeAccentIndex != i) {
                    activeAccentIndex = i
                    performHapticFeedback()
                    updateAccentsHighlight()
                }
                return
            }
        }
    }

    private fun updateAccentsHighlight() {
        val container = accentsContainer ?: return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) as? TextView ?: continue
            val bg = child.background as? GradientDrawable ?: continue
            if (i == activeAccentIndex) {
                bg.setColor(currentTheme.keyActionColor)
                child.setTextColor(currentTheme.actionTextColor)
            } else {
                bg.setColor(Color.TRANSPARENT)
                child.setTextColor(currentTheme.popupTextColor)
            }
        }
    }

    private fun dismissAccentsPopup() {
        try {
            if (accentsPopupWindow?.isShowing == true) {
                accentsPopupWindow?.dismiss()
            }
        } catch (_: Exception) {}
        activeAccentIndex = -1
    }

    // ---------------------------------------------------------------------------------------------
    // Emoji View Switching
    // ---------------------------------------------------------------------------------------------
    private fun showEmojiKeyboard() {
        keyboardMode = KeyboardMode.EMOJI
        rowsLayout.visibility = View.GONE

        if (emojiKeyboardView == null) {
            emojiKeyboardView = EmojiKeyboardView(context).apply {
                applyTheme(currentTheme)
                setEmojiListener(object : EmojiKeyboardView.EmojiListener {
                    override fun onEmojiSelected(emoji: String) {
                        performHapticFeedback()
                        performAudioFeedback()
                        actionListener?.onTextKey(emoji)
                    }

                    override fun onBackToAlpha() {
                        performHapticFeedback()
                        showAlphaKeyboard()
                    }

                    override fun onBackspace() {
                        performHapticFeedback()
                        performAudioFeedback()
                        actionListener?.onBackspace()
                    }

                    override fun onSpace() {
                        performHapticFeedback()
                        performAudioFeedback()
                        actionListener?.onSpace()
                    }
                })
            }
            keyboardContainer.addView(emojiKeyboardView)
        } else {
            emojiKeyboardView?.visibility = View.VISIBLE
        }
    }

    private fun showAlphaKeyboard() {
        keyboardMode = KeyboardMode.ALPHA
        emojiKeyboardView?.visibility = View.GONE
        rowsLayout.visibility = View.VISIBLE
        renderKeyboardLayout()
    }

    // ---------------------------------------------------------------------------------------------
    // Haptic & Sound Feedback
    // ---------------------------------------------------------------------------------------------
    private fun performHapticFeedback() {
        if (!preferences.isHapticEnabled) return
        try {
            val duration = preferences.hapticDuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    private fun performAudioFeedback() {
        if (!preferences.isSoundEnabled) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
