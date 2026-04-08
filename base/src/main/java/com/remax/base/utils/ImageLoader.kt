package com.remax.base.utils

import android.content.Context
import android.util.TypedValue
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

/**
 * 图片加载工具类
 * 基于Glide封装，提供常用的图片加载方法
 */
object ImageLoader {

    /**
     * 加载图片到ImageView
     * @param context 上下文
     * @param imageView 目标ImageView
     * @param url 图片URL或路径
     * @param placeholder 占位图资源ID
     * @param error 错误图资源ID
     * @param cornerRadius 圆角半径，单位dp，默认0表示无圆角
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        url: String?,
        placeholder: Int = 0,
        error: Int = 0,
        cornerRadius: Float = 0f
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
        
        if (placeholder != 0) {
            requestOptions.placeholder(placeholder)
        }
        if (error != 0) {
            requestOptions.error(error)
        }

        val requestBuilder = Glide.with(context)
            .load(url)
            .apply(requestOptions)

        // 如果设置了圆角，应用圆角变换
        if (cornerRadius > 0) {
            val radiusInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadius,
                context.resources.displayMetrics
            )
            
            requestBuilder.transform(RoundedCornersTransformation(radiusInPixels))
        }

        requestBuilder.into(imageView)
    }

    /**
     * 加载本地文件图片
     * @param context 上下文
     * @param imageView 目标ImageView
     * @param filePath 本地文件路径
     * @param placeholder 占位图资源ID
     * @param error 错误图资源ID
     * @param cornerRadius 圆角半径，单位dp，默认0表示无圆角
     */
    fun loadLocalImage(
        context: Context,
        imageView: ImageView,
        filePath: String?,
        placeholder: Int = 0,
        error: Int = 0,
        cornerRadius: Float = 0f
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
        
        if (placeholder != 0) {
            requestOptions.placeholder(placeholder)
        }
        if (error != 0) {
            requestOptions.error(error)
        }

        val requestBuilder = Glide.with(context)
            .load(filePath)
            .apply(requestOptions)

        // 如果设置了圆角，应用圆角变换
        if (cornerRadius > 0) {
            val radiusInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadius,
                context.resources.displayMetrics
            )
            
            requestBuilder.transform(RoundedCornersTransformation(radiusInPixels))
        }

        requestBuilder.into(imageView)
    }

    /**
     * 加载圆形图片
     * @param context 上下文
     * @param imageView 目标ImageView
     * @param url 图片URL或路径
     * @param placeholder 占位图资源ID
     * @param error 错误图资源ID
     */
    fun loadCircleImage(
        context: Context,
        imageView: ImageView,
        url: String?,
        placeholder: Int = 0,
        error: Int = 0
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
        
        if (placeholder != 0) {
            requestOptions.placeholder(placeholder)
        }
        if (error != 0) {
            requestOptions.error(error)
        }

        Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }

    /**
     * 加载图片用于全屏预览
     * 图片会完整显示，不会被裁剪，会根据屏幕尺寸自动缩放并居中
     * @param context 上下文
     * @param imageView 目标ImageView
     * @param filePath 本地文件路径
     * @param placeholder 占位图资源ID
     * @param error 错误图资源ID
     */
    fun loadImageForFullScreenPreview(
        context: Context,
        imageView: ImageView,
        filePath: String?,
        placeholder: Int = 0,
        error: Int = 0
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .fitCenter() // 使用fitCenter而不是centerCrop，确保图片完整显示

        if (placeholder != 0) {
            requestOptions.placeholder(placeholder)
        }
        if (error != 0) {
            requestOptions.error(error)
        }

        Glide.with(context)
            .load(filePath)
            .apply(requestOptions)
            .into(imageView)
    }

    /**
     * 清除内存缓存
     * @param context 上下文
     */
    fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }

    /**
     * 清除磁盘缓存
     * @param context 上下文
     */
    fun clearDiskCache(context: Context) {
        Glide.get(context).clearDiskCache()
    }

    /**
     * 清除所有缓存
     * @param context 上下文
     */
    fun clearAllCache(context: Context) {
        clearMemoryCache(context)
        clearDiskCache(context)
    }
} 