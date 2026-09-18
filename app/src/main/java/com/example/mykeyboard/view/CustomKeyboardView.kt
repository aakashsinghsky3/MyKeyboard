package com.example.mykeyboard.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import com.example.mykeyboard.model.KeyboardLanguage
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.example.mykeyboard.R
import com.example.mykeyboard.engine.PredictionEngine
import com.example.mykeyboard.engine.SuggestionResult
import com.example.mykeyboard.model.KeyLayoutHelper
import com.example.mykeyboard.model.KeyModel
import com.example.mykeyboard.model.KeyType
import com.example.mykeyboard.model.KeyboardMode
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.model.ShiftState
import com.example.mykeyboard.utils.KeyboardPreferences
import com.example.mykeyboard.theme.KeyColorResolver
import com.example.mykeyboard.theme.KeyLabelMetrics
import com.example.mykeyboard.theme.KeyMotion
import com.example.mykeyboard.theme.KeyRole
import com.example.mykeyboard.theme.KeyView
import com.example.mykeyboard.theme.KeyboardBackgroundDrawable
import com.example.mykeyboard.theme.KeyboardThemeManager
import com.example.mykeyboard.theme.KeycapDrawable
import com.example.mykeyboard.theme.KeycapResolver
import com.example.mykeyboard.theme.KeycapSpec
import com.example.mykeyboard.theme.StripStyle
import com.example.mykeyboard.theme.StripDividerDrawable
import com.example.mykeyboard.theme.ThemeColor
import android.util.TypedValue
import java.io.File
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
        fun onUndo()
        fun onRedo()
        fun onAddWordToDictionary(word: String)
        fun onDeleteSuggestedWord(word: String)
        fun onReplaceText(oldText: String, newText: String)
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
    private val suggestionContainer: LinearLayout
    private val toolbarActionsLayout: LinearLayout
    private val candidatesLayout: LinearLayout
    private val keyboardContainer: FrameLayout
    private val rowsLayout: LinearLayout
    private var emojiKeyboardView: EmojiKeyboardView? = null
    private var clipboardView: ClipboardView? = null

    // Suggestion Candidate Views
    private lateinit var candidateLeftTv: TextView
    private lateinit var candidateCenterTv: TextView
    private lateinit var candidateRightTv: TextView

    // Prediction Engine
    private val predictionEngine = PredictionEngine(context)
    private var currentSuggestionResult: SuggestionResult? = null

    // Popup window for key preview & accents
    private var popupWindow: PopupWindow? = null
    private var popupTextView: TextView? = null

    // Accents popup
    private var accentsPopupWindow: PopupWindow? = null
    private var accentsContainer: LinearLayout? = null
    private var activeAccentIndex: Int = -1
    private var currentPopupChars: List<String> = emptyList()

    // Backspace & Space repeat handler
    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceHeld = false
    private var spaceLongPressRunnable: Runnable? = null
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceHeld) {
                performHapticFeedback()
                performAudioFeedback()
                actionListener?.onBackspace()
                handler.postDelayed(this, 45)
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

    // Shift & letter views tracking for instantaneous in-place Shift toggle (no view rebuild)
    private val letterKeyViews = mutableListOf<Pair<TextView, KeyModel>>()
    private var shiftKeyIcon: ImageView? = null
    private var shiftKeyDrawable: KeycapDrawable? = null
    private var shiftSpecIdle: KeycapSpec? = null
    private var shiftSpecActive: KeycapSpec? = null

    // Theme render context – rebuilt only in applyTheme(), shared by every key.
    private var keycapResolver = KeycapResolver(currentTheme, context.resources.displayMetrics.density)
    private var keyMotion = KeyMotion(currentTheme.pressEffect, false)
    private var labelTypeface: Typeface = Typeface.DEFAULT
    private var labelTypefaceStrong: Typeface = Typeface.DEFAULT_BOLD
    // Declared before init{}: init starts the async photo load that compares against this.
    private var backgroundRequestPath: String? = null
    private var renderedLanguageId: String? = null
    private var renderedRowsHeight = 0
    private var pendingShiftRender = false

    // Navigation-bar spacing state (see readSystemInsets); declared before init{} so nothing
    // resets it after the first layout.
    private var panelResizeRunnable: Runnable? = null
    private var systemBottomInset = 0
    private var systemLeftInset = 0
    private var systemRightInset = 0

    // Long press & multi-touch management
    private var activeLongPressRunnable: Runnable? = null
    private var activeLongPressKey: KeyModel? = null
    private var isSpaceHeld = false
    private var isSpaceCommitted = false

    private var pendingKeyView: View? = null
    private var pendingKeyModel: KeyModel? = null
    private var isPendingKeyCommitted = false
    private var isPendingKeyLongPressed = false

    private fun cancelPendingLongPress() {
        activeLongPressRunnable?.let { handler.removeCallbacks(it) }
        activeLongPressRunnable = null
        activeLongPressKey = null
        dismissPopup()
    }

    private fun commitPendingKeyIfNeeded() {
        cancelPendingLongPress()
        val key = pendingKeyModel
        val v = pendingKeyView
        if (key != null && !isPendingKeyCommitted && !isPendingKeyLongPressed) {
            isPendingKeyCommitted = true
            handleKeyClick(key)
        }
        v?.isPressed = false
        pendingKeyView = null
        pendingKeyModel = null
        isPendingKeyLongPressed = false
        isPendingKeyCommitted = false
        dismissPopup()
    }

    init {
        isMotionEventSplittingEnabled = true
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 1. Suggestion & Smart Action Toolbar
        suggestionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(42))
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }

        toolbarActionsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }
        suggestionContainer.addView(toolbarActionsLayout)

        candidatesLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
        }
        suggestionContainer.addView(candidatesLayout)

        addView(suggestionContainer)

        // 2. Keyboard Views Container
        keyboardContainer = FrameLayout(context).apply {
            isMotionEventSplittingEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val initialBottomPad = dpToPx(6)
        rowsLayout = LinearLayout(context).apply {
            isMotionEventSplittingEnabled = true
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(2), dpToPx(3), dpToPx(2), initialBottomPad)
        }
        keyboardContainer.addView(rowsLayout)
        addView(keyboardContainer)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            readSystemInsets(windowInsets)
            // Never consume: other views (and the system) still need these.
            windowInsets
        }

        rebuildRenderContext()
        initKeyPopup()
        initAccentsPopup()
        setupToolbarAndCandidates()
        applyBackgroundAndTheme()
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
        if (emojiSearchActive) endEmojiSearch(reopenEmoji = false)
        this.currentTheme = theme
        rebuildRenderContext()
        refreshPopupStyles()
        applyBackgroundAndTheme()
        emojiKeyboardView?.applyTheme(theme, keycapResolver, keyMotion)
        clipboardView?.applyTheme(theme)
        setupToolbarAndCandidates()
        renderKeyboardLayout()
    }

    private fun rebuildRenderContext() {
        val density = context.resources.displayMetrics.density
        keycapResolver = KeycapResolver(currentTheme, density, preferences.isHighContrastEnabled, preferences.effectivePressEffect)
        keyMotion = KeyMotion(preferences.effectivePressEffect, preferences.isReduceMotionEnabled)
        val typo = currentTheme.typography
        labelTypeface = Typeface.create(typo.fontFamily, if (typo.bold) Typeface.BOLD else Typeface.NORMAL)
        labelTypefaceStrong = Typeface.create(typo.fontFamily, Typeface.BOLD)
    }

    private fun applyBackgroundAndTheme() {
        val density = context.resources.displayMetrics.density
        val customPath = preferences.customBgPath
        val stripBg = currentTheme.palette.suggestionBg
        if (!customPath.isNullOrEmpty() && File(customPath).exists()) {
            backgroundRequestPath = customPath
            val cachedPhoto = KeyboardThemeManager.peek(customPath)
            if (cachedPhoto != null) {
                applyPhotoBackground(cachedPhoto)
            } else {
                // Show the theme chassis immediately; swap in the photo when decoded off-thread.
                background = KeyboardBackgroundDrawable(currentTheme.background, density)
                KeyboardThemeManager.loadPhoto(customPath) { info ->
                    if (info != null && backgroundRequestPath == customPath) applyPhotoBackground(info)
                }
            }
            suggestionContainer.setBackgroundColor(ThemeColor.withAlpha(stripBg, minOf(ThemeColor.alpha(stripBg), 150)))
            return
        }
        backgroundRequestPath = null
        background = KeyboardBackgroundDrawable(currentTheme.background, density)
        suggestionContainer.setBackgroundColor(stripBg)
    }

    private fun applyPhotoBackground(info: KeyboardThemeManager.PhotoInfo) {
        val (scrimColor, strength) = KeyboardThemeManager.scrimFor(currentTheme, info, preferences.customBgOpacity)
        background = KeyboardBackgroundDrawable(
            currentTheme.background, context.resources.displayMetrics.density,
            photo = info.bitmap, scrimStrength = strength, scrimColor = scrimColor
        )
    }

    private var activeRephraseRawInput: String? = null

    fun setSuggestionsResult(result: SuggestionResult, prefix: String) {
        if (emojiSearchActive) return
        activeRephraseRawInput = null
        currentSuggestionResult = result

        val hasLeft = !result.left.isNullOrEmpty()
        val hasCenter = !result.center.isNullOrEmpty()
        val hasRight = !result.right.isNullOrEmpty()
        val hasAnyCandidate = hasLeft || hasCenter || hasRight

        // Always keep toolbar actions (Settings, Clipboard, Dialpad) visible!
        toolbarActionsLayout.visibility = View.VISIBLE

        if (prefix.isNotEmpty() || hasAnyCandidate) {
            candidatesLayout.visibility = View.VISIBLE

            candidateLeftTv.visibility = if (hasLeft) View.VISIBLE else View.INVISIBLE
            candidateCenterTv.visibility = if (hasCenter) View.VISIBLE else View.INVISIBLE
            candidateRightTv.visibility = if (hasRight) View.VISIBLE else View.INVISIBLE
        } else {
            candidatesLayout.visibility = View.INVISIBLE

            candidateLeftTv.visibility = View.INVISIBLE
            candidateCenterTv.visibility = View.INVISIBLE
            candidateRightTv.visibility = View.INVISIBLE
        }

        // Left Candidate
        candidateLeftTv.text = result.left ?: ""

        // Center Candidate (Primary / Autocorrect)
        candidateCenterTv.text = result.center ?: ""

        // Right Candidate
        candidateRightTv.text = result.right ?: ""
    }

    fun updatePredictions(prefix: String, previousWords: List<String>) {
        val result = predictionEngine.getSuggestions(prefix, previousWords, preferences.autoCorrectMode, preferences.currentLanguage)
        setSuggestionsResult(result, prefix)
    }

    fun showProfessionalSuggestions(rawSentence: String, options: List<String>) {
        if (emojiSearchActive) return
        activeRephraseRawInput = rawSentence

        candidateCenterTv.text = options.getOrNull(0) ?: ""
        candidateCenterTv.setTypeface(Typeface.DEFAULT_BOLD)
        candidateCenterTv.setTextColor(currentTheme.actionTextColor)
        val bgCenter = GradientDrawable().apply {
            cornerRadius = dpToPx(10).toFloat()
            setColor(currentTheme.keyActionColor)
        }
        candidateCenterTv.background = bgCenter

        candidateLeftTv.text = options.getOrNull(1) ?: ""
        candidateLeftTv.visibility = if (options.getOrNull(1).isNullOrEmpty()) View.INVISIBLE else View.VISIBLE

        candidateRightTv.text = options.getOrNull(2) ?: ""
        candidateRightTv.visibility = if (options.getOrNull(2).isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
    }

    /** Called by auto-capitalisation only. */
    fun setShiftState(state: ShiftState) {
        // Devanagari has no capitals and its Shift key swaps consonants for vowels, so the layout
        // must never flip on its own after a sentence end.
        if (languageUsesShiftLayout()) return
        if (this.shiftState != state) {
            this.shiftState = state
            if (keyboardMode == KeyboardMode.ALPHA) {
                applyShiftStateChange()
            }
        }
    }

    /** Re-renders letters only when the active language was changed elsewhere (settings app). */
    fun refreshLanguageIfChanged() {
        if (preferences.currentLanguage != renderedLanguageId && keyboardMode != KeyboardMode.EMOJI) {
            shiftState = ShiftState.UNSHIFTED
            renderKeyboardLayout()
        }
    }

    fun resetToAlpha() {
        if (emojiSearchActive) endEmojiSearch(reopenEmoji = false)
        keyboardMode = KeyboardMode.ALPHA
        shiftState = ShiftState.UNSHIFTED
        showAlphaKeyboard()
    }

    fun refreshClipboard() {
        clipboardView?.refreshClips()
    }

    // ---------------------------------------------------------------------------------------------
    // Toolbar & 3-Candidate Suggestion Strip
    // ---------------------------------------------------------------------------------------------
    private fun setupToolbarAndCandidates() {
        toolbarActionsLayout.removeAllViews()
        candidatesLayout.removeAllViews()
        candidateIndex = 0

        // 1. Settings Shortcut
        val settingsBtn = createToolbarIconButton(R.drawable.ic_settings) {
            actionListener?.onOpenSettings()
        }
        toolbarActionsLayout.addView(settingsBtn)

        // 2. Clipboard Manager Button
        val clipBtn = createToolbarIconButton(R.drawable.ic_paste) {
            if (clipboardView?.visibility == View.VISIBLE) {
                showAlphaKeyboard()
            } else {
                showClipboardView()
            }
        }
        toolbarActionsLayout.addView(clipBtn)

        // 3. Phone Dialpad Button
        val dialpadBtn = createToolbarIconButton(R.drawable.ic_numbers) {
            if (keyboardMode == KeyboardMode.DIALPAD) {
                showAlphaKeyboard()
            } else {
                showDialpadKeyboard()
            }
        }
        toolbarActionsLayout.addView(dialpadBtn)

        // 5. 3 Candidate TextViews in CandidatesLayout
        candidateLeftTv = createCandidateTextView().apply {
            setOnClickListener {
                text.toString().takeIf { it.isNotEmpty() }?.let { candidateText ->
                    performHapticFeedback()
                    performAudioFeedback()
                    val raw = activeRephraseRawInput
                    if (raw != null) {
                        actionListener?.onReplaceText(raw, candidateText)
                        activeRephraseRawInput = null
                    } else {
                        actionListener?.onTextKey("$candidateText ")
                    }
                }
            }
            setOnLongClickListener {
                showRemoveSuggestionDialog(text.toString())
                true
            }
        }
        candidatesLayout.addView(candidateLeftTv)

        candidateCenterTv = createCandidateTextView().apply {
            setOnClickListener {
                text.toString().takeIf { it.isNotEmpty() }?.let { candidateText ->
                    performHapticFeedback()
                    performAudioFeedback()
                    val raw = activeRephraseRawInput
                    if (raw != null) {
                        actionListener?.onReplaceText(raw, candidateText)
                        activeRephraseRawInput = null
                    } else {
                        actionListener?.onTextKey("$candidateText ")
                    }
                }
            }
            setOnLongClickListener {
                showRemoveSuggestionDialog(text.toString())
                true
            }
        }
        candidatesLayout.addView(candidateCenterTv)

        candidateRightTv = createCandidateTextView().apply {
            setOnClickListener {
                text.toString().takeIf { it.isNotEmpty() }?.let { candidateText ->
                    performHapticFeedback()
                    performAudioFeedback()
                    val raw = activeRephraseRawInput
                    if (raw != null) {
                        actionListener?.onReplaceText(raw, candidateText)
                        activeRephraseRawInput = null
                    } else {
                        actionListener?.onTextKey("$candidateText ")
                    }
                }
            }
            setOnLongClickListener {
                showRemoveSuggestionDialog(text.toString())
                true
            }
        }
        candidatesLayout.addView(candidateRightTv)
    }

    private fun createToolbarIconButton(iconRes: Int, onClick: () -> Unit): View {
        val chips = currentTheme.stripStyle == StripStyle.CHIPS
        return ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(currentTheme.textColorSecondary)
            val pad = dpToPx(7)
            setPadding(pad, pad, pad, pad)
            val size = dpToPx(32)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dpToPx(3) }
            if (chips) {
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(16).toFloat()
                    setColor(currentTheme.palette.chipBg)
                }
            }
            setOnClickListener {
                performHapticFeedback()
                onClick()
            }
        }
    }

    private var candidateIndex = 0

    private fun createCandidateTextView(): TextView {
        val chips = currentTheme.stripStyle == StripStyle.CHIPS
        val index = candidateIndex++
        return TextView(context).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            includeFontPadding = false
            setTextColor(currentTheme.suggestionTextColor)
            // The centre (primary / autocorrect) candidate is slightly stronger.
            typeface = if (index % 3 == 1) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.create("sans-serif", Typeface.NORMAL)
            val padH = dpToPx(8)
            setPadding(padH, dpToPx(4), padH, dpToPx(4))
            background = if (chips) {
                GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(currentTheme.palette.chipBg)
                }
            } else if (index % 3 != 0) {
                StripDividerDrawable(ThemeColor.withAlpha(currentTheme.suggestionTextColor, 0.2f), dpToPx(1).toFloat(), dpToPx(18).toFloat())
            } else null
            layoutParams = LayoutParams(0, dpToPx(34), 1.0f).apply {
                marginStart = if (chips) dpToPx(3) else 0
                marginEnd = if (chips) dpToPx(3) else 0
            }
        }
    }

    private fun showRemoveSuggestionDialog(word: String) {
        if (word.isNotEmpty()) {
            predictionEngine.deleteWordFromSuggestions(word)
            actionListener?.onDeleteSuggestedWord(word)
            performHapticFeedback()
            Toast.makeText(context, "Removed \"$word\" from suggestions 🗑️", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Hindi (and other Devanagari layouts) swap to a different key set on Shift – vowels and
     * matras instead of consonants – so relabelling is not enough there. Latin layouts keep the
     * fast in-place path that only swaps labels and the Shift key's own look.
     */
    private fun languageUsesShiftLayout(): Boolean =
        KeyboardLanguage.fromId(preferences.currentLanguage) != KeyboardLanguage.ENGLISH

    private fun layoutDependsOnShift(): Boolean =
        keyboardMode == KeyboardMode.ALPHA && languageUsesShiftLayout()

    /** Call after any change to [shiftState] so the keys match it. */
    private fun applyShiftStateChange() {
        if (layoutDependsOnShift()) renderKeyboardLayout() else updateKeyLabelsForShift()
    }

    private fun updateKeyLabelsForShift() {
        val isShifted = shiftState != ShiftState.UNSHIFTED
        letterKeyViews.forEach { (tv, key) ->
            tv.text = if (isShifted) key.shiftText else key.primaryText
        }
        // In-place update: swap pre-resolved specs, no drawable or view allocation.
        val activeSpec = shiftSpecActive
        val idleSpec = shiftSpecIdle
        shiftKeyDrawable?.let { d ->
            if (activeSpec != null && idleSpec != null) d.spec = if (isShifted) activeSpec else idleSpec
            d.indicatorVisible = shiftState == ShiftState.CAPS_LOCKED
        }
        shiftKeyIcon?.let { icon ->
            icon.setImageResource(shiftIconFor(shiftState))
            val spec = if (isShifted) activeSpec else idleSpec
            icon.setColorFilter(spec?.textColor ?: currentTheme.textColorPrimary)
            icon.contentDescription = when (shiftState) {
                ShiftState.UNSHIFTED -> "Shift"
                ShiftState.SHIFTED_ONCE -> "Shift on"
                ShiftState.CAPS_LOCKED -> "Caps lock on"
            }
        }
    }

    /** Outline = off, filled = shift once, arrow-with-bar + LED dot = caps lock. */
    private fun shiftIconFor(state: ShiftState): Int = when (state) {
        ShiftState.UNSHIFTED -> R.drawable.ic_shift
        ShiftState.SHIFTED_ONCE -> R.drawable.ic_shift_filled
        ShiftState.CAPS_LOCKED -> R.drawable.ic_capslock
    }

    // ---------------------------------------------------------------------------------------------
    // Keyboard Layout Rendering
    // ---------------------------------------------------------------------------------------------
    private fun renderKeyboardLayout() {
        rowsLayout.removeAllViews()
        letterKeyViews.clear()
        shiftKeyIcon = null
        shiftKeyDrawable = null
        shiftSpecIdle = null
        shiftSpecActive = null

        val currentLang = KeyboardLanguage.fromId(preferences.currentLanguage)
        renderedLanguageId = currentLang.id
        val rows = when (keyboardMode) {
            KeyboardMode.ALPHA -> KeyLayoutHelper.getAlphaRows(preferences.isNumberRowEnabled, currentLang, shiftState)
            KeyboardMode.SYMBOLS_1 -> KeyLayoutHelper.getSymbols1Rows(preferences.isNumberRowEnabled, currentLang)
            KeyboardMode.SYMBOLS_2 -> KeyLayoutHelper.getSymbols2Rows(preferences.isNumberRowEnabled, currentLang)
            KeyboardMode.DIALPAD -> KeyLayoutHelper.getDialpadRows()
            KeyboardMode.EMOJI -> emptyList()
        }

        val rowMarginB = (3.0f * context.resources.displayMetrics.density).toInt()
        val defaultBottomPad = getCalculatedBottomPadding()
        val targetContentHeight = getStandardContentHeight()

        rowsLayout.setPadding(dpToPx(2), dpToPx(3), dpToPx(2), defaultBottomPad)

        if (rows.isNotEmpty()) {
            val hasNumberRow = preferences.isNumberRowEnabled && (keyboardMode == KeyboardMode.ALPHA || keyboardMode == KeyboardMode.SYMBOLS_1 || keyboardMode == KeyboardMode.SYMBOLS_2)
            val totalMargins = (rows.size - 1) * rowMarginB

            // The number row is a little shorter than the letter rows but scales with them, so
            // digits stay properly sized on tablets and at larger keyboard heights. Heights are
            // derived from the height budget, so the keyboard never grows past it (which would
            // make the window jump when switching to the emoji or clipboard panel).
            val usableH = targetContentHeight - totalMargins
            val withNumberRow = hasNumberRow && rows.size > 1
            val rowUnits = if (withNumberRow) (rows.size - 1) + NUMBER_ROW_RATIO else rows.size.toFloat()
            val letterRowH = maxOf(dpToPx(MIN_ROW_DP), (usableH / rowUnits).toInt())
            val numberRowH = if (withNumberRow) {
                (letterRowH * NUMBER_ROW_RATIO).toInt().coerceIn(minOf(dpToPx(MIN_ROW_DP), letterRowH), letterRowH)
            } else 0

            rows.forEachIndexed { rowIndex, keyRow ->
                val isNumRow = hasNumberRow && rowIndex == 0
                val currentRowHeight = if (isNumRow) numberRowH else letterRowH

                val totalWeight = keyRow.sumOf { it.weight.toDouble() }.toFloat()
                val rowLayout = LinearLayout(context).apply {
                    isMotionEventSplittingEnabled = true
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER
                    weightSum = totalWeight
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, currentRowHeight)
                }

                var weightBefore = 0f
                var charIndex = 0
                keyRow.forEachIndexed { colIndex, keyModel ->
                    val xFraction = if (totalWeight > 0f) (weightBefore + keyModel.weight / 2f) / totalWeight else 0.5f
                    val delegateKey = if (keyModel.type == KeyType.SPACER) {
                        if (colIndex == 0) keyRow.getOrNull(1) else keyRow.getOrNull(colIndex - 1)
                    } else null
                    val keyView = createKeyView(keyModel, rowIndex, rows.size, charIndex, xFraction, currentRowHeight, delegateKey)
                    if (keyModel.type == KeyType.CHARACTER) charIndex++
                    weightBefore += keyModel.weight
                    rowLayout.addView(keyView)
                }

                rowsLayout.addView(rowLayout)
            }

            renderedRowsHeight = (if (hasNumberRow) numberRowH else 0) +
                letterRowH * (if (hasNumberRow) rows.size - 1 else rows.size) + totalMargins
        }
    }

    private fun createKeyView(key: KeyModel, row: Int, rowCount: Int, charIndex: Int, xFraction: Float, rowHeightPx: Int, delegateKey: KeyModel? = null): View {
        if (key.type == KeyType.SPACER) {
            val spacerView = View(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight)
            }
            if (delegateKey != null) {
                attachTouchListener(spacerView, delegateKey)
            }
            return spacerView
        }

        val density = context.resources.displayMetrics.density
        val rowH = rowHeightPx.toFloat()
        val userScale = preferences.labelScale
        val role = KeyColorResolver.roleOf(key)
        val wide = KeyColorResolver.isWide(key)
        val baseColor = KeyColorResolver.baseColor(currentTheme, key, role, row, rowCount, charIndex, xFraction)
        val edgeTint = KeyColorResolver.edgeTint(currentTheme, xFraction)

        val shiftActive = key.type == KeyType.SHIFT && shiftState != ShiftState.UNSHIFTED
        val idleSpec = keycapResolver.resolve(role, baseColor, wide, false, edgeTint)
        val spec = if (key.type == KeyType.SHIFT) {
            val activeSpec = keycapResolver.resolve(role, currentTheme.palette.keyAction, wide, true, 0)
            shiftSpecIdle = idleSpec
            shiftSpecActive = activeSpec
            if (shiftActive) activeSpec else idleSpec
        } else idleSpec

        val keycap = KeycapDrawable(spec, keyMotion)
        val keyLayout = KeyView(context, keycap).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight)
        }

        val iconSize = KeyLabelMetrics.iconPx(rowH, density, userScale).toInt()
        fun iconView(res: Int, tint: Int, desc: String) = ImageView(context).apply {
            setImageResource(res)
            setColorFilter(tint)
            contentDescription = desc
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
        }
        fun labelView(text: String, sizePx: Float, color: Int, face: Typeface) = TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(color)
            typeface = face
            maxLines = 1
            if (currentTheme.typography.letterSpacing != 0f) letterSpacing = currentTheme.typography.letterSpacing
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        when (key.type) {
            KeyType.SHIFT -> {
                shiftKeyDrawable = keycap
                keycap.indicatorVisible = shiftState == ShiftState.CAPS_LOCKED
                if (layoutDependsOnShift()) {
                    // Devanagari: the key switches between consonants and vowels, so it says so.
                    val label = if (shiftState == ShiftState.UNSHIFTED) "अ आ" else "क ख"
                    shiftKeyIcon = null
                    keyLayout.addView(
                        labelView(label, KeyLabelMetrics.functionTextPx(rowH, density, label, userScale), spec.textColor, labelTypefaceStrong).apply {
                            contentDescription = if (shiftState == ShiftState.UNSHIFTED) "Switch to vowels" else "Switch to consonants"
                        }
                    )
                } else {
                    val shiftIcon = iconView(shiftIconFor(shiftState), spec.textColor, "Shift")
                    shiftKeyIcon = shiftIcon
                    keyLayout.addView(shiftIcon)
                }
            }
            KeyType.BACKSPACE -> {
                keyLayout.addView(iconView(R.drawable.ic_backspace, spec.textColor, "Delete").apply {
                    // Backspace glyph is visually heavier; trim it so it matches shift.
                    layoutParams = FrameLayout.LayoutParams((iconSize * 0.92f).toInt(), (iconSize * 0.92f).toInt(), Gravity.CENTER)
                })
            }
            KeyType.ENTER -> {
                val (enterIconRes, enterText) = getActionInfo()
                if (enterIconRes != 0) {
                    keyLayout.addView(iconView(enterIconRes, spec.textColor, enterText))
                } else {
                    keyLayout.addView(labelView(enterText, KeyLabelMetrics.functionTextPx(rowH, density, enterText, userScale), spec.textColor, labelTypefaceStrong))
                }
            }
            KeyType.SPACE -> {
                val currentLang = KeyboardLanguage.fromId(preferences.currentLanguage)
                keyLayout.addView(labelView(currentLang.spaceLabel, KeyLabelMetrics.spaceLabelPx(rowH, density, userScale), spec.secondaryTextColor, labelTypeface))
            }
            KeyType.EMOJI -> {
                keyLayout.addView(labelView("😀", KeyLabelMetrics.iconPx(rowH, density, userScale) * 0.85f, spec.textColor, Typeface.DEFAULT).apply {
                    contentDescription = "Emoji"
                })
            }
            KeyType.MODE_CHANGE -> {
                keyLayout.addView(labelView(key.primaryText, KeyLabelMetrics.functionTextPx(rowH, density, key.primaryText, userScale), spec.textColor, labelTypefaceStrong))
            }
            KeyType.LANGUAGE_SWITCH -> {
                keyLayout.addView(iconView(R.drawable.ic_globe, spec.textColor, "Switch language"))
            }
            else -> {
                if (keyboardMode == KeyboardMode.DIALPAD) {
                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    container.addView(labelView(key.primaryText, (rowH * 0.40f * userScale).coerceAtMost(30f * density), spec.textColor, labelTypefaceStrong).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    if (key.altText.isNotEmpty()) {
                        container.addView(labelView(key.altText, KeyLabelMetrics.altHintPx(rowH, density, userScale), spec.secondaryTextColor, labelTypeface).apply {
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        })
                    }
                    keyLayout.addView(container)
                } else {
                    val hasAlt = key.altText.isNotEmpty() && keyboardMode == KeyboardMode.ALPHA
                    val charText = if (shiftState != ShiftState.UNSHIFTED) key.shiftText else key.primaryText
                    val mainTv = labelView(
                        charText,
                        KeyLabelMetrics.letterPx(rowH, density, key.primaryText, hasAlt, currentTheme.typography, userScale),
                        spec.textColor,
                        labelTypeface
                    ).apply {
                        if (hasAlt) {
                            // Nudge the main glyph down a touch so the corner hint has room.
                            setPadding(0, (rowH * 0.08f).toInt(), 0, 0)
                        }
                    }
                    if (key.type == KeyType.CHARACTER) {
                        letterKeyViews.add(Pair(mainTv, key))
                    }
                    keyLayout.addView(mainTv)

                    if (hasAlt) {
                        val altTv = TextView(context).apply {
                            text = key.altText
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, KeyLabelMetrics.altHintPx(rowH, density, userScale))
                            gravity = Gravity.END or Gravity.TOP
                            includeFontPadding = false
                            setTextColor(spec.secondaryTextColor)
                            typeface = labelTypeface
                            val inset = (spec.gapHPx + spec.skirtXPx + 4 * density).toInt()
                            setPadding(0, (spec.gapVPx + spec.skirtTopPx + 3 * density).toInt(), inset, 0)
                            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        }
                        keyLayout.addView(altTv)
                    }
                }
            }
        }

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
        var downY = 0f
        var cursorMoved = false
        var lastCursorMoveX = 0f
        var isLongPressHandled = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    // 1. Commit any previously held key from another finger before processing this one
                    if (key.type != KeyType.SPACE && isSpaceHeld && !isSpaceCommitted && !cursorMoved) {
                        isSpaceCommitted = true
                        actionListener?.onSpace()
                    }
                    if (pendingKeyModel != null && !isPendingKeyCommitted) {
                        commitPendingKeyIfNeeded()
                    } else {
                        cancelPendingLongPress()
                    }

                    downX = event.rawX
                    downY = event.rawY
                    lastCursorMoveX = event.rawX
                    cursorMoved = false
                    isLongPressHandled = false

                    performHapticFeedback()
                    performAudioFeedback()
                    v.drawableHotspotChanged(event.getX(event.actionIndex), event.getY(event.actionIndex))
                    animateKeyPress(v, true)

                    if (key.type == KeyType.CHARACTER || key.type == KeyType.COMMA || key.type == KeyType.PERIOD) {
                        pendingKeyView = v
                        pendingKeyModel = key
                        isPendingKeyCommitted = false
                        isPendingKeyLongPressed = false

                        val text = if (shiftState != ShiftState.UNSHIFTED) key.shiftText else key.primaryText
                        if (preferences.isPopupEnabled) {
                            showKeyPopup(v, text)
                        }

                        // Schedule long press only if key has popups or altText
                        val hasLongPress = key.popupChars.isNotEmpty() || key.altText.isNotEmpty()
                        if (hasLongPress) {
                            activeLongPressKey = key
                            val lpr = Runnable {
                                if (activeLongPressKey == key && pendingKeyModel == key && !isPendingKeyCommitted && !cursorMoved) {
                                    isLongPressHandled = true
                                    isPendingKeyLongPressed = true
                                    dismissPopup()
                                    performHapticFeedback()
                                    performAudioFeedback()
                                    val chars = when {
                                        key.popupChars.isNotEmpty() -> key.popupChars
                                        key.altText.isNotEmpty() -> listOf(key.altText)
                                        else -> emptyList()
                                    }
                                    if (chars.isNotEmpty()) {
                                        showAccentsPopup(view, chars)
                                    }
                                }
                            }
                            activeLongPressRunnable = lpr
                            handler.postDelayed(lpr, 500)
                        }
                    } else if (key.type == KeyType.BACKSPACE) {
                        commitPendingKeyIfNeeded()
                        isBackspaceHeld = true
                        actionListener?.onBackspace()
                        handler.postDelayed(backspaceRepeatRunnable, 350)
                    } else if (key.type == KeyType.SPACE) {
                        isSpaceHeld = true
                        isSpaceCommitted = false
                        spaceLongPressRunnable = Runnable {
                            if (!cursorMoved && !isLongPressHandled) {
                                isLongPressHandled = true
                                performHapticFeedback()
                                showLanguageSelectionDialog()
                            }
                        }
                        handler.postDelayed(spaceLongPressRunnable!!, 500)
                    } else if (key.type == KeyType.SHIFT || key.type == KeyType.MODE_CHANGE || key.type == KeyType.EMOJI || key.type == KeyType.ENTER) {
                        commitPendingKeyIfNeeded()
                        handleKeyClick(key)
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY

                    if (key.type == KeyType.SPACE && abs(dx) > dpToPx(36)) {
                        spaceLongPressRunnable?.let { handler.removeCallbacks(it) }
                        val diff = event.rawX - lastCursorMoveX
                        val step = dpToPx(14)
                        if (abs(diff) >= step) {
                            val offset = if (diff > 0) 1 else -1
                            actionListener?.onMoveCursor(offset)
                            performHapticFeedback()
                            cursorMoved = true
                            lastCursorMoveX = event.rawX
                        }
                    } else if (key.type != KeyType.SPACE && !isLongPressHandled && (abs(dx) > dpToPx(18) || abs(dy) > dpToPx(18))) {
                        cancelPendingLongPress()
                    }

                    if (isLongPressHandled && accentsPopupWindow?.isShowing == true) {
                        handleAccentsMove(event.rawX)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    animateKeyPress(v, false)
                    handler.removeCallbacks(backspaceRepeatRunnable)
                    spaceLongPressRunnable?.let { handler.removeCallbacks(it) }
                    isBackspaceHeld = false
                    dismissPopup()

                    if (key.type == KeyType.CHARACTER || key.type == KeyType.COMMA || key.type == KeyType.PERIOD) {
                        cancelPendingLongPress()
                        if (isLongPressHandled && accentsPopupWindow?.isShowing == true) {
                            if (activeAccentIndex in currentPopupChars.indices) {
                                val accent = currentPopupChars[activeAccentIndex]
                                actionListener?.onTextKey(accent)
                            }
                            dismissAccentsPopup()
                            isPendingKeyCommitted = true
                            pendingKeyModel = null
                            pendingKeyView = null
                        } else if (pendingKeyModel == key && !isPendingKeyCommitted) {
                            isPendingKeyCommitted = true
                            handleKeyClick(key)
                            pendingKeyModel = null
                            pendingKeyView = null
                        }
                    } else if (key.type == KeyType.SPACE) {
                        cancelPendingLongPress()
                        if (!isSpaceCommitted && !cursorMoved && !isLongPressHandled) {
                            isSpaceCommitted = true
                            actionListener?.onSpace()
                        }
                        isSpaceHeld = false
                    }

                    if (pendingShiftRender) {
                        pendingShiftRender = false
                        renderKeyboardLayout()
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    animateKeyPress(v, false)
                    cancelPendingLongPress()
                    handler.removeCallbacks(backspaceRepeatRunnable)
                    spaceLongPressRunnable?.let { handler.removeCallbacks(it) }
                    isBackspaceHeld = false
                    dismissPopup()
                    dismissAccentsPopup()
                    if (key.type == KeyType.SPACE) {
                        isSpaceHeld = false
                    }
                    if (pendingKeyModel == key) {
                        pendingKeyModel = null
                        pendingKeyView = null
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
                    if (layoutDependsOnShift()) {
                        // Rebuilding now would detach the key under the finger (killing its popup
                        // and long-press); do it when the touch ends.
                        pendingShiftRender = true
                    } else {
                        updateKeyLabelsForShift()
                    }
                }
            }
            KeyType.SPACE -> {
                actionListener?.onSpace()
            }
            KeyType.SHIFT -> {
                val now = System.currentTimeMillis()
                if (now - lastShiftPressTime < 300) {
                    shiftState = if (shiftState == ShiftState.CAPS_LOCKED) ShiftState.UNSHIFTED else ShiftState.CAPS_LOCKED
                } else {
                    shiftState = when (shiftState) {
                        ShiftState.UNSHIFTED -> ShiftState.SHIFTED_ONCE
                        ShiftState.SHIFTED_ONCE -> ShiftState.UNSHIFTED
                        ShiftState.CAPS_LOCKED -> ShiftState.UNSHIFTED
                    }
                }
                lastShiftPressTime = now
                applyShiftStateChange()
            }
            KeyType.MODE_CHANGE -> {
                keyboardMode = when (key.primaryText) {
                    "?123" -> KeyboardMode.SYMBOLS_1
                    "=\\<" -> KeyboardMode.SYMBOLS_2
                    "ABC", "अआइ", "हिंदी" -> KeyboardMode.ALPHA
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
            KeyType.LANGUAGE_SWITCH -> {
                toggleLanguage()
            }
            KeyType.SETTINGS -> {
                actionListener?.onOpenSettings()
            }
            KeyType.BACKSPACE, KeyType.SPACER -> {}
        }
    }

    private fun toggleLanguage() {
        val nextLang = if (preferences.currentLanguage == "en") "hi" else "en"
        preferences.currentLanguage = nextLang
        shiftState = ShiftState.UNSHIFTED
        renderKeyboardLayout()
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf(
            "English",
            "हिंदी"
        )
        val langIds = arrayOf("en", "hi")
        val currentIdx = if (preferences.currentLanguage == "hi") 1 else 0

        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Select Keyboard Language")
        builder.setSingleChoiceItems(languages, currentIdx) { dialog, which ->
            preferences.currentLanguage = langIds[which]
            shiftState = ShiftState.UNSHIFTED
            renderKeyboardLayout()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)

        val dialog = builder.create()
        val win = dialog.window
        if (win != null) {
            val lp = win.attributes
            lp.token = windowToken
            lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            win.attributes = lp
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        try {
            dialog.show()
        } catch (_: Exception) {
            toggleLanguage()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Key Animations & Popups
    // ---------------------------------------------------------------------------------------------
    private fun animateKeyPress(view: View, isPressed: Boolean) {
        view.isPressed = isPressed
    }

    private fun popupBackground(): GradientDrawable = GradientDrawable().apply {
        val style = currentTheme.keyStyle
        cornerRadius = dpToPx(if (style.shape == com.example.mykeyboard.theme.KeyShape.NOTCHED) 3 else 12).toFloat()
        setColor(currentTheme.popupBgColor)
        setStroke(dpToPx(1), currentTheme.rippleColor)
    }

    /** Popups are created once; re-skin them when the theme changes instead of rebuilding. */
    private fun refreshPopupStyles() {
        popupTextView?.apply {
            background = popupBackground()
            typeface = labelTypefaceStrong
            setTextColor(currentTheme.popupTextColor)
        }
        accentsContainer?.background = popupBackground()
    }

    private fun initKeyPopup() {
        val popupView = TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 28f
            typeface = labelTypefaceStrong
            includeFontPadding = false
            val pad = dpToPx(8)
            setPadding(pad, pad, pad, pad)
            background = popupBackground()
        }
        popupTextView = popupView
        popupWindow = PopupWindow(popupView, dpToPx(56), dpToPx(64)).apply {
            isTouchable = false
            animationStyle = 0
        }
    }

    private fun showKeyPopup(anchor: View, text: String) {
        popupTextView?.let { tv ->
            tv.text = text
            tv.setTextColor(currentTheme.popupTextColor)

            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val x = location[0] + (anchor.width - dpToPx(56)) / 2
            val y = location[1] - dpToPx(68)

            try {
                if (popupWindow?.isShowing == true) {
                    popupWindow?.update(x, y, dpToPx(56), dpToPx(64))
                } else {
                    popupWindow?.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
                }
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
            background = popupBackground()
        }
        accentsPopupWindow = PopupWindow(accentsContainer, LayoutParams.WRAP_CONTENT, dpToPx(52)).apply {
            isTouchable = false
        }
    }

    private fun showAccentsPopup(anchor: View, chars: List<String>) {
        currentPopupChars = chars
        activeAccentIndex = 0
        accentsContainer?.removeAllViews()

        chars.forEach { char ->
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
        updateAccentsHighlight()

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
    // Emoji & Clipboard View Switching
    // ---------------------------------------------------------------------------------------------
    private fun showEmojiKeyboard() {
        if (emojiSearchActive) endEmojiSearch(reopenEmoji = false)
        keyboardMode = KeyboardMode.EMOJI
        rowsLayout.visibility = View.GONE
        clipboardView?.visibility = View.GONE
        // The emoji panel brings its own category bar; give the strip's height to browsing.
        suggestionContainer.visibility = View.GONE

        val keyboardTotal = getEmojiPanelHeightHint()
        val bottomPad = getCalculatedBottomPadding()

        if (emojiKeyboardView == null) {
            emojiKeyboardView = EmojiKeyboardView(context).apply {
                applyTheme(currentTheme, keycapResolver, keyMotion)
                setEmojiListener(object : EmojiKeyboardView.EmojiListener {
                    override fun onEmojiSelected(emoji: String) {
                        performHapticFeedback()
                        performAudioFeedback()
                        inputListener()?.onTextKey(emoji)
                    }

                    override fun onBackToAlpha() {
                        performHapticFeedback()
                        showAlphaKeyboard()
                    }

                    override fun onBackspace() {
                        performHapticFeedback()
                        performAudioFeedback()
                        inputListener()?.onBackspace()
                    }

                    override fun onSpace() {
                        performHapticFeedback()
                        performAudioFeedback()
                        inputListener()?.onSpace()
                    }

                    override fun onSearchRequested() {
                        performHapticFeedback()
                        startEmojiSearch()
                    }
                })
            }
            keyboardContainer.addView(
                emojiKeyboardView,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )
        }
        emojiKeyboardView?.updateFixedContentHeight(keyboardTotal, bottomPad)
        emojiKeyboardView?.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------------------------------------
    // Offline emoji search: letters typed on the normal keyboard go into a query shown in the
    // strip instead of the app; results appear as a scrollable row.
    // ---------------------------------------------------------------------------------------------
    private var emojiSearchActive = false
    private val emojiSearchQuery = StringBuilder()
    private var searchListenerBackup: KeyboardActionListener? = null
    private var searchStrip: LinearLayout? = null
    private var searchQueryTv: TextView? = null
    private var searchResultsRow: LinearLayout? = null
    private var searchResultsScroll: android.widget.HorizontalScrollView? = null

    /** The listener that talks to the app, even while search intercepts key input. */
    private fun inputListener(): KeyboardActionListener? = if (emojiSearchActive) searchListenerBackup else actionListener

    private val searchInterceptor = object : KeyboardActionListener {
        override fun onTextKey(text: String) { emojiSearchQuery.append(text); updateEmojiSearch() }
        override fun onBackspace() {
            if (emojiSearchQuery.isEmpty()) { endEmojiSearch(reopenEmoji = true); return }
            val cp = emojiSearchQuery.codePointBefore(emojiSearchQuery.length)
            emojiSearchQuery.setLength(emojiSearchQuery.length - Character.charCount(cp))
            updateEmojiSearch()
        }
        override fun onSpace() { if (emojiSearchQuery.isNotEmpty() && emojiSearchQuery.last() != ' ') { emojiSearchQuery.append(' '); updateEmojiSearch() } }
        override fun onEnter(actionId: Int) {
            val first = com.example.mykeyboard.model.EmojiData.search(emojiSearchQuery.toString(), 1).firstOrNull()
            if (first != null) commitSearchEmoji(first) else endEmojiSearch(reopenEmoji = true)
        }
        override fun onOpenEmoji() { endEmojiSearch(reopenEmoji = true) }
        override fun onOpenSettings() { searchListenerBackup?.onOpenSettings() }
        override fun onMoveCursor(offset: Int) {}
        override fun onPasteClipboard(text: String) {}
        override fun onUndo() {}
        override fun onRedo() {}
        override fun onAddWordToDictionary(word: String) {}
        override fun onDeleteSuggestedWord(word: String) {}
        override fun onReplaceText(oldText: String, newText: String) {}
    }

    private fun startEmojiSearch() {
        if (emojiSearchActive) return
        searchListenerBackup = actionListener
        actionListener = searchInterceptor
        emojiSearchActive = true
        emojiSearchQuery.setLength(0)
        buildSearchStrip()
        toolbarActionsLayout.visibility = View.GONE
        candidatesLayout.visibility = View.GONE
        searchStrip?.visibility = View.VISIBLE
        showAlphaKeyboard()
        com.example.mykeyboard.model.EmojiData.ensureLoaded(context) { updateEmojiSearch() }
        updateEmojiSearch()
    }

    private fun endEmojiSearch(reopenEmoji: Boolean) {
        if (!emojiSearchActive) return
        emojiSearchActive = false
        // Stop a held backspace so its repeat can't continue into the app's text.
        isBackspaceHeld = false
        handler.removeCallbacks(backspaceRepeatRunnable)
        actionListener = searchListenerBackup
        searchListenerBackup = null
        searchStrip?.visibility = View.GONE
        toolbarActionsLayout.visibility = View.VISIBLE
        candidatesLayout.visibility = View.VISIBLE
        if (reopenEmoji) showEmojiKeyboard()
    }

    private fun commitSearchEmoji(emoji: String) {
        performHapticFeedback()
        performAudioFeedback()
        searchListenerBackup?.onTextKey(emoji)
        KeyboardPreferences(context).recordRecentEmoji(emoji)
    }

    private fun buildSearchStrip() {
        searchStrip?.let { suggestionContainer.removeView(it) }
        val pal = currentTheme.palette
        val strip = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }
        strip.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_clear)
            setColorFilter(pal.suggestionText)
            contentDescription = "Close emoji search"
            val p = dpToPx(7)
            setPadding(p, p, p, p)
            layoutParams = LayoutParams(dpToPx(32), dpToPx(32)).apply { marginEnd = dpToPx(4) }
            setOnClickListener { performHapticFeedback(); endEmojiSearch(reopenEmoji = true) }
        })
        searchQueryTv = TextView(context).apply {
            textSize = 14f
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            minWidth = dpToPx(96)
            maxWidth = (resources.displayMetrics.widthPixels * 0.38f).toInt()
            ellipsize = android.text.TextUtils.TruncateAt.START
            background = GradientDrawable().apply { cornerRadius = dpToPx(16).toFloat(); setColor(pal.chipBg) }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dpToPx(32)).apply { marginEnd = dpToPx(4) }
        }
        strip.addView(searchQueryTv)
        searchResultsRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        searchResultsScroll = android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            addView(searchResultsRow, android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT))
        }
        strip.addView(searchResultsScroll)
        searchStrip = strip
        suggestionContainer.addView(strip)
    }

    private fun updateEmojiSearch() {
        val pal = currentTheme.palette
        val query = emojiSearchQuery.toString()
        searchQueryTv?.apply {
            if (query.isEmpty()) {
                text = "🔍  Search emoji"
                setTextColor(ThemeColor.withAlpha(pal.suggestionText, 0.6f))
            } else {
                text = "🔍  $query"
                setTextColor(pal.suggestionText)
            }
        }
        val row = searchResultsRow ?: return
        row.removeAllViews()
        val results = if (query.isBlank()) KeyboardPreferences(context).recentEmojis.take(24)
        else com.example.mykeyboard.model.EmojiData.search(query, 48)
        if (results.isEmpty() && query.isNotBlank()) {
            row.addView(TextView(context).apply {
                text = if (com.example.mykeyboard.model.EmojiData.isLoaded) "No matches" else "Loading…"
                textSize = 13f
                setTextColor(ThemeColor.withAlpha(pal.suggestionText, 0.6f))
                setPadding(dpToPx(8), 0, dpToPx(8), 0)
            })
        }
        val cell = dpToPx(40)
        results.forEach { e ->
            row.addView(TextView(context).apply {
                text = e
                gravity = Gravity.CENTER
                textSize = 22f
                includeFontPadding = false
                layoutParams = LayoutParams(cell, cell)
                setOnClickListener { commitSearchEmoji(e) }
            })
        }
        searchResultsScroll?.scrollTo(0, 0)
    }

    private fun showClipboardView() {
        if (emojiSearchActive) endEmojiSearch(reopenEmoji = false)
        suggestionContainer.visibility = View.VISIBLE
        rowsLayout.visibility = View.GONE
        emojiKeyboardView?.visibility = View.GONE

        val targetContentH = getPanelContentHeight()

        if (clipboardView == null) {
            clipboardView = ClipboardView(context).apply {
                applyTheme(currentTheme)
                setClipboardListener(object : ClipboardView.ClipboardListener {
                    override fun onClipSelected(text: String) {
                        performHapticFeedback()
                        actionListener?.onPasteClipboard(text)
                        showAlphaKeyboard()
                    }

                    override fun onCloseClipboard() {
                        performHapticFeedback()
                        showAlphaKeyboard()
                    }
                })
            }
            keyboardContainer.addView(
                clipboardView,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )
            clipboardView?.updateFixedContentHeight(targetContentH, getCalculatedBottomPadding())
        } else {
            clipboardView?.updateFixedContentHeight(targetContentH, getCalculatedBottomPadding())
            clipboardView?.refreshClips()
            clipboardView?.visibility = View.VISIBLE
        }
    }

    private fun showAlphaKeyboard() {
        suggestionContainer.visibility = View.VISIBLE
        keyboardMode = KeyboardMode.ALPHA
        emojiKeyboardView?.visibility = View.GONE
        clipboardView?.visibility = View.GONE
        rowsLayout.visibility = View.VISIBLE
        renderKeyboardLayout()
    }

    private fun showDialpadKeyboard() {
        if (emojiSearchActive) endEmojiSearch(reopenEmoji = false)
        suggestionContainer.visibility = View.VISIBLE
        keyboardMode = KeyboardMode.DIALPAD
        emojiKeyboardView?.visibility = View.GONE
        clipboardView?.visibility = View.GONE
        rowsLayout.visibility = View.VISIBLE
        renderKeyboardLayout()
    }

    // ---------------------------------------------------------------------------------------------
    // Haptic & Sound Feedback
    // ---------------------------------------------------------------------------------------------
    private val hapticExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private fun performHapticFeedback() {
        if (!preferences.isHapticEnabled) return
        hapticExecutor.execute {
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
    }

    private fun performAudioFeedback() {
        if (!preferences.isSoundEnabled) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
        } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------------------------------------
    // System bar (navigation) spacing
    //
    // The IME window is laid out edge-to-edge by MyKeyboardService, so it may extend underneath the
    // navigation bar. We reserve exactly the space the system reports – full height for 3-button
    // bars, the handle height in gesture mode, 0 when the window already sits above the bar – and
    // the keyboard background paints through that area, so there is never an empty black strip.
    // ---------------------------------------------------------------------------------------------

    private fun readSystemInsets(insets: androidx.core.view.WindowInsetsCompat) {
        val nav = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        // tappableElement is 0 where only a gesture handle overlays us, and equals the bar height
        // where real buttons sit, so the larger of the two is what must stay free of keys.
        val tappable = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.tappableElement())
        // Tablets/foldables report the taskbar here (up to ~90dp), so the clamp only guards
        // against absurd values.
        val limit = dpToPx(120)
        val bottom = maxOf(nav.bottom, tappable.bottom).coerceIn(0, limit)
        val left = maxOf(nav.left, tappable.left).coerceIn(0, limit)
        val right = maxOf(nav.right, tappable.right).coerceIn(0, limit)
        if (bottom != systemBottomInset || left != systemLeftInset || right != systemRightInset) {
            systemBottomInset = bottom
            systemLeftInset = left
            systemRightInset = right
            applyEdgeSpacing()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Some OEM ROMs skip the first dispatch for IME windows; read what the window already knows.
        androidx.core.view.ViewCompat.getRootWindowInsets(this)?.let { readSystemInsets(it) }
        androidx.core.view.ViewCompat.requestApplyInsets(this)
    }

    /** Re-reads the navigation bar state (called when the keyboard is shown again). */
    fun refreshWindowInsets() {
        androidx.core.view.ViewCompat.getRootWindowInsets(this)?.let { readSystemInsets(it) }
        androidx.core.view.ViewCompat.requestApplyInsets(this)
    }

    /** Applies the reserved space to every panel that reaches the bottom of the window. */
    private fun applyEdgeSpacing() {
        val bottomPad = getCalculatedBottomPadding()
        // Side bars (landscape 3-button) are handled once, for every panel; the background still
        // paints full-bleed because padding only insets children.
        setPadding(systemLeftInset, 0, systemRightInset, 0)
        rowsLayout.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), bottomPad)
        // Insets can arrive mid-layout; resize the panels on the next frame so the emoji grid is
        // never told to rebind while RecyclerView is laying out. Only ever one pending pass.
        panelResizeRunnable?.let { removeCallbacks(it) }
        val resize = Runnable {
            panelResizeRunnable = null
            emojiKeyboardView?.updateFixedContentHeight(getEmojiPanelHeightHint(), bottomPad)
            clipboardView?.updateFixedContentHeight(getPanelContentHeight(), bottomPad)
        }
        panelResizeRunnable = resize
        post(resize)
        requestLayout()
    }

    private fun getCalculatedBottomPadding(): Int =
        if (systemBottomInset > 0) systemBottomInset + dpToPx(2) else dpToPx(6)


    /** Height the emoji / clipboard panels should match: what the letter keyboard really uses. */
    private fun getPanelContentHeight(): Int = maxOf(getStandardContentHeight(), renderedRowsHeight)

    private fun getEmojiPanelHeightHint(): Int = getPanelContentHeight() + dpToPx(42) + dpToPx(3)

    private fun getStandardContentHeight(): Int {
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val numRows = if (preferences.isNumberRowEnabled) 5 else 4
        val baseRowHeight = if (isTablet) dpToPx(56) else if (isLandscape) dpToPx(42) else if (numRows == 5) dpToPx(44) else dpToPx(52)
        val scaledRowHeight = (baseRowHeight * preferences.heightScale).toInt()
        val rowMarginB = (3.0f * context.resources.displayMetrics.density).toInt()
        return (numRows * scaledRowHeight) + ((numRows - 1) * rowMarginB)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private companion object {
        /** Number row height as a fraction of a letter row. */
        const val NUMBER_ROW_RATIO = 0.82f

        /** Absolute floor for a key row, so compact + landscape stays tappable. */
        const val MIN_ROW_DP = 34
    }
}
