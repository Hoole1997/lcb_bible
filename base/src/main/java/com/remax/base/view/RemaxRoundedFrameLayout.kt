package com.remax.base.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import com.remax.base.R

/**
 * 支持圆角的FrameLayout
 * 支持自定义圆角半径、背景颜色、边框等
 */
class RemaxRoundedFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cornerRadius: Float = 0f
    private var cornerRadiusTopLeft: Float = 0f
    private var cornerRadiusTopRight: Float = 0f
    private var cornerRadiusBottomLeft: Float = 0f
    private var cornerRadiusBottomRight: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    init {
        initAttributes(context, attrs)
        setWillNotDraw(false)
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RemaxRoundedFrameLayout)
        try {
            // 读取圆角半径
            cornerRadius = typedArray.getDimension(R.styleable.RemaxRoundedFrameLayout_remaxCornerRadius, 0f)
            cornerRadiusTopLeft = typedArray.getDimension(R.styleable.RemaxRoundedFrameLayout_remaxCornerRadiusTopLeft, cornerRadius)
            cornerRadiusTopRight = typedArray.getDimension(R.styleable.RemaxRoundedFrameLayout_remaxCornerRadiusTopRight, cornerRadius)
            cornerRadiusBottomLeft = typedArray.getDimension(R.styleable.RemaxRoundedFrameLayout_remaxCornerRadiusBottomLeft, cornerRadius)
            cornerRadiusBottomRight = typedArray.getDimension(R.styleable.RemaxRoundedFrameLayout_remaxCornerRadiusBottomRight, cornerRadius)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (cornerRadius > 0 || hasCustomCorners()) {
            drawRoundedBackground(canvas)
        }
        super.onDraw(canvas)
    }

    private fun hasCustomCorners(): Boolean {
        return cornerRadiusTopLeft > 0 || cornerRadiusTopRight > 0 || 
               cornerRadiusBottomLeft > 0 || cornerRadiusBottomRight > 0
    }

    private fun drawRoundedBackground(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // 重置路径
        path.reset()

        // 设置矩形区域
        rect.set(0f, 0f, width, height)

        // 创建圆角路径
        if (hasCustomCorners()) {
            // 使用不同的圆角半径
            path.moveTo(cornerRadiusTopLeft, 0f)
            path.lineTo(width - cornerRadiusTopRight, 0f)
            path.quadTo(width, 0f, width, cornerRadiusTopRight)
            path.lineTo(width, height - cornerRadiusBottomRight)
            path.quadTo(width, height, width - cornerRadiusBottomRight, height)
            path.lineTo(cornerRadiusBottomLeft, height)
            path.quadTo(0f, height, 0f, height - cornerRadiusBottomLeft)
            path.lineTo(0f, cornerRadiusTopLeft)
            path.quadTo(0f, 0f, cornerRadiusTopLeft, 0f)
        } else {
            // 使用统一的圆角半径
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        // 应用裁剪路径
        canvas.clipPath(path)
    }

    /**
     * 设置圆角半径
     * @param radius 圆角半径，单位dp
     */
    fun setCornerRadius(radius: Float) {
        this.cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            radius,
            resources.displayMetrics
        )
        cornerRadiusTopLeft = this.cornerRadius
        cornerRadiusTopRight = this.cornerRadius
        cornerRadiusBottomLeft = this.cornerRadius
        cornerRadiusBottomRight = this.cornerRadius
        invalidate()
    }

    /**
     * 设置各个角的圆角半径
     * @param topLeft 左上角圆角半径，单位dp
     * @param topRight 右上角圆角半径，单位dp
     * @param bottomLeft 左下角圆角半径，单位dp
     * @param bottomRight 右下角圆角半径，单位dp
     */
    fun setCornerRadius(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        this.cornerRadiusTopLeft = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            topLeft,
            resources.displayMetrics
        )
        this.cornerRadiusTopRight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            topRight,
            resources.displayMetrics
        )
        this.cornerRadiusBottomLeft = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            bottomLeft,
            resources.displayMetrics
        )
        this.cornerRadiusBottomRight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            bottomRight,
            resources.displayMetrics
        )
        invalidate()
    }


} 