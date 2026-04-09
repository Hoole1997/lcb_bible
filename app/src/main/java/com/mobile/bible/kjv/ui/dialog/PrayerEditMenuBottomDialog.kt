package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PrayerEditMenuBottomDialog : BottomSheetDialogFragment() {

    private var pinText: String = ""
    private var visibleText: String = ""

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinText = arguments?.getString(ARG_PIN_TEXT).orEmpty()
        visibleText = arguments?.getString(ARG_VISIBLE_TEXT).orEmpty()
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_prayer_edit_menu_bottom_sheet, container, false)
        val rowSticky = view.findViewById<View>(R.id.row_sticky)
        val rowVisible = view.findViewById<View>(R.id.row_visible)
        val rowDelete = view.findViewById<View>(R.id.row_delete)
        val buttonCancel = view.findViewById<View>(R.id.button_cancel)
        val stickyValue = view.findViewById<TextView>(R.id.text_sticky_value)
        val visibleValue = view.findViewById<TextView>(R.id.text_visible_value)

        if (pinText.isNotBlank()) {
            stickyValue.text = pinText
        }
        if (visibleText.isNotBlank()) {
            visibleValue.text = visibleText
        }

        rowSticky.setOnClickListener {
            sendActionAndDismiss(ACTION_STICKY_POST)
        }
        rowVisible.setOnClickListener {
            val hostActivity = activity as? FragmentActivity ?: return@setOnClickListener
            val selectedScope = resolveSelectedScope()
            dismiss()
            hostActivity.window.decorView.post {
                PrayerVisibleBottomDialog.show(hostActivity, selectedScope)
            }
        }
        rowDelete.setOnClickListener {
            sendActionAndDismiss(ACTION_DELETE)
        }
        buttonCancel.setOnClickListener { dismiss() }
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

    private fun sendActionAndDismiss(action: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_ACTION to action)
        )
        dismiss()
    }

    private fun resolveSelectedScope(): String {
        val current = if (visibleText.isNotBlank()) visibleText else getString(R.string.prayer_edit_menu_visible_default)
        val justMe = getString(R.string.prayer_wall_edit_visible_just_me)
        return if (current.equals(justMe, ignoreCase = true)) {
            PrayerVisibleBottomDialog.SCOPE_JUST_ME
        } else {
            PrayerVisibleBottomDialog.SCOPE_ANYONE
        }
    }

    companion object {
        const val REQUEST_KEY = "prayer_edit_menu_result"
        const val RESULT_ACTION = "action"

        const val ACTION_STICKY_POST = "sticky_post"
        const val ACTION_VISIBLE_TO = "visible_to"
        const val ACTION_DELETE = "delete"

        private const val ARG_PIN_TEXT = "arg_pin_text"
        private const val ARG_VISIBLE_TEXT = "arg_visible_text"

        fun show(
            activity: FragmentActivity,
            pinText: String? = null,
            visibleText: String? = null
        ) {
            PrayerEditMenuBottomDialog().apply {
                arguments = bundleOf(
                    ARG_PIN_TEXT to pinText,
                    ARG_VISIBLE_TEXT to visibleText
                )
            }.show(activity.supportFragmentManager, "PrayerEditMenuBottomDialog")
        }
    }
}
