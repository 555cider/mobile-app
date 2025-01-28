package org.example.mykeyboard

import android.util.Log
import org.example.mykeyboard.Constants.Automata.State
import org.example.mykeyboard.InputTables.FirstConsonant
import org.example.mykeyboard.InputTables.LastConsonant
import org.example.mykeyboard.InputTables.NormalKeyMap
import org.example.mykeyboard.InputTables.ShiftedKeyMap
import org.example.mykeyboard.InputTables.Vowel

class KoreanAutomata {

    companion object {
        const val TAG = "KoreanAutomata"
    }

    private var state = State.INITIAL
    private var compositeString = ""
    private var completeString = ""
    private var isKoreanMode = false

    init {
        resetComposition()
        isKoreanMode = false
    }

    fun resetComposition() {
        state = State.INITIAL
        compositeString = ""
        completeString = ""
    }

    fun getState(): State {
        return state
    }

    fun getCompositeString(): String {
        return compositeString
    }

    fun getCompleteString(): String {
        return completeString
    }

    fun toggleMode() {
        isKoreanMode = !isKoreanMode
    }

    fun isKoreanMode(): Boolean {
        return isKoreanMode
    }

    private fun isHangul(word: Char): Boolean {
        return when (word.code) {
            in HANGUL_START..HANGUL_END, in HANGUL_JAMO_START..HANGUL_JAMO_END -> true
            else -> false
        }
    }

    private fun isJamo(word: Char): Boolean {
        return word.code in HANGUL_JAMO_START..HANGUL_JAMO_END
    }

    private fun isConsonant(word: Char): Boolean {
        return word.code in HANGUL_JAMO_START until HANGUL_MO_START
    }

    private fun isVowel(word: Char): Boolean {
        return word.code in HANGUL_MO_START..HANGUL_JAMO_END
    }

    private fun getFirstConsonantIndex(word: Char): Int {
        if (!isHangul(word)) {
            return -1
        }
        if (!isConsonant(word)) {
            val offset = word.code - HANGUL_START
            return offset / (NUM_OF_MIDDLE * NUM_OF_LAST_INDEX)
        }
        return FirstConsonant.Word.indexOf(word)
    }

    private fun getVowelIndex(word: Char): Int {
        if (!isHangul(word)) {
            return -1
        }
        if (!isVowel(word)) {
            val offset = word.code - HANGUL_START
            return offset % (NUM_OF_MIDDLE * NUM_OF_LAST_INDEX) / NUM_OF_LAST_INDEX
        }
        return Vowel.Word.indexOf(word)
    }

    private fun getLastConsonantIndex(word: Char): Int {
        if (!isHangul(word)) {
            return -1
        }
        if (!isJamo(word)) {
            val offset = word.code - HANGUL_START
            return offset % NUM_OF_LAST_INDEX
        }
        if (!isConsonant(word)) {
            return -1
        }
        return LastConsonant.Word.indexOf(word)
    }

    private fun getFirstConsonant(word: Char): Char {
        val index = getFirstConsonantIndex(word)
        return if (index < 0) 0.toChar() else FirstConsonant.Word[index]
    }

    private fun getVowel(word: Char): Char {
        val index = getVowelIndex(word)
        return if (index < 0) 0.toChar() else Vowel.Word[index]
    }

    private fun getLastConsonant(word: Char): Char {
        val index = getLastConsonantIndex(word)
        return if (index < 0) 0.toChar() else LastConsonant.Word[index]
    }

    private fun convertFirstConsonantToIndex(word: Char): Int {
        return FirstConsonant.Word.indexOf(word)
    }

    private fun convertVowelToIndex(word: Char): Int {
        val index = word - Vowel.Word[0]
        if (index < 0) {
            return -1
        }
        return if (index >= NUM_OF_MIDDLE) -1 else index
    }

    private fun convertLastConsonantToIndex(word: Char): Int {
        return LastConsonant.Word.indexOf(word)
    }

