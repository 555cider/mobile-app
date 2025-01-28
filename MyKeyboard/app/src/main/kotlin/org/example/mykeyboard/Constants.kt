package org.example.mykeyboard

// Action Codes
const val ACTION_ERROR = -1
const val ACTION_NONE = 0
const val ACTION_UPDATE_COMPOSITE = 1
const val ACTION_UPDATE_COMPLETE = 2
const val ACTION_USE_INPUT_AS_RESULT = 4
const val ACTION_APPEND = 8
const val ACTION_DOWN = 0

// keyCode
const val KEYCODE_ALT = -6
const val KEYCODE_DELETE = -5
const val KEYCODE_DONE = -4
const val KEYCODE_CANCEL = -3
const val KEYCODE_MODE_CHANGE = -2
const val KEYCODE_SHIFT = -1
const val KEYCODE_HOME = 3
const val KEYCODE_BACK = 4
const val KEYCODE_DPAD_UP = 19
const val KEYCODE_DPAD_DOWN = 20
const val KEYCODE_DPAD_LEFT = 21
const val KEYCODE_DPAD_RIGHT = 22
const val KEYCODE_SPACE = 62
const val KEYCODE_ENTER = 66
const val KEYCODE_DEL = 67 // Backspace
const val KEYCODE_HANGUL = 218

// num
const val NUM_OF_FIRST = 19
const val NUM_OF_MIDDLE = 21
const val NUM_OF_LAST = 27
const val NUM_OF_LAST_INDEX = NUM_OF_LAST + 1 // add 1 for non-last consonant added characters

// keyState
const val KEYSTATE_NONE = 0
const val KEYSTATE_SHIFT = 1
const val KEYSTATE_SHIFT_MASK = 3

// Korean
const val HANGUL_START = 0xAC00 // 가
const val HANGUL_END = 0xD7A3 // 힣
const val HANGUL_JAMO_START = 0x3131 // ㄱ
const val HANGUL_MO_START = 0x314F // ㅏ
const val HANGUL_JAMO_END = 0x3163 // ㅣ

object Constants {
    object Automata {
        enum class State {
            INITIAL,
            CONSONANT,
            COMPOUND_CONSONANT,
            VOWEL,
            COMPOUND_VOWEL,
            CONSONANT_VOWEL,
            CONSONANT_VOWEL_CONSONANT,
            CONSONANT_VOWEL_COMPOUND_CONSONANT,
            CONSONANT_COMPOUND_VOWEL,
            CONSONANT_COMPOUND_VOWEL_CONSONANT,
            CONSONANT_COMPOUND_VOWEL_COMPOUND_CONSONANT
        }
    }
}