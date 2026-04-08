package com.remax.base.ext

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.remax.base.utils.ActivityLauncher
import com.remax.base.controller.SystemPageNavigationController

fun Context.checkStorePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val readPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }
}

fun Context.requestStorePermission(
    launcher: ActivityLauncher,
    jumpAction: (() -> Unit)? = null,
    result: (flag: Boolean) -> Unit
) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // 标记进入系统页面
        SystemPageNavigationController.markEnterStorageAccessPage()
        
        val intent =
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.addCategory("android.intent.category.DEFAULT")
        intent.data = Uri.parse("package:${packageName}")
        jumpAction?.invoke()
        launcher.launch(intent) {
            // 权限检查完成后，标记离开系统页面
            SystemPageNavigationController.markLeaveSystemPage()
            result.invoke(checkStorePermission())
        }
    } else {
        launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) { map ->
            result(map.values.all { it })
        }
    }

}