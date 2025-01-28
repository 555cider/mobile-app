package org.example.mykeyboard

import android.content.Context
import android.content.res.Resources
import android.inputmethodservice.Keyboard
import android.view.inputmethod.EditorInfo

class MyKeyboard(context: Context?, xmlLayoutResId: Int) : Keyboard(context, xmlLayoutResId) {

    private var mEnterKey: Key? = null

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    fun setImeOptions(res: Resources, options: Int) {
        if (mEnterKey == null) {
            return
        }
        when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_GO -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_go_key)
            }

            EditorInfo.IME_ACTION_NEXT -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_next_key)
            }

            EditorInfo.IME_ACTION_SEARCH -> {
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_key_search, null)
                mEnterKey!!.label = null
            }

            EditorInfo.IME_ACTION_SEND -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_send_key)
            }

            else -> {
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_key_return, null)
                mEnterKey!!.label = null
            }
        }
    }

}