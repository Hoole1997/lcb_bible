package com.mobile.bible.kjv.ui.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.ui.activity.GuideFloatingWindowActivity
import com.mobile.bible.kjv.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.net.toUri

class FloatingWindowBottomDialog : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_float_window_bottom_sheet, container, false)
        val cancel = view.findViewById<ImageView>(R.id.button_cancel)
        val open = view.findViewById<TextView>(R.id.btn_open)
        cancel.setOnClickListener { dismiss() }
        open.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${open.context.packageName}".toUri()
            )
            startActivity(intent)
            Handler(Looper.getMainLooper()).postDelayed({
                val overlay = Intent(open.context, GuideFloatingWindowActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                startActivity(overlay)
            }, 100)
            dismiss()
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        d.dismissWithAnimation = true
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(Color.TRANSPARENT)
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

    companion object {
        fun show(activity: FragmentActivity) {
            FloatingWindowBottomDialog().show(activity.supportFragmentManager, "BlessBottomSheet")
        }
    }
}
