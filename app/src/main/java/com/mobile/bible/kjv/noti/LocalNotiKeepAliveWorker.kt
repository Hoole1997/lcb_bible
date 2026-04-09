package com.mobile.bible.kjv.noti

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class LocalNotiKeepAliveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        LocalNotificationService.onKeepAliveTick(applicationContext)
        return Result.success()
    }
}