    private fun combineVowelWithIndex(index1: Int, index2: Int): Int {
        val newWord = combineVowelWithWord(Vowel.Word[index1], Vowel.Word[index2])
        return if (newWord == 0.toChar()) -1 else convertVowelToIndex(newWord)
    }

    private fun combineVowelWithWord(word1: Char, word2: Char): Char {
        return when (word1.code) {
            0x3157 -> when (word2.code) { // ㅗ
                0x314F -> 0x3158.toChar() // ㅗ + ㅏ = ㅘ
                0x3150 -> 0x3159.toChar() // ㅗ + ㅐ = ㅙ
                0x3163 -> 0x315A.toChar() // ㅗ + ㅣ = ㅚ
                else -> 0.toChar()
            }

            0x315C -> when (word2.code) { // ㅜ
                0x3153 -> 0x315D.toChar()  // ㅜ + ㅓ = ㅝ
                0x3154 -> 0x315E.toChar()  // ㅜ + ㅔ = ㅞ
                0x3163 -> 0x315F.toChar() // ㅜ + ㅣ = ㅟ
                else -> 0.toChar()
            }

            0x3161 -> if (word2.code == 0x3163) 0x3162.toChar() else 0.toChar() // ㅡ + ㅣ = ㅢ
            else -> 0.toChar()
        }
    }

    private val lastConsonantCombinationMap = mapOf(
        0x3131 to mapOf(0x3145 to 0x3133.toChar()), // ㄱ + ㅅ = ㄳ
        0x3142 to mapOf(0x3145 to 0x3144.toChar()), // ㅂ + ㅅ = ㅄ
        0x3134 to mapOf( // ㄴ
            0x3148 to 0x3135.toChar(), // ㄴ + ㅈ = ㄵ
            0x314E to 0x3136.toChar() // ㄴ + ㅎ = ㄶ
        ),
        0x3139 to mapOf( // ㄹ
            0x3131 to 0x313A.toChar(), // ㄹ + ㄱ = ㄺ
            0x3141 to 0x313B.toChar(), // ㄹ + ㅁ = ㄻ
            0x3142 to 0x313C.toChar(), // ㄹ + ㅂ = ㄼ
            0x3145 to 0x313D.toChar(), // ㄹ + ㅅ = ㄽ
            0x314C to 0x313E.toChar(), // ㄹ + ㅌ = ㄾ
            0x314D to 0x313F.toChar(), // ㄹ + ㅍ = ㄿ
            0x314E to 0x3140.toChar()  // ㄹ + ㅎ = ㅀ
        )
    )

    private fun combineLastConsonantWithIndex(index1: Int, index2: Int): Int {
        val newWord = combineLastConsonantWithWord(LastConsonant.Word[index1], LastConsonant.Word[index2])
        return if (newWord == 0.toChar()) -1 else convertLastConsonantToIndex(newWord)
    }

    private fun combineLastConsonantWithWord(word1: Char, word2: Char): Char {
        return lastConsonantCombinationMap[word1.code]?.get(word2.code) ?: 0.toChar()
    }

    private fun composeWordWithIndexes(firstConsonantIndex: Int, vowelIndex: Int, lastConsonantIndex: Int): Char {
        if (firstConsonantIndex !in 0 until NUM_OF_FIRST ||
            vowelIndex !in 0 until NUM_OF_MIDDLE ||
            lastConsonantIndex !in 0 until NUM_OF_LAST
        ) {
            return 0.toChar()
        }
        val offset =
            firstConsonantIndex * NUM_OF_MIDDLE * NUM_OF_LAST_INDEX + vowelIndex * NUM_OF_LAST_INDEX + lastConsonantIndex
        return (offset + HANGUL_START).toChar()
    }

    private fun getAlphabetIndex(code: Char): Int {
        return when (code) {
            in 'a'..'z' -> code - 'a'
            in 'A'..'Z' -> code - 'A'
            else -> -1
        }
    }

