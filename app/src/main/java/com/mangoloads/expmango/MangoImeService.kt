package com.mangoloads.expmango

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.util.Locale

class MangoImeService : InputMethodService() {
    private lateinit var engine: PredictionEngine
    private lateinit var keyboard: MangoKeyboardView

    private var fieldPolicy = FieldPolicyResolver.resolve(FieldMode.NORMAL)
    private var wordBuffer = StringBuilder()
    private var lastAutoCorrection: Pair<String, String>? = null
    private var shift = false
    private var capsLock = false

    override fun onCreate() {
        super.onCreate()
        engine = PredictionEngine(this)
    }

    override fun onCreateInputView(): android.view.View {
        keyboard = MangoKeyboardView(this)
        return keyboard
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        fieldPolicy = FieldPolicyResolver.fromEditorInfo(info)
        wordBuffer = StringBuilder()
        lastAutoCorrection = null
        shift = false
        capsLock = false
    }

    override fun onFinishInput() {
        super.onFinishInput()
        wordBuffer.setLength(0)
        lastAutoCorrection = null
    }

    override fun onDestroy() {
        if (::engine.isInitialized) engine.shutdown()
        super.onDestroy()
    }

    fun onCharacter(character: Char) {
        val output = if (shift || capsLock) character.uppercaseChar() else character
        currentInputConnection?.commitText(output.toString(), 1)
        wordBuffer.append(output.lowercaseChar())

        if (shift && !capsLock) shift = false
        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun onBackspace() {
        val ic = currentInputConnection ?: return

        lastAutoCorrection?.let { correction ->
            if (wordBuffer.isEmpty()) {
                val typo = correction.first
                val fixed = correction.second
                ic.deleteSurroundingText(fixed.length + 1, 0)
                ic.commitText(typo + " ", 1)
                lastAutoCorrection = null
                refreshSuggestions()
                return
            }
        }

        if (wordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(1, 0)
            wordBuffer.deleteCharAt(wordBuffer.length - 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }

        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun onSpace() {
        val ic = currentInputConnection ?: return
        val typed = wordBuffer.toString()
        val previous = previousWord(ic)

        if (fieldPolicy.canAutocorrect) {
            engine.correct(typed, previous, policy = fieldPolicy)?.let { correction ->
                if (correction.word.lowercase(Locale.ROOT) != typed.lowercase(Locale.ROOT)) {
                    ic.deleteSurroundingText(typed.length, 0)
                    ic.commitText(correction.word + " ", 1)
                    lastAutoCorrection = typed to correction.word
                    if (fieldPolicy.canLearn) {
                        engine.learn(correction.word, previous, policy = fieldPolicy)
                    }
                    wordBuffer.clear()
                    refreshSuggestions()
                    return
                }
            }
        }

        ic.commitText(" ", 1)

        if (fieldPolicy.canLearn && typed.isNotBlank()) {
            engine.learn(typed, previous, policy = fieldPolicy)
        }

        wordBuffer.clear()
        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun onEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        wordBuffer.clear()
        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun onSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val previous = previousWord(ic)

        if (wordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(wordBuffer.length, 0)
        }

        ic.commitText(word + " ", 1)

        if (fieldPolicy.canLearn) {
            engine.learn(word, previous, policy = fieldPolicy)
        }

        wordBuffer.clear()
        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun onGlide(raw: String) {
        val ic = currentInputConnection ?: return
        val previous = previousWord(ic)

        val prediction = engine.glide(raw, previous, policy = fieldPolicy)

        if (prediction == null) {
            for (c in raw) onCharacter(c)
            return
        }

        ic.commitText(prediction.word + " ", 1)

        if (fieldPolicy.canLearn) {
            engine.learn(prediction.word, previous, policy = fieldPolicy)
        }

        wordBuffer.clear()
        lastAutoCorrection = null
        refreshSuggestions()
    }

    fun toggleShift() {
        if (shift) capsLock = !capsLock else shift = true
        keyboard.invalidate()
    }

    fun currentShifted(): Boolean = shift || capsLock

    fun getFieldPolicy(): FieldPolicy = fieldPolicy

    fun refreshSuggestions() {
        if (!::keyboard.isInitialized) return
        val previous = previousWord(currentInputConnection)
        val candidates = engine.suggestions(wordBuffer.toString(), previous1 = previous, policy = fieldPolicy, limit = 3)
            .map { it.word }
        keyboard.setSuggestions(candidates, fieldPolicy.isPrivate)
    }

    private fun previousWord(ic: InputConnection?): String? {
        if (ic == null) return null
        val before = ic.getTextBeforeCursor(80, 0)?.toString() ?: return null
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return null
        return trimmed.split(Regex("\\s+")).lastOrNull()
    }
}
