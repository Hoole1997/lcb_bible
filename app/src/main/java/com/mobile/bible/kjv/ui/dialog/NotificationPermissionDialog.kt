package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.mobile.bible.kjv.R

class NotificationPermissionDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_notification_permission, container, false)
        val open = view.findViewById<TextView>(R.id.btn_open)
        open.setOnClickListener { dismiss() }
        val bless = view.findViewById<ImageView>(R.id.image_bless)
        val d = bless.resources.displayMetrics.density
        val r = 16f * d
        Glide.with(this)
            .load(R.mipmap.img_bless_b)
            .transform(CenterCrop(), GranularRoundedCorners(r, r, 0f, 0f))
            .into(bless)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    companion object {
        fun show(activity: FragmentActivity) {
            NotificationPermissionDialog().show(activity.supportFragmentManager, "NotificationPermissionDialog")
        }
    }
}
