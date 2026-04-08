package com.example.base.extention

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.annotation.ColorInt

/**
 * WindowInsets相关扩展函数
 * 用于处理Android 15边到边显示的适配
 */

/**
 * WindowInsets缓存单例
 * 避免重复设置监听器，提高性能
 */
object WindowInsetsCache {
    private var cachedSystemBars: Insets? = null
    private var isInitialized = false
    private val pendingActions = mutableListOf<(Insets) -> Unit>()

    fun getSystemBars(): Insets? = cachedSystemBars

    fun initIfNeeded(view: View, onReady: ((Insets) -> Unit)? = null) {
        if (!isInitialized) {
            // 找到根View来设置监听器
            val rootView = view.rootView
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
                cachedSystemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                isInitialized = true

                // 执行所有等待的操作
                executePendingActions()

                windowInsets
            }
            ViewCompat.requestApplyInsets(rootView)
        }

        // 处理回调
        if (cachedSystemBars != null && onReady != null) {
            // 已经有缓存值，立即执行
            onReady(cachedSystemBars!!)
        } else if (onReady != null) {
            // 还没有缓存值，加入等待列表
            pendingActions.add(onReady)
        }
    }

    private fun executePendingActions() {
        cachedSystemBars?.let { systemBars ->
            pendingActions.forEach { action ->
                action(systemBars)
            }
            pendingActions.clear()
        }
    }

    /**
     * 重置缓存（主要用于测试或特殊情况）
     */
    fun reset() {
        cachedSystemBars = null
        isInitialized = false
        pendingActions.clear()
    }
}

/**
 * 为View设置顶部边距以适配状态栏
 * 直接使用缓存版本实现
 */
fun View.appendStatusBarMarginTop() {
    WindowInsetsCache.initIfNeeded(this) { systemBars ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += systemBars.top
        }
    }
}

/**
 * 为View添加底部边距，避免被导航栏遮挡
 * 用于确保内容不延伸到导航栏下方
 */
fun View.appendNavigationBarMarginBottom() {
    WindowInsetsCache.initIfNeeded(this) { systemBars ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin += systemBars.bottom
        }
    }
}

/**
 * 为View设置顶部Padding以适配状态栏
 */
fun View.appendStatusBarPaddingTop() {
    WindowInsetsCache.initIfNeeded(this) { systemBars ->
        setPadding(paddingLeft, paddingTop + systemBars.top, paddingRight, paddingBottom)
    }
}

/**
 * 为View设置底部Padding以适配导航栏
 * 用于确保内容不被导航栏遮挡
 */
fun View.appendNavigationBarPaddingBottom() {
    WindowInsetsCache.initIfNeeded(this) { systemBars ->
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + systemBars.bottom)
    }
}

fun View.appendNavigationBarPaddingVertical() {
    WindowInsetsCache.initIfNeeded(this) { systemBars ->
        setPadding(
            paddingLeft,
            paddingTop + systemBars.top,
            paddingRight,
            paddingBottom + systemBars.bottom
        )
    }
}

/**
 * 设置状态栏外观
 * @param isDarkFont true=暗色字体(亮色背景), false=亮色字体(暗色背景)
 */
fun Activity.setStatusBarAppearance(isDarkFont: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.isAppearanceLightStatusBars = isDarkFont
}

/**
 * 现代方式：使用WindowInsetsController设置导航栏样式
 * 推荐使用此方法替代直接设置颜色的过时API
 * @param isLightBackground true=亮色背景(深色按钮), false=暗色背景(白色按钮)
 * @param enforceContrast 是否强制系统对比度
 */
fun Activity.setNavigationBarAppearance(
    isLightBackground: Boolean = false,
    enforceContrast: Boolean = true
) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.isAppearanceLightNavigationBars = isLightBackground

    // 设置对比度强制策略
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = enforceContrast
    }
}

/**
 * 设置导航栏背景颜色 (现代API版本)
 * 适用于边到边显示模式下的导航栏颜色自定义
 * @param color 导航栏背景颜色，支持透明度
 * @param lightButtons true=深色按钮(亮色背景), false=白色按钮(暗色背景)
 */
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    // 使用现代API设置导航栏颜色
    window.apply {
        // 确保设置了正确的标志
        addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // 设置颜色（尽管已废弃，但仍需要用于兼容性）
        @Suppress("DEPRECATION")
        navigationBarColor = color
    }
}

/**
 * 设置状态栏背景颜色 (现代API版本)
 * 适用于边到边显示模式下的状态栏颜色自定义
 * @param color 状态栏背景颜色，支持透明度
 */
fun Activity.setStatusBarColor(@ColorInt color: Int) {
    // 使用现代API设置状态栏颜色
    window.apply {
        // 确保设置了正确的标志
        addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // 设置颜色（尽管已废弃，但仍需要用于兼容性）
        @Suppress("DEPRECATION")
        statusBarColor = color
    }
}

/**
 * 启用边到边显示并设置默认样式
 * 这是推荐的现代方式，替代直接设置导航栏颜色的过时API
 */
fun ComponentActivity.edgeToEdge() {
    enableEdgeToEdge()// 状态栏、导航栏显示内容，并且透明
    setStatusBarAppearance(isDarkFont = true)
    setNavigationBarColor(Color.WHITE)
    setNavigationBarAppearance(isLightBackground = true, enforceContrast = true)
}

/**
 * 让 View 忽略系统 WindowInsets（不自动适配系统栏的 padding）
 */
fun View.ignoreWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        insets
    }
}