    fun doBackSpace(): Int {
        Log.v(TAG, "doBackSpace: 0. state=$state")

        if (state == State.INITIAL) {
            return ACTION_USE_INPUT_AS_RESULT
        }

        val word = if (compositeString.isNotEmpty()) compositeString[0] else 0.toChar()
        if (word == 0.toChar()) {
            return ACTION_ERROR
        }

        return when (state) {
            State.INITIAL -> ACTION_USE_INPUT_AS_RESULT
            State.CONSONANT -> {
                resetComposition()
                ACTION_USE_INPUT_AS_RESULT
            }

            State.COMPOUND_CONSONANT -> {
                val index = getLastConsonantIndex(word)
                if (index < 0) {
                    return ACTION_ERROR
                }
                val newIndex = LastConsonant.Last[index]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = LastConsonant.Word[newIndex].toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPOSITE
            }

            State.VOWEL -> {
                resetComposition()
                ACTION_USE_INPUT_AS_RESULT
            }

            State.COMPOUND_VOWEL -> {
                val index = getVowelIndex(word)
                if (index < 0) {
                    return ACTION_ERROR
                }
                val newIndex = Vowel.Middle[index]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = Vowel.Word[newIndex].toString()
                state = State.VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_VOWEL -> {
                compositeString = getFirstConsonant(word).toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_VOWEL_CONSONANT -> {
                compositeString = (word.code - getLastConsonantIndex(word)).toChar().toString()
                state = State.CONSONANT_VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_VOWEL_COMPOUND_CONSONANT -> {
                val index = getLastConsonantIndex(word)
                if (index < 0) {
                    return ACTION_ERROR
                }
                val newIndex = LastConsonant.Last[index]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = (word.code - index + newIndex).toChar().toString()
                state = State.CONSONANT_VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_COMPOUND_VOWEL -> {
                val firstConsonantIndex = getFirstConsonantIndex(word)
                val vowelIndex = getVowelIndex(word)
                val newIndex = Vowel.Middle[vowelIndex]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = composeWordWithIndexes(firstConsonantIndex, newIndex, 0).toString()
                state = State.CONSONANT_VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_COMPOUND_VOWEL_CONSONANT -> {
                val index = getLastConsonantIndex(word)
                if (index < 0) {
                    return ACTION_ERROR
                }
                val newIndex = LastConsonant.Last[index]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = (word.code - index + newIndex).toChar().toString()
                state = State.CONSONANT_COMPOUND_VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            State.CONSONANT_COMPOUND_VOWEL_COMPOUND_CONSONANT -> {
                val index = getLastConsonantIndex(word)
                if (index < 0) {
                    return ACTION_ERROR
                }
                val newIndex = LastConsonant.Last[index]
                if (newIndex < 0) {
                    return ACTION_ERROR
                }
                compositeString = (word.code - index + newIndex).toChar().toString()
                state = State.CONSONANT_COMPOUND_VOWEL
                ACTION_UPDATE_COMPOSITE
            }

            else -> ACTION_ERROR
        }
    }

    fun doAutomata(word: Char, keyState: Int): Int {
        Log.v(TAG, "doAutomata: 0. word=$word, KeyState=$keyState, state=$state")

        // word가 [a, ... z, A, ... Z]에 속하지 않은 경우
        val alphaIndex = getAlphabetIndex(word)
        if (alphaIndex < 0) {
            var result = ACTION_NONE
            // flush Korean characters first.
            if (isKoreanMode) {
                completeString = compositeString
                compositeString = ""
                state = State.INITIAL
                result = ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            }
            // process the code as English
            if (keyState == 0) {
                result = result or ACTION_USE_INPUT_AS_RESULT
            }
            return result
        }

        if (!isKoreanMode) {
            return ACTION_USE_INPUT_AS_RESULT
        }

        val hCode =
            if (keyState and KEYSTATE_SHIFT_MASK == 0) NormalKeyMap.Word[alphaIndex] else ShiftedKeyMap.Word[alphaIndex]
        Log.v(TAG, "doAutomata: 1. hCode=$hCode, state=$state")
        return when (state) {
            State.INITIAL -> forInitial(hCode)
            State.CONSONANT -> forConsonant(hCode)
            State.COMPOUND_CONSONANT -> forCompoundConsonant(hCode)
            State.VOWEL -> forVowel(hCode)
            State.COMPOUND_VOWEL -> forCompoundVowel(hCode)
            State.CONSONANT_VOWEL -> forConsonantVowel(hCode)
            State.CONSONANT_VOWEL_CONSONANT -> forConsonantVowelConsonant(hCode)
            State.CONSONANT_VOWEL_COMPOUND_CONSONANT -> forConsonantVowelCompoundConsonant(hCode)
            State.CONSONANT_COMPOUND_VOWEL -> forConsonantCompoundVowel(hCode)
            State.CONSONANT_COMPOUND_VOWEL_CONSONANT -> forConsonantCompoundVowelLastConsonant(hCode)
            State.CONSONANT_COMPOUND_VOWEL_COMPOUND_CONSONANT -> forConsonantCompoundVowelCompoundConsonant(hCode)
            else -> ACTION_ERROR
        }
    }

    /**
     * 조합: NULL
     */
    private fun forInitial(word: Char): Int {
        Log.v(TAG, "-forInitial: 0. word=$word")

        state = if (isConsonant(word)) State.CONSONANT else State.VOWEL
        completeString = ""
        compositeString = word.toString()
        return ACTION_UPDATE_COMPOSITE or ACTION_APPEND
    }

    /**
     * 조합: single Consonant only
     * 예시: ㄱ
     */
    private fun forConsonant(word: Char): Int {
        Log.v(TAG, "-forConsonant: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            val newWord = combineLastConsonantWithWord(compositeString[0], word)
            Log.v(TAG, "-forConsonant: 1. newWord=$newWord")
            return if (newWord == 0.toChar()) {
                completeString = compositeString
                compositeString = word.toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            } else {
                completeString = ""
                compositeString = newWord.toString()
                state = State.COMPOUND_CONSONANT
                ACTION_UPDATE_COMPOSITE
            }
        }

        val firstConsonantIndex = convertFirstConsonantToIndex(compositeString[0])
        val vowelIndex = convertVowelToIndex(word)
        val newWord = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0)
        Log.v(TAG, "-forConsonant: 1. newWord=$newWord")
        state = State.CONSONANT_VOWEL
        completeString = ""
        compositeString = newWord.toString()
        return ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: compound Consonant
     * 예시: ㄳ
     */
    private fun forCompoundConsonant(word: Char): Int {
        Log.v(TAG, "-forCompoundConsonant: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            completeString = compositeString
            compositeString = word.toString()
            state = State.CONSONANT
            return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }

        val lastConsonantIndex0 = getLastConsonantIndex(compositeString[0])
        val lastConsonantIndex1 = LastConsonant.Last[lastConsonantIndex0]
        val firstConsonantIndex = LastConsonant.First[lastConsonantIndex0]
        val vowelIndex = getVowelIndex(word)
        completeString = LastConsonant.Word[lastConsonantIndex1].toString()
        compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
        state = State.CONSONANT_VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: single Vowel
     * 예시: ㅏ
     */
    private fun forVowel(word: Char): Int {
        Log.v(TAG, "-forVowel: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            completeString = compositeString
            compositeString = word.toString()
            state = State.CONSONANT
            return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }

        val newWord = combineVowelWithWord(compositeString[0], word)
        Log.v(TAG, "-forVowel: 1. newWord=$newWord")
        return if (newWord != 0.toChar()) {
            completeString = ""
            compositeString = newWord.toString()
            state = State.COMPOUND_VOWEL
            ACTION_UPDATE_COMPOSITE
        } else {
            completeString = compositeString
            compositeString = word.toString()
            state = State.VOWEL
            ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }
    }

    /**
     * 조합: compound Vowel
     * 예시: ㅘ
     */
    private fun forCompoundVowel(word: Char): Int {
        Log.v(TAG, "-forCompoundVowel: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            completeString = compositeString
            compositeString = word.toString()
            state = State.CONSONANT
            return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }

        val newWord = combineVowelWithWord(compositeString[0], word)
        Log.v(TAG, "-forCompoundVowel: 3. newWord=$newWord")
        return if (newWord != 0.toChar()) {
            completeString = ""
            compositeString = newWord.toString()
            state = State.COMPOUND_VOWEL
            ACTION_UPDATE_COMPOSITE
        } else {
            completeString = compositeString
            compositeString = word.toString()
            state = State.VOWEL
            ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }
    }

    /**
     * 조합: single Consonant + single Vowel
     * 예시: 가
     */
    private fun forConsonantVowel(word: Char): Int {
        Log.v(TAG, "-forConsonantVowel: 0. word=$word, compositeString=$compositeString")

        val isEmpty = compositeString.isEmpty()
        if (isEmpty) {
            return ACTION_ERROR
        }

        val isConsonant = isConsonant(word)
        if (isConsonant) {
            val index = getLastConsonantIndex(word)
            return if (index != -1) {
                val tempChar = compositeString[0]
                completeString = ""
                compositeString = (tempChar.code + index).toChar().toString()
                state = State.CONSONANT_VOWEL_CONSONANT
                ACTION_UPDATE_COMPOSITE
            } else {
                completeString = compositeString
                compositeString = word.toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            }
        }

        val code = getVowel(compositeString[0])
        val newWord = combineVowelWithWord(code, word)
        return if (newWord != 0.toChar()) {
            val firstConsonantIndex = getFirstConsonantIndex(compositeString[0])
            val vowelIndex = convertVowelToIndex(newWord)
            completeString = ""
            compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
            state = State.CONSONANT_COMPOUND_VOWEL
            ACTION_UPDATE_COMPOSITE
        } else {
            completeString = compositeString
            compositeString = word.toString()
            state = State.VOWEL
            ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }
    }

    /**
     * 조합: single Consonant + single Vowel + single Consonant
     * 예시: 각
     */
    private fun forConsonantVowelConsonant(word: Char): Int {
        Log.v(TAG, "-forConsonantVowelConsonant: 0. word=$word, compositeString=$compositeString")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            val index = getLastConsonantIndex(compositeString[0])
            if (index < 0) {
                return ACTION_ERROR
            }

            val newWord = combineLastConsonantWithWord(LastConsonant.Word[index], word)
            return if (newWord != 0.toChar()) {
                completeString = ""
                compositeString = (compositeString[0].code - index + getLastConsonantIndex(newWord)).toChar().toString()
                state = State.CONSONANT_COMPOUND_VOWEL_CONSONANT
                ACTION_UPDATE_COMPOSITE
            } else {
                completeString = compositeString
                compositeString = word.toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            }
        }

        val lastConsonantIndex = getLastConsonantIndex(compositeString[0])
        if (lastConsonantIndex < 0) {
            return ACTION_ERROR
        }

        completeString = (compositeString[0].code - lastConsonantIndex).toChar().toString()
        val firstConsonantIndex = getFirstConsonantIndex(LastConsonant.Word[lastConsonantIndex])
        if (firstConsonantIndex < 0) {
            return ACTION_ERROR
        }

        val vowelIndex = getVowelIndex(word)
        compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
        state = State.CONSONANT_VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: single Consonant + single Vowel + compound Consonant
     * 예시: 갃
     */
    private fun forConsonantVowelCompoundConsonant(word: Char): Int {
        Log.v(TAG, "-forConsonantVowelCompoundConsonant: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            val index = getLastConsonantIndex(compositeString[0])
            if (index < 0) {
                return ACTION_ERROR
            }

            val newWord = combineLastConsonantWithWord(LastConsonant.Word[index], word)
            return if (newWord != 0.toChar()) {
                completeString = ""
                compositeString = (compositeString[0].code - index + getLastConsonantIndex(newWord)).toChar().toString()
                state = State.CONSONANT_VOWEL_COMPOUND_CONSONANT
                ACTION_UPDATE_COMPOSITE
            } else {
                completeString = compositeString
                compositeString = word.toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            }
        }

        val lastConsonantIndex = getLastConsonantIndex(compositeString[0])
        if (lastConsonantIndex < 0) {
            return ACTION_ERROR
        }

        completeString = (compositeString[0].code - lastConsonantIndex).toChar().toString()
        val firstConsonantIndex = getFirstConsonantIndex(LastConsonant.Word[lastConsonantIndex])
        if (firstConsonantIndex < 0) {
            return ACTION_ERROR
        }

        val vowelIndex = getVowelIndex(word)
        compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
        state = State.CONSONANT_VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: single Consonant + compound Vowel
     * 예시: 과
     */
    private fun forConsonantCompoundVowel(word: Char): Int {
        Log.v(TAG, "-forConsonantCompoundVowel: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            val index = getLastConsonantIndex(word)
            return if (index != -1) {
                completeString = ""
                compositeString = (compositeString[0].code + index).toChar().toString()
                state = State.CONSONANT_COMPOUND_VOWEL_CONSONANT
                ACTION_UPDATE_COMPOSITE
            } else {
                completeString = compositeString
                compositeString = word.toString()
                state = State.CONSONANT
                ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
            }
        }

        completeString = compositeString
        compositeString = word.toString()
        state = State.VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: single Consonant + compound Vowel + single Consonant
     * 예시: 곽
     */
    private fun forConsonantCompoundVowelLastConsonant(word: Char): Int {
        Log.v(TAG, "-forConsonantCompoundVowelLastConsonant: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            completeString = compositeString
            compositeString = word.toString()
            state = State.CONSONANT
            return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }

        val tempChar = compositeString[0]
        val lastConsonantIndex0 = getLastConsonantIndex(tempChar)
        val lastConsonantIndex1 = LastConsonant.Last[lastConsonantIndex0]
        val firstConsonantIndex = LastConsonant.First[lastConsonantIndex0]
        val vowelIndex = getVowelIndex(word)
        completeString = (tempChar.code - lastConsonantIndex0 + lastConsonantIndex1).toChar().toString()
        compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
        state = State.CONSONANT_VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

    /**
     * 조합: single Consonant + compound Vowel + compound Consonant
     * 예시: 곿
     */
    private fun forConsonantCompoundVowelCompoundConsonant(word: Char): Int {
        Log.v(TAG, "-forConsonantCompoundVowelCompoundConsonant: 0. word=$word")

        if (compositeString.isEmpty()) {
            return ACTION_ERROR
        }

        if (isConsonant(word)) {
            completeString = compositeString
            compositeString = word.toString()
            state = State.CONSONANT
            return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
        }

        val tempChar = compositeString[0]
        val lastConsonantIndex0 = getLastConsonantIndex(tempChar)
        val lastConsonantIndex1 = LastConsonant.Last[lastConsonantIndex0]
        val firstConsonantIndex = LastConsonant.First[lastConsonantIndex0]
        val vowelIndex = getVowelIndex(word)
        completeString = (tempChar.code - lastConsonantIndex0 + lastConsonantIndex1).toChar().toString()
        compositeString = composeWordWithIndexes(firstConsonantIndex, vowelIndex, 0).toString()
        state = State.CONSONANT_VOWEL
        return ACTION_UPDATE_COMPLETE or ACTION_UPDATE_COMPOSITE
    }

}