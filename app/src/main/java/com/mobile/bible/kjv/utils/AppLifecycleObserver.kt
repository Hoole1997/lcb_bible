package com.mobile.bible.kjv.utils

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.mobile.bible.kjv.noti.LocalNotificationService

object AppLifecycleObserver : Application.ActivityLifecycleCallbacks {

    private var startedActivityCount = 0
    @Volatile
    private var appInForeground = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var backgroundCheckRunnable: Runnable? = null

    fun isAppInForeground(): Boolean = appInForeground

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        appInForeground = startedActivityCount > 0
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount < 0) {
            startedActivityCount = 0
        }
        appInForeground = startedActivityCount > 0
        if (appInForeground || activity.isChangingConfigurations) {
            backgroundCheckRunnable?.let { mainHandler.removeCallbacks(it) }
            return
        }
        backgroundCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        backgroundCheckRunnable = Runnable {
            if (!appInForeground) {
                LocalNotificationService.onAppMovedToBackground(activity.applicationContext)
            }
        }.also {
            // Delay briefly to avoid false background during page transitions.
            mainHandler.postDelayed(it, 300L)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

