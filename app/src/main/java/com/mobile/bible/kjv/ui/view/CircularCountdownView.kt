package com.mobile.bible.kjv.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import com.mobile.bible.kjv.R
import kotlin.math.ceil

class CircularCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()

    private var strokeWidthPx = 0f
    private var progress = 1f
    private var durationMillis = 10000L
    private var remainingMillis = durationMillis

    private var animator: ValueAnimator? = null
    private var onCountdownFinishedListener: (() -> Unit)? = null

    init {
        val density = resources.displayMetrics.density
        strokeWidthPx = 6f * density

        backgroundPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx

        backgroundPaint.color = 0x33FFFFFF
        progressPaint.color = 0xFFFFFFFFF.toInt()

        textPaint.color = ResourcesCompat.getColor(resources, android.R.color.white, null)
        textPaint.textSize = 46f * density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (120 * resources.displayMetrics.density).toInt()
        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        val size = minOf(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) - strokeWidthPx) / 2f

        arcBounds.set(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius
        )

        canvas.drawArc(arcBounds, -90f, 360f, false, backgroundPaint)

        canvas.drawArc(arcBounds, -90f, -360f * progress, false, progressPaint)

        val seconds = if (remainingMillis <= 0L) {
            0
        } else {
            ceil(remainingMillis / 1000f).toInt()
        }
        val text = seconds.toString()
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, cx, textY, textPaint)
    }

    fun setDuration(millis: Long) {
        if (millis <= 0) return
        durationMillis = millis
        reset()
    }

    fun setOnCountdownFinishedListener(listener: () -> Unit) {
        onCountdownFinishedListener = listener
    }

    fun start() {
        animator?.cancel()
        remainingMillis = durationMillis
        progress = 1f
        animator = ValueAnimator.ofInt(durationMillis.toInt(), 0).apply {
            duration = durationMillis
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                remainingMillis = (valueAnimator.animatedValue as Int).toLong()
                progress = remainingMillis.toFloat() / durationMillis
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    remainingMillis = 0L
                    progress = 0f
                    invalidate()
                    onCountdownFinishedListener?.invoke()
                }
            })
            start()
        }
    }

    fun reset() {
        animator?.cancel()
        remainingMillis = durationMillis
        progress = 1f
        invalidate()
    }

    fun pause() {
        animator?.pause()
    }

    fun resume() {
        animator?.resume()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
