package com.mobile.bible.kjv.ui.dialog

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import net.corekit.core.report.ReportDataManager

class DeletePrayerDialog : DialogFragment() {
    private var didClickOk = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_delete_prayer, container, false)
        val buttonCancel = view.findViewById<View>(R.id.button_cancel)
        val buttonOk = view.findViewById<View>(R.id.button_ok)

        buttonCancel.setOnClickListener { dismiss() }
        buttonOk.setOnClickListener {
            didClickOk = true
            ReportDataManager.reportData("PostDelete_Click", mapOf("result" to "ok"))
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_CONFIRMED to true)
            )
            dismiss()
        }
        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!didClickOk) {
            ReportDataManager.reportData("PostDelete_Click", mapOf("result" to "cancel"))
        }
        super.onDismiss(dialog)
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
        const val REQUEST_KEY = "delete_prayer_dialog_result"
        const val RESULT_CONFIRMED = "confirmed"

        fun show(activity: FragmentActivity) {
            DeletePrayerDialog().show(activity.supportFragmentManager, "DeletePrayerDialog")
        }
    }
}
