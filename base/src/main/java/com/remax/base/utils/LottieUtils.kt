package com.remax.base.utils

import android.content.Context
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieTask

/**
 * Lottie动画工具类
 */
object LottieUtils {

    /**
     * 从资源文件加载Lottie动画
     * @param context 上下文
     * @param rawResId 资源ID
     * @param callback 加载回调
     */
    fun loadAnimationFromRaw(
        context: Context,
        rawResId: Int,
        callback: (LottieComposition?) -> Unit
    ) {
        LottieCompositionFactory.fromRawRes(context, rawResId)
            .addListener { composition ->
                callback(composition)
            }
            .addFailureListener { exception ->
                callback(null)
            }
    }

    /**
     * 从Assets文件夹加载Lottie动画
     * @param context 上下文
     * @param fileName 文件名
     * @param callback 加载回调
     */
    fun loadAnimationFromAssets(
        context: Context,
        fileName: String,
        callback: (LottieComposition?) -> Unit
    ) {
        LottieCompositionFactory.fromAsset(context, fileName)
            .addListener { composition ->
                callback(composition)
            }
            .addFailureListener { exception ->
                callback(null)
            }
    }

    /**
     * 从网络URL加载Lottie动画
     * @param context 上下文
     * @param url 动画URL
     * @param callback 加载回调
     */
    fun loadAnimationFromUrl(
        context: Context,
        url: String,
        callback: (LottieComposition?) -> Unit
    ) {
        LottieCompositionFactory.fromUrl(context, url)
            .addListener { composition ->
                callback(composition)
            }
            .addFailureListener { exception ->
                callback(null)
            }
    }

    /**
     * 设置LottieAnimationView的动画
     * @param lottieView Lottie动画视图
     * @param rawResId 资源ID
     * @param autoPlay 是否自动播放
     * @param repeatCount 重复次数，-1为无限循环
     */
    fun setAnimation(
        lottieView: LottieAnimationView,
        rawResId: Int,
        autoPlay: Boolean = true,
        repeatCount: Int = -1
    ) {
        lottieView.setAnimation(rawResId)
        lottieView.repeatCount = repeatCount
        if (autoPlay) {
            lottieView.playAnimation()
        }
    }

    /**
     * 设置LottieAnimationView的动画（从Assets）
     * @param lottieView Lottie动画视图
     * @param fileName 文件名
     * @param autoPlay 是否自动播放
     * @param repeatCount 重复次数，-1为无限循环
     */
    fun setAnimationFromAssets(
        lottieView: LottieAnimationView,
        fileName: String,
        autoPlay: Boolean = true,
        repeatCount: Int = -1
    ) {
        lottieView.setAnimation(fileName)
        lottieView.repeatCount = repeatCount
        if (autoPlay) {
            lottieView.playAnimation()
        }
    }

    /**
     * 设置LottieAnimationView的动画（从URL）
     * @param lottieView Lottie动画视图
     * @param url 动画URL
     * @param autoPlay 是否自动播放
     * @param repeatCount 重复次数，-1为无限循环
     */
    fun setAnimationFromUrl(
        lottieView: LottieAnimationView,
        url: String,
        autoPlay: Boolean = true,
        repeatCount: Int = -1
    ) {
        lottieView.setAnimationFromUrl(url)
        lottieView.repeatCount = repeatCount
        if (autoPlay) {
            lottieView.playAnimation()
        }
    }

    /**
     * 创建加载中的Lottie动画视图
     * @param context 上下文
     * @param rawResId 资源ID
     * @return LottieAnimationView
     */
    fun createLoadingView(
        context: Context,
        rawResId: Int
    ): LottieAnimationView {
        return LottieAnimationView(context).apply {
            setAnimation(rawResId)
            repeatCount = -1
            playAnimation()
        }
    }

    /**
     * 显示/隐藏Lottie动画
     * @param lottieView Lottie动画视图
     * @param show 是否显示
     */
    fun showLottieAnimation(lottieView: LottieAnimationView, show: Boolean) {
        if (show) {
            lottieView.visibility = View.VISIBLE
            if (!lottieView.isAnimating) {
                lottieView.playAnimation()
            }
        } else {
            lottieView.visibility = View.GONE
            lottieView.pauseAnimation()
        }
    }
} 