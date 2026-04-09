package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.content.DialogInterface
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.prefs.AudioSettings
import com.mobile.bible.kjv.R

class AnswerPauseDialog : DialogFragment() {

    private var listener: OnActionListener? = null
    private var musicIconView: ImageView? = null
    private var soundIconView: ImageView? = null
    private var isQuitting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_answer_pause, container, false)
        val cancel = view.findViewById<ImageView>(R.id.button_cancel)
        val music = view.findViewById<View>(R.id.button_music)
        val sound = view.findViewById<View>(R.id.button_sound)
        val quit = view.findViewById<View>(R.id.button_quit)
        val resume = view.findViewById<View>(R.id.button_resume)
        
        musicIconView = view.findViewById(R.id.svg_music)
        soundIconView = view.findViewById(R.id.svg_sound)
        
        // Initialize icons based on persisted settings
        updateMusicIcon()
        updateSoundIcon()

        cancel.setOnClickListener { dismiss() }
        music.setOnClickListener {
            AudioSettings.isMusicEnabled = !AudioSettings.isMusicEnabled
            updateMusicIcon()
            listener?.onMusicClick()
        }
        sound.setOnClickListener {
            AudioSettings.isSoundEnabled = !AudioSettings.isSoundEnabled
            updateSoundIcon()
            listener?.onSoundClick()
        }
        quit.setOnClickListener {
            isQuitting = true
            listener?.onQuitClick()
            dismiss()
        }
        resume.setOnClickListener {
            listener?.onResumeClick()
            dismiss()
        }

        return view
    }
    
    private fun updateMusicIcon() {
        musicIconView?.setImageResource(
            if (AudioSettings.isMusicEnabled) R.drawable.svg_answer_pause_music
            else R.drawable.svg_music_mute
        )
    }
    
    private fun updateSoundIcon() {
        soundIconView?.setImageResource(
            if (AudioSettings.isSoundEnabled) R.drawable.svg_answer_pause_sound
            else R.drawable.svg_sound_mute
        )
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
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // When dialog is dismissed (by clicking outside, back button, or cancel button)
        // resume the countdown unless the user clicked quit
        if (!isQuitting) {
            listener?.onResumeClick()
            listener?.onDialogDismissed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener = null
        musicIconView = null
        soundIconView = null
    }

    fun setOnActionListener(listener: OnActionListener?) {
        this.listener = listener
    }

    interface OnActionListener {
        fun onMusicClick()
        fun onSoundClick()
        fun onQuitClick()
        fun onResumeClick()
        fun onDialogDismissed()
    }

    companion object {
        fun show(activity: FragmentActivity) {
            AnswerPauseDialog().show(activity.supportFragmentManager, "AnswerPauseDialog")
        }

        fun show(activity: FragmentActivity, listener: OnActionListener) {
            val dialog = AnswerPauseDialog()
            dialog.listener = listener
            dialog.show(activity.supportFragmentManager, "AnswerPauseDialog")
        }
    }
}

