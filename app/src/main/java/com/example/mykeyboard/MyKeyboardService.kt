package com.example.mykeyboard

import android.graphics.Color
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.mykeyboard.engine.AutoCorrectEngine
import com.example.mykeyboard.engine.ClipboardHistoryManager
import com.example.mykeyboard.engine.PredictionEngine
import com.example.mykeyboard.engine.UndoRedoManager
import android.widget.Toast
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.model.ShiftState
import com.example.mykeyboard.utils.KeyboardPreferences
import com.example.mykeyboard.view.CustomKeyboardView

class MyKeyboardService : InputMethodService(),
    CustomKeyboardView.KeyboardActionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var undoRedoManager: UndoRedoManager
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager

    private var keyboardView: CustomKeyboardView? = null
    private var lastSpaceTime: Long = 0L
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        preferences.registerListener(this)
        predictionEngine = PredictionEngine(this)
        undoRedoManager = UndoRedoManager()
        clipboardHistoryManager = ClipboardHistoryManager(this)

        setupClipboardListener()
    }

    override fun onDestroy() {
        preferences.unregisterListener(this)
        removeClipboardListener()
        super.onDestroy()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onCreateInputView(): View {
        val view = CustomKeyboardView(this).apply {
            setActionListener(this@MyKeyboardService)
            applyTheme(preferences.theme)
        }
        keyboardView = view
        updateNavigationBarAppearance()
        return view
    }

    override fun onWindowShown() {
        super.onWindowShown()
        updateNavigationBarAppearance()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.resetToAlpha()
        undoRedoManager.clear()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateNavigationBarAppearance()
        if (info != null) {
            keyboardView?.setImeOptions(info.imeOptions, info.actionLabel)
        }
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    // ---------------------------------------------------------------------------------------------
    // Keyboard Action Listeners
    // ---------------------------------------------------------------------------------------------
    private fun isEmojiOrSymbol(s: String): Boolean {
        return s.any { Character.isSurrogate(it) || it.code in 0x2000..0x3300 || it.code in 0x1F000..0x1FAFF }
    }

    private val uiHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val predictionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val predictionSequence = java.util.concurrent.atomic.AtomicInteger(0)
    private var pendingPredictionRunnable: Runnable? = null
    private var latestSuggestionResult: com.example.mykeyboard.engine.SuggestionResult? = null
    private var latestPrefix: String = ""
    private var lastAutocorrectReplaced: Pair<String, String>? = null

    override fun onTextKey(text: String) {
        val ic = currentInputConnection ?: return
        lastAutocorrectReplaced = null

        // Fast Path: Direct key from keyboard layout (does not end with space)
        if (!text.endsWith(" ")) {
            ic.commitText(text, 1)
            checkAutoCaps()

            pendingPredictionRunnable?.let { uiHandler.removeCallbacks(it) }
            pendingPredictionRunnable = Runnable {
                updatePredictions()
            }
            uiHandler.postDelayed(pendingPredictionRunnable!!, 20)
            return
        }

        // Suggestion Chip Commit Path
        recordCurrentSnapshot()

        val isEmoji = isEmojiOrSymbol(text)
        val trimmed = text.trim()
        if (trimmed.isNotEmpty() && !trimmed.contains(" ") && !isEmoji) {
            predictionExecutor.execute {
                predictionEngine.learnWord(trimmed)
            }
        }

        val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val lastWord = if (textBefore.endsWith(" ")) "" else (textBefore.split(Regex("[^\\p{L}\\p{N}']")).lastOrNull() ?: "")

        if (!isEmoji && lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }

        ic.commitText(text, 1)

        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    private data class EmojiSequenceInfo(val charLength: Int, val codePointCount: Int)

    private fun isEmojiCodePoint(cp: Int): Boolean {
        return (cp in 0x1F000..0x1FAFF) ||
               (cp in 0x2600..0x27BF) ||
               (cp in 0x2300..0x2BFF) ||
               (cp in 0x1F300..0x1F9FF) ||
               (cp in 0x2000..0x32FF) ||
               (cp in 0xE0000..0xE007F) ||
               cp == 0x200D ||
               cp == 0xFE0F ||
               cp == 0xFE0E ||
               cp == 0x2640 ||
               cp == 0x2642
    }

    private fun isRegionalIndicator(cp: Int): Boolean {
        return cp in 0x1F1E6..0x1F1FF
    }

    private fun getTrailingEmojiSequenceInfo(text: String): EmojiSequenceInfo? {
        if (text.isEmpty()) return null

        val endIndex = text.length
        var currIndex = text.length

        // Check if trailing character is a Regional Indicator (Country Flag part)
        val lastCp = text.codePointBefore(currIndex)
        if (isRegionalIndicator(lastCp)) {
            currIndex -= Character.charCount(lastCp)
            if (currIndex > 0) {
                val prevCp = text.codePointBefore(currIndex)
                if (isRegionalIndicator(prevCp)) {
                    currIndex -= Character.charCount(prevCp)
                }
            }
            val matchedStr = text.substring(currIndex, endIndex)
            val charLength = matchedStr.length
            val codePointCount = matchedStr.codePointCount(0, charLength)
            return EmojiSequenceInfo(charLength, codePointCount)
        }

        var expectZwjOrStop = false
        while (currIndex > 0) {
            val cp = text.codePointBefore(currIndex)
            val step = Character.charCount(cp)

            if (expectZwjOrStop) {
                if (cp == 0x200D) {
                    currIndex -= step
                    expectZwjOrStop = false
                } else if (cp == 0xFE0F || cp == 0xFE0E || (cp in 0x1F3FB..0x1F3FF)) {
                    currIndex -= step
                } else {
                    break
                }
            } else {
                if (isEmojiCodePoint(cp)) {
                    currIndex -= step
                    if (cp != 0x200D && cp != 0xFE0F && cp != 0xFE0E && (cp !in 0x1F3FB..0x1F3FF)) {
                        expectZwjOrStop = true
                    }
                } else {
                    break
                }
            }
        }

        if (currIndex == endIndex) return null

        val matchedStr = text.substring(currIndex, endIndex)
        val charLength = matchedStr.length
        val codePointCount = matchedStr.codePointCount(0, charLength)
        return EmojiSequenceInfo(charLength, codePointCount)
    }

    private fun deleteEmojiSequence(ic: InputConnection): Boolean {
        val textBefore = ic.getTextBeforeCursor(24, 0)?.toString() ?: ""
        if (textBefore.isEmpty()) return false

        val info = getTrailingEmojiSequenceInfo(textBefore)
        if (info != null && info.codePointCount > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ic.deleteSurroundingTextInCodePoints(info.codePointCount, 0)
            } else {
                ic.deleteSurroundingText(info.charLength, 0)
            }
            return true
        }
        return false
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        // 1. Undo autocorrect if backspace is pressed immediately after autocorrect on space
        if (lastAutocorrectReplaced != null) {
            val (originalTypo, correctedWord) = lastAutocorrectReplaced!!
            val textBefore = ic.getTextBeforeCursor(correctedWord.length + 2, 0)?.toString() ?: ""
            if (textBefore.endsWith("$correctedWord ")) {
                ic.deleteSurroundingText(correctedWord.length + 1, 0)
                ic.commitText(originalTypo, 1)
                lastAutocorrectReplaced = null
                checkAutoCaps()
                updatePredictions()
                recordCurrentSnapshot()
                return
            }
        }
        lastAutocorrectReplaced = null

        val selectedText = ic.getSelectedText(0)
        if (!TextUtils.isEmpty(selectedText)) {
            ic.commitText("", 1)
        } else {
            val handled = deleteEmojiSequence(ic)
            if (!handled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ic.deleteSurroundingTextInCodePoints(1, 0)
                } else {
                    val textBefore = ic.getTextBeforeCursor(2, 0)
                    if (!TextUtils.isEmpty(textBefore) && Character.isSurrogate(textBefore!!.last())) {
                        ic.deleteSurroundingText(2, 0)
                    } else {
                        ic.deleteSurroundingText(1, 0)
                    }
                }
            }
        }
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""

        // 1. Double space for ". " shortcut
        if (now - lastSpaceTime < 450) {
            if (textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTime = 0L
                lastAutocorrectReplaced = null
                checkAutoCaps()
                updatePredictions()
                return
            }
        }

        // 2. Autocorrect on space if enabled and valid autocorrect suggestion exists
        val allWords = textBefore.trim().split(Regex("[^\\p{L}\\p{N}']")).filter { it.isNotEmpty() }
        val lastWord = if (textBefore.endsWith(" ")) "" else (allWords.lastOrNull() ?: "")
        val autoCorrectWord = latestSuggestionResult?.takeIf { it.isAutoCorrect }?.center
        if (preferences.autoCorrectMode != com.example.mykeyboard.engine.AutoCorrectMode.OFF &&
            autoCorrectWord != null &&
            lastWord.isNotEmpty() &&
            (lastWord.equals(latestPrefix, ignoreCase = true) || lastWord.length >= 2)
        ) {
            ic.deleteSurroundingText(lastWord.length, 0)
            ic.commitText("$autoCorrectWord ", 1)
            lastAutocorrectReplaced = Pair(lastWord, autoCorrectWord)
            lastSpaceTime = now

            predictionExecutor.execute {
                predictionEngine.learnWord(autoCorrectWord)
            }

            checkAutoCaps()
            updatePredictions()
            return
        }

        // 3. Commit space instantly
        ic.commitText(" ", 1)
        lastAutocorrectReplaced = null
        lastSpaceTime = now

        // 4. Learn typed word in background thread so typing is never blocked
        if (lastWord.isNotEmpty()) {
            predictionExecutor.execute {
                predictionEngine.learnWord(lastWord)
            }
        }

        checkAutoCaps()
        updatePredictions()
    }

    override fun onEnter(actionId: Int) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun onOpenEmoji() {}

    override fun onOpenSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(intent)
    }

    override fun onMoveCursor(offset: Int) {
        val keyCode = if (offset < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val count = kotlin.math.abs(offset)
        for (i in 0 until count) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    override fun onPasteClipboard(text: String) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()
        ic.commitText(text, 1)
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onUndo() {
        val ic = currentInputConnection ?: return
        undoRedoManager.undo(ic)
        checkAutoCaps()
        updatePredictions()
    }

    override fun onRedo() {
        val ic = currentInputConnection ?: return
        undoRedoManager.redo(ic)
        checkAutoCaps()
        updatePredictions()
    }

    override fun onAddWordToDictionary(word: String) {
        predictionEngine.addCustomWord(word)
        updatePredictions()
    }

    override fun onDeleteSuggestedWord(word: String) {
        predictionEngine.deleteWordFromSuggestions(word)
        updatePredictions()
    }

    override fun onReplaceText(oldText: String, newText: String) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()
        ic.deleteSurroundingText(oldText.length, 0)
        val formattedNewText = if (newText.endsWith(" ")) newText else "$newText "
        ic.commitText(formattedNewText, 1)
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    // ---------------------------------------------------------------------------------------------
    // Undo / Redo & Prediction Helpers
    // ---------------------------------------------------------------------------------------------
    private fun recordCurrentSnapshot() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val textAfter = ic.getTextAfterCursor(1000, 0)?.toString() ?: ""
        val fullText = textBefore + textAfter
        undoRedoManager.recordState(fullText, textBefore.length)
    }

    private fun checkAutoCaps() {
        if (!preferences.isAutoCapsEnabled) return

        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(3, 0)?.toString() ?: ""

        val shouldCap = when {
            textBefore.isEmpty() -> true
            textBefore.endsWith("\n") -> true
            textBefore.endsWith(". ") -> true
            textBefore.endsWith("? ") -> true
            textBefore.endsWith("! ") -> true
            else -> false
        }

        if (shouldCap) {
            keyboardView?.setShiftState(ShiftState.SHIFTED_ONCE)
        }
    }

    private fun updatePredictions() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""

        val isAfterSpace = textBefore.endsWith(" ")
        val allWords = textBefore.trim().split(Regex("[^\\p{L}\\p{N}']")).filter { it.isNotEmpty() }

        val prefix = if (isAfterSpace) "" else (allWords.lastOrNull() ?: "")
        val prevWords = if (isAfterSpace) allWords else allWords.dropLast(1)
        val lang = preferences.currentLanguage
        val autoCorrectMode = preferences.autoCorrectMode

        val seq = predictionSequence.incrementAndGet()
        predictionExecutor.execute {
            if (seq != predictionSequence.get()) return@execute
            val result = predictionEngine.getSuggestions(prefix, prevWords, autoCorrectMode, lang)
            if (seq != predictionSequence.get()) return@execute
            uiHandler.post {
                if (seq == predictionSequence.get()) {
                    latestSuggestionResult = result
                    latestPrefix = prefix
                    keyboardView?.setSuggestionsResult(result, prefix)
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Clipboard Listener
    // ---------------------------------------------------------------------------------------------
    private fun setupClipboardListener() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                if (preferences.isClipboardHistoryEnabled) {
                    val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrEmpty()) {
                        clipboardHistoryManager.addClip(clip)
                        keyboardView?.refreshClipboard()
                    }
                }
            }
            cm?.addPrimaryClipChangedListener(clipboardListener)
        } catch (_: Exception) {}
    }

    private fun removeClipboardListener() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------------------------------------
    // Preference Changes
    // ---------------------------------------------------------------------------------------------
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // key == null: preferences were cleared (Reset all settings) on API 30+.
        if (key == null || key in KeyboardPreferences.APPEARANCE_KEYS) {
            keyboardView?.applyTheme(preferences.theme)
            updateNavigationBarAppearance()
        } else if (key == KeyboardPreferences.KEY_LANGUAGE) {
            // Changed from the settings app; the keyboard's own toggle already re-rendered.
            keyboardView?.refreshLanguageIfChanged()
        }
    }

    @Suppress("DEPRECATION")
    private fun updateNavigationBarAppearance() {
        try {
            val win = window?.window ?: return
            val theme = preferences.theme
            val isCustomBg = !preferences.customBgPath.isNullOrEmpty() && java.io.File(preferences.customBgPath!!).exists()
            val chassis = if (isCustomBg) {
                Color.parseColor("#0F172A")
            } else {
                androidx.core.graphics.ColorUtils.setAlphaComponent(theme.background.endColor, 255)
            }
            val isLight = if (isCustomBg) false else (ColorUtils.calculateLuminance(chassis) > 0.5)

            // Lay the keyboard window out edge-to-edge so the system always reports the real
            // navigation bar insets to CustomKeyboardView, on every OEM and both navigation modes.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.setDecorFitsSystemWindows(win, false)
            } else {
                // Only the two flags that matter for a bottom, wrap-content window; the compat
                // helper would also add LAYOUT_FULLSCREEN, which some older OEM ROMs mishandle.
                @Suppress("DEPRECATION")
                win.decorView.systemUiVisibility = win.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
            win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            // Transparent system bar: the keyboard's own background paints behind the navigation
            // bar, and the keys are kept clear of it by the inset padding. The decor keeps the
            // chassis colour so nothing shows through while the window animates in or resizes.
            win.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }

            val decor = win.decorView
            decor.setBackgroundColor(chassis)

            val controller = WindowInsetsControllerCompat(win, decor)
            controller.isAppearanceLightNavigationBars = isLight

            keyboardView?.refreshWindowInsets()
        } catch (_: Exception) {}
    }
}