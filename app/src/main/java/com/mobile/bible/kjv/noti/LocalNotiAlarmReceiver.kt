package com.mobile.bible.kjv.noti

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LocalNotiAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LocalNotificationService.onAlarmTriggered(
            context = context,
            slot = intent?.getStringExtra(LocalNotificationService.EXTRA_SLOT)
        )
    }
}
