package com.mobile.bible.kjv.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object NotificationPermission {
    interface Callback {
        fun onGranted()
        fun onDenied()
        fun onDeniedPermanently()
    }

    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun request(activity: FragmentActivity, callback: Callback) {
        if (isGranted(activity)) {
            callback.onGranted()
            return
        }
        val tag = "NotificationPermissionRequester"
        val fm = activity.supportFragmentManager
        val existing = fm.findFragmentByTag(tag) as? RequestFragment
        val fragment = existing ?: RequestFragment()
        fragment.callback = callback
        if (existing == null) {
            fm.beginTransaction().add(fragment, tag).commitNow()
        }
        fragment.launch()
    }

    class RequestFragment : Fragment() {
        var callback: Callback? = null
        private lateinit var launcher: ActivityResultLauncher<String>

        override fun onCreate(savedInstanceState: android.os.Bundle?) {
            super.onCreate(savedInstanceState)
            launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val cb = callback
                val act = activity
                val permanentlyDenied = if (act != null) {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        act,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else false
                if (granted) {
                    cb?.onGranted()
                } else if (permanentlyDenied) {
                    cb?.onDeniedPermanently()
                } else {
                    cb?.onDenied()
                }
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            }
        }

        fun launch() {
            if (Build.VERSION.SDK_INT < 33) {
                callback?.onGranted()
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}