package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R

class AnswerFailedDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_answer_failed, container, false)
        val cancel = view.findViewById<ImageView>(R.id.button_cancel)
        val rewardButton = view.findViewById<View>(R.id.button_try_again_reward)
        cancel.setOnClickListener { dismiss() }
        rewardButton.setOnClickListener { dismiss() }
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val metrics = resources.displayMetrics
            val marginPx = (30 * metrics.density).toInt()
            val width = metrics.widthPixels - marginPx * 2
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun show(activity: FragmentActivity) {
            AnswerFailedDialog().show(activity.supportFragmentManager, "AnswerFailedDialog")
        }
    }
}
