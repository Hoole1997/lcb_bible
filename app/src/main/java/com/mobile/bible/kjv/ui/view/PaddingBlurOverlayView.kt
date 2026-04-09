package com.mobile.bible.kjv.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View

class PaddingBlurOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFDFFFFFF.toInt()
    }
    private var blurEnabled = false
    private var blurRadiusPx = 0f
    private var blurTargetView: View? = null
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private val targetLocation = IntArray(2)
    private val selfLocation = IntArray(2)

    fun setBlurEnabled(enabled: Boolean) {
        if (blurEnabled == enabled) return
        blurEnabled = enabled
        visibility = if (enabled) VISIBLE else GONE
        updateRenderEffect()
        if (enabled) {
            invalidate()
        }
    }

    fun setBlurRadiusPx(radius: Float) {
        blurRadiusPx = radius
        updateRenderEffect()
        if (blurEnabled) {
            invalidate()
        }
    }

    fun setBlurTargetView(view: View?) {
        blurTargetView = view
    }

    private fun updateRenderEffect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!blurEnabled) {
            setRenderEffect(null)
            return
        }
        val radius = if (blurRadiusPx > 0f) blurRadiusPx else resources.displayMetrics.density * 12f
        setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            if (bitmap == null || bitmap?.width != w || bitmap?.height != h) {
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmapCanvas = Canvas(bitmap!!)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!blurEnabled) return
        if (width <= 0 || height <= 0) return
        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom
        if (right <= left || bottom <= top) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            canvas.save()
            canvas.clipRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), fallbackPaint)
            canvas.restore()
            return
        }
        val target = blurTargetView ?: rootView
        val bitmap = bitmap ?: return
        val bitmapCanvas = bitmapCanvas ?: return
        bitmapCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        val previousVisibility = visibility
        visibility = INVISIBLE
        target.getLocationOnScreen(targetLocation)
        getLocationOnScreen(selfLocation)
        val dx = (selfLocation[0] - targetLocation[0]).toFloat()
        val dy = (selfLocation[1] - targetLocation[1]).toFloat()
        bitmapCanvas.save()
        bitmapCanvas.translate(-dx, -dy)
        target.draw(bitmapCanvas)
        bitmapCanvas.restore()
        visibility = previousVisibility
        if (blurRadiusPx <= 0f) {
            blurRadiusPx = resources.displayMetrics.density * 12f
        }
        canvas.save()
        canvas.clipRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        canvas.drawBitmap(bitmap, 0f, 0f, blurPaint)
        canvas.restore()
    }
}
