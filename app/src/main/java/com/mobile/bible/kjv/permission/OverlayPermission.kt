package com.mobile.bible.kjv.permission

import android.content.Context
import android.os.Build
import android.provider.Settings

object OverlayPermission {
    fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}