package com.remax.base.utils

import android.graphics.*
import androidx.annotation.NonNull
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 自定义圆角变换
 * 兼容Glide v4
 */
class RoundedCornersTransformation(
    private val cornerRadius: Float
) : BitmapTransformation() {

    companion object {
        private const val ID = "com.remax.base.utils.RoundedCornersTransformation"
        private val ID_BYTES = ID.toByteArray(CHARSET)
    }

    override fun transform(
        @NonNull pool: BitmapPool,
        @NonNull toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val bitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        if (bitmap == null) {
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val rect = RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        return bitmap
    }

    override fun equals(other: Any?): Boolean {
        return other is RoundedCornersTransformation && other.cornerRadius == cornerRadius
    }

    override fun hashCode(): Int {
        return ID.hashCode() + (cornerRadius * 1000).toInt()
    }

    override fun updateDiskCacheKey(@NonNull messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
        val radiusBytes = cornerRadius.toString().toByteArray(CHARSET)
        messageDigest.update(radiusBytes)
    }
} 