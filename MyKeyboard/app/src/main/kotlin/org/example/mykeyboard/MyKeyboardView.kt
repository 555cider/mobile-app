package org.example.mykeyboard

import android.content.Context
import android.graphics.Paint
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat

class MyKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleRes: Int = 0
) : KeyboardView(context, attrs) {

    companion object {
        const val TAG = "MyKeyboardView"
    }

    private var mLabelTextSize = 0
    private var mKeyTextSize = 0
    private var mTextColor = 0
    private var mBackgroundColor = 0

    private var mPreviewText: TextView? = null
    private val mPopupPreview: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewHeight = 0

    private val mPopupKeyboard: PopupWindow
    private var mPopupParent: View

    private var mVerticalCorrection = 0

    private val mPaint: Paint
    private var mPopupLayout = 0
    private var mPopupMaxMoveDistance = 0f
    private val mSpaceMoveThreshold: Int

    /** The accessibility manager for accessibility support  */
    private val mAccessibilityManager: AccessibilityManager

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.MyKeyboardView, 0, defStyleRes)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val keyTextSize = 0
        try {
            for (i in 0 until attributes.indexCount) {
                Log.v(TAG, attributes.getIndex(i).toString())
                when (val attr = attributes.getIndex(i)) {
                    R.styleable.MyKeyboardView_keyTextSize -> mKeyTextSize = attributes.getDimensionPixelSize(attr, 18)
                }
            }
        } finally {
            attributes.recycle()
        }

        mPopupLayout = R.layout.keyboard_view_popup
        mVerticalCorrection = resources.getDimension(R.dimen.vertical_correction).toInt()
        mLabelTextSize = resources.getDimension(R.dimen.label_text_size).toInt()
        mPreviewHeight = resources.getDimension(R.dimen.key_height).toInt()
        mSpaceMoveThreshold = resources.getDimension(R.dimen.medium_margin).toInt()
        mTextColor = ContextCompat.getColor(context, R.color.black)
        mBackgroundColor = ContextCompat.getColor(context, R.color.black)

        mPopupPreview = PopupWindow(context)
        mPreviewText = inflater.inflate(resources.getLayout(R.layout.keyboard_key_preview), null) as TextView
        mPreviewTextSizeLarge = context.resources.getDimension(R.dimen.preview_text_size).toInt()
        mPopupPreview.contentView = mPreviewText
        mPopupPreview.setBackgroundDrawable(null)

        mPopupPreview.isTouchable = false
        mPopupKeyboard = PopupWindow(context)
        mPopupKeyboard.setBackgroundDrawable(null)
        mPopupParent = this
        mPaint = Paint().apply {
            isAntiAlias = true
            textSize = mKeyTextSize.toFloat()
            textAlign = Paint.Align.CENTER
            alpha = 255
        }
        mAccessibilityManager = (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
        mPopupMaxMoveDistance = resources.getDimension(R.dimen.popup_max_move_distance)
    }

}