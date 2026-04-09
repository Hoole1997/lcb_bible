package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PrayerVisibleBottomDialog : BottomSheetDialogFragment() {

    private var selectedScope: String = SCOPE_ANYONE

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedScope = arguments?.getString(ARG_SELECTED_SCOPE, SCOPE_ANYONE) ?: SCOPE_ANYONE
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_prayer_visible_bottom_sheet, container, false)
        val optionAnyone = view.findViewById<TextView>(R.id.option_anyone)
        val optionAnyoneCheck = view.findViewById<ImageView>(R.id.option_anyone_check)
        val optionJustMe = view.findViewById<TextView>(R.id.option_just_me)
        val optionJustMeCheck = view.findViewById<ImageView>(R.id.option_just_me_check)
        val cancel = view.findViewById<TextView>(R.id.button_cancel)
        val ok = view.findViewById<TextView>(R.id.button_ok)

        fun renderScope() {
            val anyoneSelected = selectedScope == SCOPE_ANYONE
            optionAnyone.setBackgroundResource(
                if (anyoneSelected) R.drawable.bg_prayer_visible_option_selected
                else R.drawable.bg_prayer_visible_option_normal
            )
            optionAnyone.setTextColor(
                if (anyoneSelected) Color.parseColor("#DCB888")
                else Color.parseColor("#666666")
            )
            optionAnyoneCheck.visibility = if (anyoneSelected) View.VISIBLE else View.GONE

            optionJustMe.setBackgroundResource(
                if (anyoneSelected) R.drawable.bg_prayer_visible_option_normal
                else R.drawable.bg_prayer_visible_option_selected
            )
            optionJustMe.setTextColor(
                if (anyoneSelected) Color.parseColor("#666666")
                else Color.parseColor("#DCB888")
            )
            optionJustMeCheck.visibility = if (anyoneSelected) View.GONE else View.VISIBLE
        }

        optionAnyone.setOnClickListener {
            selectedScope = SCOPE_ANYONE
            renderScope()
        }
        optionJustMe.setOnClickListener {
            selectedScope = SCOPE_JUST_ME
            renderScope()
        }
        cancel.setOnClickListener { dismiss() }
        ok.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_SELECTED_SCOPE to selectedScope)
            )
            dismiss()
        }

        renderScope()
        return view
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        dialog.dismissWithAnimation = true
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        val behavior = dialog.behavior
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        })
    }

    companion object {
        const val REQUEST_KEY = "prayer_visible_result"
        const val RESULT_SELECTED_SCOPE = "selected_scope"
        const val SCOPE_ANYONE = "anyone"
        const val SCOPE_JUST_ME = "just_me"
        private const val ARG_SELECTED_SCOPE = "arg_selected_scope"

        fun show(activity: FragmentActivity, selectedScope: String) {
            PrayerVisibleBottomDialog().apply {
                arguments = bundleOf(ARG_SELECTED_SCOPE to selectedScope)
            }.show(activity.supportFragmentManager, "PrayerVisibleBottomDialog")
        }
    }
}
