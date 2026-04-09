package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import java.util.Locale
import androidx.core.graphics.toColorInt

class AnswerRecoveryDialog : DialogFragment() {

    private var timerView: TextView? = null
    private var countDownTimer: CountDownTimer? = null
    private var callback: OnTimeoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_answer_recovery, container, false)
        val recoveryButton = view.findViewById<View>(R.id.button_recovery)
        val noThanks = view.findViewById<View>(R.id.button_no_thanks)
        timerView = view.findViewById(R.id.text_timer)
        recoveryButton.setOnClickListener { dismiss() }
        noThanks.setOnClickListener { dismiss() }
        startCountdown()
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

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        timerView = null
        callback = null
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        updateTimerText(10)
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                if (seconds >= 1) {
                    updateTimerText(seconds)
                } else if (seconds == 0) {
                    updateTimerText(0)
                    callback?.onRecoveryTimeout()
                    cancel()
                    dismiss()
                }
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun updateTimerText(seconds: Int) {
        val view = timerView ?: return
        val numberString = String.format(Locale.getDefault(), "%02d", seconds)
        val text = "$numberString s"
        val spannable = SpannableString(text)
        val numberEnd = numberString.length
        spannable.setSpan(
            ForegroundColorSpan("#f4241e".toColorInt()),
            0,
            numberEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            numberEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            AbsoluteSizeSpan(32, true),
            0,
            numberEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#85633e")),
            numberEnd,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.NORMAL),
            numberEnd,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            AbsoluteSizeSpan(18, true),
            numberEnd,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        view.text = spannable
    }

    companion object {
        fun show(activity: FragmentActivity) {
            AnswerRecoveryDialog().show(activity.supportFragmentManager, "AnswerRecoveryDialog")
        }

        fun show(activity: FragmentActivity, callback: OnTimeoutListener) {
            val dialog = AnswerRecoveryDialog()
            dialog.callback = callback
            dialog.show(activity.supportFragmentManager, "AnswerRecoveryDialog")
        }
    }

    interface OnTimeoutListener {
        fun onRecoveryTimeout()
    }
}
