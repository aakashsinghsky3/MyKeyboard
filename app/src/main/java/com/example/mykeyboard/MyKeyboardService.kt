package com.example.mykeyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.example.mykeyboard.engine.AutoCorrectEngine
import com.example.mykeyboard.engine.ClipboardHistoryManager
import com.example.mykeyboard.engine.PredictionEngine
import com.example.mykeyboard.engine.ProfessionalToneEngine
import com.example.mykeyboard.engine.UndoRedoManager
import android.widget.Toast
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
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.resetToAlpha()
        undoRedoManager.clear()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
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

    override fun onTextKey(text: String) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        // If committing a suggestion with a trailing space, learn the word
        val trimmed = text.trim()
        if (trimmed.isNotEmpty() && !trimmed.contains(" ") && !isEmojiOrSymbol(text)) {
            predictionEngine.learnWord(trimmed)
        }

        // Replace partial prefix if committing a suggestion word
        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val lastWord = textBefore.split(Regex("[^a-zA-Z0-9']")).lastOrNull() ?: ""

        if (!isEmojiOrSymbol(text) && text.startsWith(lastWord, ignoreCase = true) && lastWord.isNotEmpty() && text.length > lastWord.length) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }

        // Do not add trailing space for single characters or emojis
        val isSingleChar = text.length == 1
        val isEmoji = isEmojiOrSymbol(text)
        val toCommit = if (isSingleChar || isEmoji || text.endsWith(" ")) text else "$text "

        ic.commitText(toCommit, 1)
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
        recordCurrentSnapshot()

        val now = System.currentTimeMillis()
        val textBefore = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""

        // 1. Double space for ". " shortcut
        if (now - lastSpaceTime < 450) {
            if (textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTime = 0L
                checkAutoCaps()
                updatePredictions()
                recordCurrentSnapshot()
                return
            }
        }

        // 2. Auto-Correction on Space
        val words = textBefore.trimEnd().split(Regex("[^a-zA-Z0-9']")).filter { it.isNotEmpty() }
        val lastWord = words.lastOrNull() ?: ""

        if (lastWord.isNotEmpty()) {
            predictionEngine.learnWord(lastWord)

            val correction = AutoCorrectEngine.getCorrection(lastWord, preferences.autoCorrectMode)
            if (correction != null && correction != lastWord) {
                ic.deleteSurroundingText(lastWord.length, 0)
                ic.commitText("$correction ", 1)
                lastSpaceTime = now
                checkAutoCaps()
                updatePredictions()
                recordCurrentSnapshot()
                return
            }
        }

        ic.commitText(" ", 1)
        lastSpaceTime = now
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
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

    override fun onProfessionalRephrase() {
        val ic = currentInputConnection ?: return
        val fullText = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val activeSentence = fullText.split(Regex("[.\\n?!]")).lastOrNull()?.trim() ?: fullText.trim()
        val textToRephrase = if (activeSentence.isNotEmpty()) activeSentence else fullText.trim()

        if (textToRephrase.isEmpty()) {
            Toast.makeText(this, "Type a sentence first, then tap 💼 Professional Tone!", Toast.LENGTH_SHORT).show()
            return
        }

        val options = ProfessionalToneEngine.getProfessionalRephrasings(textToRephrase)
        if (options.isNotEmpty()) {
            Toast.makeText(this, "💼 Professional Corporate Suggestions Ready", Toast.LENGTH_SHORT).show()
            keyboardView?.showProfessionalSuggestions(textToRephrase, options)
        }
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
        val allWords = textBefore.trim().split(Regex("[^a-zA-Z0-9']")).filter { it.isNotEmpty() }

        val prefix = if (isAfterSpace) "" else (allWords.lastOrNull() ?: "")
        val prevWords = if (isAfterSpace) allWords else allWords.dropLast(1)

        keyboardView?.updatePredictions(prefix, prevWords)
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
        when (key) {
            KeyboardPreferences.KEY_THEME,
            KeyboardPreferences.KEY_NUMBER_ROW,
            KeyboardPreferences.KEY_HEIGHT_SCALE,
            KeyboardPreferences.KEY_CUSTOM_BG_PATH,
            KeyboardPreferences.KEY_CUSTOM_BG_OPACITY -> {
                keyboardView?.applyTheme(preferences.theme)
            }
        }
    }
}