package com.remax.base.controller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * System Page Navigation State Controller
 * Used to manage state when returning from system pages, preventing splash screen from launching
 */
object SystemPageNavigationController {
    
    private const val TAG = "SystemPageNavigation"
    
    // Whether currently in system page
    private val isInSystemPage = AtomicBoolean(false)
    
    // Timestamp when entering system page
    private val enterSystemPageTime = AtomicLong(0)
    
    // System page timeout (milliseconds) - 5 minutes
    private const val SYSTEM_PAGE_TIMEOUT = 5 * 60 * 1000L
    
    // System page types
    enum class SystemPageType {
        SETTINGS,           // App settings page
        Notification_PERMISSION,         // Permission settings page
        STORAGE_ACCESS,     // Storage access permission page
        UNKNOWN            // Unknown page
    }
    
    /**
     * Mark entering system page
     */
    fun markEnterSystemPage(type: SystemPageType = SystemPageType.UNKNOWN) {
        isInSystemPage.set(true)
        enterSystemPageTime.set(System.currentTimeMillis())
        Log.d(TAG, "Marked entering system page: $type")
    }
    
    /**
     * Mark leaving system page
     */
    fun markLeaveSystemPage() {
        isInSystemPage.set(false)
        enterSystemPageTime.set(0)
        Log.d(TAG, "Marked leaving system page")
    }
    
    /**
     * Check if currently in system page
     */
    fun isInSystemPage(): Boolean {
        return isInSystemPage.get()
    }
    
    /**
     * Check if splash screen should be started
     * If just returned from system page, don't start
     */
    fun shouldStartSplashActivity(): Boolean {
        val inSystemPage = isInSystemPage.get()
        if (!inSystemPage) {
            return true
        }
        
        // Check if timeout
        val currentTime = System.currentTimeMillis()
        val enterTime = enterSystemPageTime.get()
        if (enterTime > 0 && (currentTime - enterTime) > SYSTEM_PAGE_TIMEOUT) {
            Log.w(TAG, "System page timeout, resetting state")
            markLeaveSystemPage()
            return true
        }
        
        Log.d(TAG, "Just returned from system page, not starting splash screen")
        return false
    }
    
        /**
     * Mark entering storage access permission page (Android 11+)
     * Used for permission requests in StoragePermissionExt
     */
    fun markEnterStorageAccessPage() {
        markEnterSystemPage(SystemPageType.STORAGE_ACCESS)
    }

    fun markEnterNotificationAccessPage() {
        markEnterSystemPage(SystemPageType.Notification_PERMISSION)
    }

    /**
     * Check if splash screen should be started (AppLifecycleObserver specific)
     * If just returned from system page, don't start
     */
    fun shouldStartSplashForAppLifecycle(): Boolean {
        val inSystemPage = isInSystemPage.get()
        if (!inSystemPage) {
            return true
        }
        
        // Check if timeout
        val currentTime = System.currentTimeMillis()
        val enterTime = enterSystemPageTime.get()
        if (enterTime > 0 && (currentTime - enterTime) > SYSTEM_PAGE_TIMEOUT) {
            Log.w(TAG, "System page timeout, resetting state")
            markLeaveSystemPage()
            return true
        }
        
        Log.d(TAG, "Just returned from system page, not starting splash screen")
        return false
    }
} 