package org.example.mykeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.KeyboardView
import android.text.method.MetaKeyKeyListener
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class SoftKeyboard : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    companion object {
        const val TAG = "SoftKeyboard"
    }

    private var mInputView: KeyboardView? = null

    private var mKeyboard: MyKeyboard? = null
    private var mSymbolsKeyboard: MyKeyboard? = null
    private var mSymbolsShiftedKeyboard: MyKeyboard? = null
    private var mQwertyKeyboard: MyKeyboard? = null
    private var mKoreanKeyboard: MyKeyboard? = null
    private var mKoreanShiftedKeyboard: MyKeyboard? = null
    private var mBackupKeyboard: MyKeyboard? = null
    private var mCurKeyboard: MyKeyboard? = null
    private var wordSeparators: String? = null
    private var kauto: KoreanAutomata? = null

    private var isHwShift = false
    private var isCapsLock = false
    private var mNoKorean = false
    private var mMetaState: Long = 0
    private var mLastShiftTime: Long = 0
    private var mLastDisplayWidth = 0
    private val mComposing = StringBuilder()

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        Log.v(TAG, "onInitializeInterface: 0.")
        super.onInitializeInterface()

        mKeyboard = MyKeyboard(this, R.xml.kbd_korean)
        if (mQwertyKeyboard != null) {
            val displayWidth = maxWidth
            if (displayWidth == mLastDisplayWidth) {
                return
            }
            mLastDisplayWidth = displayWidth
        }
        kauto = KoreanAutomata()
        mQwertyKeyboard = MyKeyboard(this, R.xml.kbd_english)
        mSymbolsKeyboard = MyKeyboard(this, R.xml.kbd_symbols)
        mSymbolsShiftedKeyboard = MyKeyboard(this, R.xml.kbd_symbols_shift)
        mKoreanKeyboard = MyKeyboard(this, R.xml.kbd_korean)
        mKoreanShiftedKeyboard = MyKeyboard(this, R.xml.kbd_korean_shifted)
        mBackupKeyboard = null
    }

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    override fun onCreate() {
        Log.v(TAG, "onCreate: 0.")
        super.onCreate()
        wordSeparators = resources.getString(R.string.word_separators)
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    override fun onCreateInputView(): View {
        Log.v(TAG, "onCreateInputView: 0.")

        mInputView = layoutInflater.inflate(R.layout.input, null) as KeyboardView
        mInputView!!.setOnKeyboardActionListener(this)
        mInputView!!.keyboard = mQwertyKeyboard

        Log.v(TAG, "onCreateInputView: 1. leave")
        return mInputView!!
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        Log.v(TAG, "onStartInputView: 0. restarting=$restarting")
        super.onStartInputView(attribute, restarting)

        mCurKeyboard = if (kauto!!.isKoreanMode()) mKoreanKeyboard else mQwertyKeyboard
        mInputView!!.keyboard = mCurKeyboard
        mInputView!!.closing()

        Log.v(TAG, "onStartInputView: 1. leave")
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    override fun onStartInput(ei: EditorInfo, restarting: Boolean) {
        Log.v(TAG, "onStartInput: 0. restarting=$restarting")
        super.onStartInput(ei, restarting)

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0)
        // updateCandidates()
        if (!restarting) {
            mMetaState = 0 // Clear shift states.
        }

        kauto!!.resetComposition()
        when (ei.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME -> {
                // Numbers and dates default to the symbols keyboard, with no extra features.
                mCurKeyboard = mSymbolsKeyboard
                mNoKorean = true
                if (kauto!!.isKoreanMode()) kauto!!.toggleMode()
            }

            EditorInfo.TYPE_CLASS_PHONE -> {
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard
                mNoKorean = true
                if (kauto!!.isKoreanMode()) kauto!!.toggleMode()
            }

            EditorInfo.TYPE_CLASS_TEXT -> {
                // We now look for a few special variations of text that will
                // modify our behavior.
                val variation = ei.inputType and EditorInfo.TYPE_MASK_VARIATION
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ) {
                    mNoKorean = true
                }
                mCurKeyboard = if (mNoKorean || !kauto!!.isKoreanMode()) mQwertyKeyboard else mKoreanKeyboard

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out shifted.
                updateShiftKeyState(ei)
            }

            else -> {
                // For all unknown input types, default to the alphabetic keyboard with no special features.
                // mCurKeyboard = mQwertyKeyboard
                mCurKeyboard = if (kauto!!.isKoreanMode()) mKoreanKeyboard else mQwertyKeyboard
                updateShiftKeyState(ei)
            }
        }

        // Update the label on the enter key, depending on what the application says it will do.
        mCurKeyboard!!.setImeOptions(resources, ei.imeOptions)

        Log.v(TAG, "onStartInput: 1. leave")
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like [.onCreateInputView].
     */
    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        Log.v(TAG, "onUpdateExtractingVisibility: 0.")
        ei.imeOptions = ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        super.onUpdateExtractingVisibility(ei)

        Log.v(TAG, "onUpdateExtractingVisibility: 1. leave")
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        Log.v(
            TAG,
            "-onUpdateSelection: 0." +
                    " oldSelStart=$oldSelStart oldSelEnd=$oldSelEnd" +
                    " newSelStart=$newSelStart newSelEnd=$newSelEnd" +
                    " candidatesStart=$candidatesStart candidatesEnd=$candidatesEnd"
        )
        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        Log.v(
            TAG,
            "-onUpdateSelection: 1. mComposing=$mComposing, newSelStart=$newSelStart newSelEnd=$newSelEnd"
        )
        // If the current selection in the text view changes, we should clear whatever candidate text we have.
        if (mComposing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0)
            kauto!!.resetComposition()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        Log.v(TAG, "onFinishInputView: 0. finishingInput=$finishingInput")
        super.onFinishInputView(finishingInput)

        if (kauto!!.isKoreanMode()) {
            kauto!!.resetComposition()
        }
        mKoreanKeyboard!!.isShifted = false

        Log.v(TAG, "onFinishInputView: 1. leave")
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    override fun onFinishInput() {
        Log.v(TAG, "onFinishInput: 0.")
        super.onFinishInput()

        // Clear current composing text and candidates.
        kauto!!.resetComposition()
        mNoKorean = false
        mComposing.setLength(0)

        setCandidatesViewShown(false)
        mCurKeyboard = mQwertyKeyboard
        if (mInputView != null) {
            mInputView!!.closing()
        }

        Log.v(TAG, "onFinishInput: 1. leave")
    }

    @Deprecated("Deprecated in Java")
    override fun onPress(primaryCode: Int) {
    }

    @Deprecated("Deprecated in Java")
    override fun onRelease(primaryCode: Int) {
    }

    /**
     * Implementation of KeyboardViewListener
     */
    @Deprecated("Deprecated in Java")
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        Log.v(TAG, "onKey: 0. primaryCode=$primaryCode, keyCodes=$keyCodes")

        if (isWordSeparator(primaryCode)) {
            Log.v(TAG, "onKey: 1. WORD_SEPARATOR")
            handleWordSeparator(primaryCode)
        } else if (primaryCode == KEYCODE_DELETE) {
            Log.v(TAG, "onKey: 1. DELETE")
            handleBackspace()
        } else if (primaryCode == KEYCODE_SHIFT) {
            Log.v(TAG, "onKey: 1. SHIFT")
            handleShift()
        } else if (primaryCode == KEYCODE_CANCEL) {
            Log.v(TAG, "onKey: 1. CANCEL")
            handleClose()
            return
        } else if (primaryCode == KEYCODE_ALT) {
            Log.v(TAG, "onKey: 1. ALT")
            handleAlt()
        } else if (primaryCode == KEYCODE_MODE_CHANGE && mInputView != null) {
            Log.v(TAG, "onKey: 1. MODE_CHANGE")
            handleModeChange()
        } else if (primaryCode == KEYCODE_DPAD_UP) {
            Log.v(TAG, "onKey: 1. DPAD_UP")
            handleDpad(KEYCODE_DPAD_UP)
        } else if (primaryCode == KEYCODE_DPAD_DOWN) {
            Log.v(TAG, "onKey: 1. DPAD_DOWN")
            handleDpad(KEYCODE_DPAD_DOWN)
        } else if (primaryCode == KEYCODE_DPAD_LEFT) {
            Log.v(TAG, "onKey: 1. DPAD_LEFT")
            handleDpad(KEYCODE_DPAD_LEFT)
        } else if (primaryCode == KEYCODE_DPAD_RIGHT) {
            Log.v(TAG, "onKey: 1. DPAD_RIGHT")
            handleDpad(KEYCODE_DPAD_RIGHT)
        } else {
            Log.v(TAG, "onKey: 1. CHARACTER")
            handleCharacter(primaryCode)
        }
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.v(TAG, "onKeyDown: 0. keyCode=$keyCode event=$event")
        if (event.isShiftPressed) {
            isHwShift = true
        }

        if (event.metaState and (KeyEvent.META_ALT_MASK or KeyEvent.META_CTRL_MASK) == 0) {
            Log.v(TAG, "onKeyDown: 1. keyCode=$keyCode")
            when (keyCode) {
                KEYCODE_BACK ->
                    // The InputMethodService already takes care of the back key for us,
                    // to dismiss the input method if it is shown.
                    // However, our keyboard could be showing a pop-up window that back should dismiss,
                    // so we first allow it to do that.
                    if (event.repeatCount == 0 && mInputView != null) {
                        if (mInputView!!.handleBack()) {
                            return true
                        }
                    }

                KEYCODE_SPACE -> return if (!mNoKorean && event.isShiftPressed) {
                    if (mComposing.isNotEmpty()) {
                        val ic = currentInputConnection
                        ic?.let { commitTyped(it) }
                    }
                    if (kauto!!.isKoreanMode()) {
                        kauto!!.resetComposition()
                    }
                    kauto!!.toggleMode()
                    Log.v(TAG, "onKeyDown: 1. SHIFT SPACE. isKoreanMode=" + kauto!!.isKoreanMode())
                    true
                } else {
                    translateKeyDown(keyCode, event)
                    true
                }

                KEYCODE_HANGUL ->
                    return if (!mNoKorean) {
                        Log.v(
                            TAG,
                            "onKeyDown: 1. KEYCODE_HANGUL. isKoreanMode=" + kauto!!.isKoreanMode()
                        )
                        if (mComposing.isNotEmpty()) {
                            val ic = currentInputConnection
                            ic?.let { commitTyped(it) }
                        }
                        if (kauto!!.isKoreanMode()) {
                            kauto!!.resetComposition()
                        }
                        kauto!!.toggleMode()
                        true
                    } else {
                        Log.v(
                            TAG,
                            "onKeyDown: 1. KEYCODE_HANGUL. but Korean is not allowed in this context. ignore this."
                        )
                        false
                    }

                KEYCODE_DEL ->
                    if (mComposing.isNotEmpty()) {
                        onKey(KEYCODE_DELETE, null)
                        return true
                    }

                KEYCODE_ENTER -> {
                    // Let the underlying text editor always handle these.
                    if (kauto!!.isKoreanMode()) {
                        kauto!!.resetComposition()
                    }
                    return false
                }

                else -> {
                    if (translateKeyDown(keyCode, event)) {
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Log.v(TAG, "onKeyUp: 0. keyCode=$keyCode event=$event")
        mMetaState = MetaKeyKeyListener.handleKeyUp(
            mMetaState,
            keyCode, event
        )
        isHwShift = false
        return super.onKeyUp(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onText(text: CharSequence?) {
        Log.v(TAG, "onText: 0. text=$text")
        val ic = currentInputConnection ?: return
        if (mComposing.isNotEmpty()) {
            commitTyped(ic)
        }
        ic.commitText(text, 0)
        updateShiftKeyState(currentInputEditorInfo)
    }

    @Deprecated("Deprecated in Java")
    override fun swipeLeft() {
        handleBackspace()
    }

    @Deprecated("Deprecated in Java")
    override fun swipeRight() {
    }

    @Deprecated("Deprecated in Java")
    override fun swipeUp() {
    }

    @Deprecated("Deprecated in Java")
    override fun swipeDown() {
        handleClose()
    }

    private fun handleWordSeparator(primaryCode: Int) {
        Log.v(TAG, "-handleWordSeparator: 0. primaryCode=$primaryCode")

        if (mComposing.isNotEmpty()) {
            commitTyped(currentInputConnection)
        }
        if (kauto!!.isKoreanMode()) {
            kauto!!.resetComposition()
        }
        sendKey(primaryCode)
        if (mInputView != null) {
            updateShiftKeyState(currentInputEditorInfo)
        }
    }

    private fun handleBackspace() {
        Log.v(TAG, "-handleBackspace: 0.")

        Log.v(TAG, "-handleBackspace: 1. mKoreanMode=${kauto!!.isKoreanMode()}")
        if (kauto!!.isKoreanMode()) {
            val result = kauto!!.doBackSpace()
            Log.v(TAG, "-handleBackspace: 2. result=$result")

            if (result == ACTION_ERROR) {
                updateShiftKeyState(currentInputEditorInfo)
                return
            }
            if (result == ACTION_UPDATE_COMPOSITE) {
                val compositeString = kauto!!.getCompositeString();
                if (compositeString !== "") {
                    if (mComposing.isNotEmpty()) {
                        mComposing.replace(mComposing.length - 1, mComposing.length, compositeString)
                        currentInputConnection.setComposingText(mComposing, 1)
                    }
                    updateShiftKeyState(currentInputEditorInfo)
                    return
                }
            }
        }

        val length = mComposing.length
        if (length > 1) {
            mComposing.delete(length - 1, length)
            currentInputConnection.setComposingText(mComposing, 1)
        } else if (length > 0) {
            mComposing.setLength(0)
            currentInputConnection.commitText("", 0)
        } else {
            keyDownUp(KEYCODE_DEL)
        }
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun handleShift() {
        Log.v(TAG, "-handleShift: 0.")

        if (mInputView == null) {
            return
        }

        val currentKeyboard = mInputView!!.keyboard
        Log.v(TAG, "-handleShift: 2. currentKeyboard=$currentKeyboard")
        if (currentKeyboard === mQwertyKeyboard) {
            checkToggleCapsLock()
            mInputView!!.isShifted = isCapsLock || !mInputView!!.isShifted
        } else if (currentKeyboard === mKoreanKeyboard) {
            mKoreanKeyboard!!.isShifted = true
            mInputView!!.keyboard = mKoreanShiftedKeyboard
            mKoreanShiftedKeyboard!!.isShifted = true
        } else if (currentKeyboard === mKoreanShiftedKeyboard) {
            mKoreanShiftedKeyboard!!.isShifted = false
            mInputView!!.keyboard = mKoreanKeyboard
            mKoreanKeyboard!!.isShifted = false
        } else if (currentKeyboard === mSymbolsKeyboard) {
            mSymbolsKeyboard!!.isShifted = true
            mInputView!!.keyboard = mSymbolsShiftedKeyboard
            mSymbolsShiftedKeyboard!!.isShifted = true
        } else if (currentKeyboard === mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard!!.isShifted = false
            mInputView!!.keyboard = mSymbolsKeyboard
            mSymbolsKeyboard!!.isShifted = false
        }
    }

    private fun handleClose() {
        Log.v(TAG, "-handleClose: 0.")
        commitTyped(currentInputConnection)
        requestHideSelf(0)
        mInputView!!.closing()
    }

    private fun handleAlt() {
        Log.v(TAG, "-handleAlt: 0.")
        if (mInputView == null) {
            return
        }

        if (mNoKorean) {
            Log.v(TAG, "-handleAlt: 1. Not Korean")
            return
        }

        val current = if (kauto!!.isKoreanMode()) {
            Log.v(TAG, "-handleAlt: 1. change keyboard view to English")
            mQwertyKeyboard
        } else {
            Log.v(TAG, "-handleAlt: 1. change keyboard view to Korean")
            mKoreanKeyboard
        }

        mInputView!!.keyboard = current
        if (mComposing.isNotEmpty()) {
            commitTyped(currentInputConnection)
        }
        kauto!!.toggleMode()
        if (current === mQwertyKeyboard || current === mKoreanKeyboard) {
            current!!.isShifted = false
        }
    }

    private fun handleModeChange() {
        Log.v(TAG, "-handleModeChange: 0.")

        var current = mInputView!!.keyboard
        Log.v(TAG, "-handleModeChange: change keyboard view from $current")
        if (current === mSymbolsKeyboard || current === mSymbolsShiftedKeyboard) {
            current = if (mBackupKeyboard != null) mBackupKeyboard else mQwertyKeyboard
            mBackupKeyboard = null
            // reset Korean input mode.
            if (current === mQwertyKeyboard && kauto!!.isKoreanMode()) {
                kauto!!.toggleMode()
            }
        } else {
            mBackupKeyboard = current as MyKeyboard
            current = mSymbolsKeyboard
            // need to submit current composition string
            if (mComposing.isNotEmpty()) {
                commitTyped(currentInputConnection)
            }
            if (kauto!!.isKoreanMode()) {
                kauto!!.resetComposition()
            }
        }
        mInputView!!.keyboard = current
        if (current === mSymbolsKeyboard) {
            current!!.isShifted = false
        }
    }

    private fun handleCharacter(primaryCode: Int) {
        Log.v(TAG, "-handleCharacter: 0. primaryCode=$primaryCode, isHwShift=$isHwShift")

        var code: Int = primaryCode
        var keyState = KEYSTATE_NONE
        if (isInputViewShown) {
            if (mInputView!!.isShifted) {
                code = Character.toUpperCase(code)
                keyState = keyState or KEYSTATE_SHIFT
            }
        }
        if (isHwShift) {
            keyState = keyState or KEYSTATE_SHIFT
        }

        val isLetter = Character.isLetter(code)
        Log.v(TAG, "-handleCharacter: 3. isLetter=$isLetter")
        if (!isLetter) {
            if (mComposing.isNotEmpty()) {
                currentInputConnection.commitText(mComposing, 1)
                mComposing.setLength(0)
            }
            kauto!!.resetComposition()
            currentInputConnection.commitText(code.toChar().toString(), 1)
            return
        }

        val result = kauto!!.doAutomata(code.toChar(), keyState)
        Log.v(TAG, "-handleCharacter: 4. result=$result")
        val isUpdateComplete: Boolean = (result and ACTION_UPDATE_COMPLETE) != 0
        val isUpdateComposite: Boolean = (result and ACTION_UPDATE_COMPOSITE) != 0
        val isAppend: Boolean = (result and ACTION_APPEND) != 0
        val isAsResult: Boolean = (result and ACTION_USE_INPUT_AS_RESULT) != 0

        Log.v(
            TAG,
            "-handleCharacter: 5. isUpdateComplete=$isUpdateComplete, isUpdateComposite=$isUpdateComposite, isAppend=$isAppend, isAsResult=$isAsResult"
        )
        if (result < 0) {
            if (kauto!!.isKoreanMode()) {
                kauto!!.toggleMode()
            }
        } else {
            if (isUpdateComplete) {
                val completeString = kauto!!.getCompleteString()
                if (mComposing.isNotEmpty()) {
                    mComposing.replace(mComposing.length - 1, mComposing.length, completeString)
                } else {
                    mComposing.append(completeString)
                }
                if (mComposing.isNotEmpty()) {
                    currentInputConnection.setComposingText(mComposing, 1)
                    // commitTyped(getCurrentInputConnection());
                }
            }
            if (isUpdateComposite) {
                val compositeString = kauto!!.getCompositeString()
                if (mComposing.isNotEmpty() && !isUpdateComplete && !isAppend) {
                    Log.v(TAG, "-handleCharacter: 6. mComposing=$mComposing, compositeString=$compositeString")
                    mComposing.replace(mComposing.length - 1, mComposing.length, compositeString)
                } else {
                    mComposing.append(compositeString)
                }
                currentInputConnection.setComposingText(mComposing, 1)
            }
        }
        if (isAsResult) {
            mComposing.append(code.toChar())
            currentInputConnection.setComposingText(mComposing, 1)
        }
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun handleDpad(primaryCode: Int) {
        Log.v(TAG, "-handleDpad: 0. primaryCode=[$primaryCode]")

        if (mComposing.isNotEmpty()) {
            currentInputConnection.commitText(mComposing, 1)
            mComposing.setLength(0)
        }
        kauto!!.resetComposition()
        currentInputConnection.commitText(primaryCode.toChar().toString(), 1)
    }

    private fun checkToggleCapsLock() {
        val now = System.currentTimeMillis()
        if (mLastShiftTime + 800 > now) {
            isCapsLock = !isCapsLock
            mLastShiftTime = 0
        } else {
            mLastShiftTime = now
        }
    }

    private fun isWordSeparator(code: Int): Boolean {
        val separators = wordSeparators
        return separators!!.contains(code.toChar().toString())
    }


    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.
     */
    private fun translateKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.v(TAG, "translateKeyDown: 0. keyCode=$keyCode event=$event")

        mMetaState = MetaKeyKeyListener.handleKeyDown(
            mMetaState,
            keyCode, event
        )
        var c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState))
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState)
        val ic = currentInputConnection
        if (c == 0 || ic == null) {
            return false
        }
        if (c and KeyCharacterMap.COMBINING_ACCENT != 0) {
            c = c and KeyCharacterMap.COMBINING_ACCENT_MASK
        }
        if (mComposing.isNotEmpty()) {
            val accent = mComposing[mComposing.length - 1]
            val composed = KeyEvent.getDeadChar(accent.code, c)
            if (composed != 0) {
                c = composed
                mComposing.setLength(mComposing.length - 1)
            }
        }
        onKey(c, null)
        return true
    }

    /**
     * Helper function to send a key down / key up pair to the current editor.
     */
    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode)
        )
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, keyEventCode)
        )
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private fun commitTyped(inputConnection: InputConnection) {
        Log.v(TAG, "-commitTyped: 0. mComposing=$mComposing length=${mComposing.length}")
        if (mComposing.isNotEmpty()) {
            inputConnection.commitText(mComposing, 1)
            mComposing.setLength(0)
        }
    }

    /**
     * Helper function to update the shift state of our keyboard based on the initial editor state.
     */
    private fun updateShiftKeyState(ei: EditorInfo?) {
        Log.v(TAG, "--updateShiftKeyState: 0.")
        if (ei != null && mInputView != null) {
            if (mQwertyKeyboard === mInputView!!.keyboard) {
                var caps = 0
                val ei = currentInputEditorInfo
                if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                    caps = currentInputConnection.getCursorCapsMode(ei.inputType)
                }
                Log.v(TAG, "--updateShiftKeyState: 1. mCapsLock=$isCapsLock, caps=$caps")
                mInputView!!.isShifted = isCapsLock || caps != 0
            } else if (mKoreanShiftedKeyboard === mInputView!!.keyboard) {
                mKoreanShiftedKeyboard!!.isShifted = false
                mInputView!!.isShifted = false
                mKoreanKeyboard!!.isShifted = false
            }
        }
    }

    /**
     * Helper function to send a character to the editor as raw key events.
     */
    private fun sendKey(keyCode: Int) {
        Log.v(TAG, "--sendKey: 0. keyCode=$keyCode")
        when (keyCode) {
            '\n'.code -> keyDownUp(KeyEvent.KEYCODE_ENTER)
            in '0'.code..'9'.code -> keyDownUp(keyCode - '0'.code + KeyEvent.KEYCODE_0)
            else -> currentInputConnection.commitText(keyCode.toChar().toString(), 1)
        }
    }

}
