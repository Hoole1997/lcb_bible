package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.slider.Slider
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.content.res.ColorStateList
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.CornerFamily
import androidx.core.graphics.drawable.toDrawable
import com.remax.base.ext.KvIntDelegate

class AdjustTextSizeBottomDialog : BottomSheetDialogFragment() {

    companion object {
        private const val REQUEST_KEY = "adjust_text_size"
        private const val RESULT_KEY = "text_size_sp"
        private val TEXT_SIZES = intArrayOf(12, 14, 16, 18, 20, 22, 24)
        private const val DEFAULT_SIZE_SP = 16

        var savedTextSize by KvIntDelegate("verse_text_size_sp", DEFAULT_SIZE_SP)

        fun show(activity: FragmentActivity) {
            AdjustTextSizeBottomDialog().show(activity.supportFragmentManager, "AdjustTextSizeBottomDialog")
        }

        private fun getIndexForSize(sizeSp: Int): Int {
            val index = TEXT_SIZES.indexOf(sizeSp)
            return if (index >= 0) index else TEXT_SIZES.indexOf(DEFAULT_SIZE_SP)
        }
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_adjust_text_size_bottom_sheet, container, false)
        val seekBar = view.findViewById<Slider>(R.id.seekbar)
        val title = view.findViewById<TextView>(R.id.text_title)
        val smallT = view.findViewById<TextView>(R.id.text_small_t)
        val bigT = view.findViewById<TextView>(R.id.text_big_t)

        val savedIndex = getIndexForSize(savedTextSize)

        seekBar.valueFrom = 0f
        seekBar.valueTo = (TEXT_SIZES.size - 1).toFloat()
        seekBar.stepSize = 1f
        seekBar.value = savedIndex.toFloat()
        title.text = resources.getString(R.string.adjust_text_size_title)
        smallT.text = "T"
        bigT.text = "T"

        seekBar.addOnChangeListener { _, value, _ ->
            val textSizeSp = TEXT_SIZES[value.toInt()]
            savedTextSize = textSizeSp
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putInt(RESULT_KEY, textSizeSp)
            })
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        d.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        d.window?.setDimAmount(0.4f)
        d.dismissWithAnimation = true
        val sheet = d.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.setBackgroundColor(Color.TRANSPARENT)
        val behavior = d.behavior
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

}