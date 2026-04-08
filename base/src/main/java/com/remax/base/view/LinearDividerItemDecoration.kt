package com.remax.base.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView线性布局分割线装饰器
 * @param dividerHeight 分割线高度，单位dp
 * @param dividerColor 分割线颜色
 * @param marginStart 分割线左边距，单位dp
 * @param marginEnd 分割线右边距，单位dp
 * @param shouldSkipItem 判断是否跳过该item的函数
 */
open class LinearDividerItemDecoration(
    private val dividerHeight: Int,
    private val dividerColor: Int,
    private val marginStart: Int = 0,
    private val marginEnd: Int = 0,
    private val shouldSkipItem: ((Int) -> Boolean)? = null
) : RecyclerView.ItemDecoration() {
    
    // 缓存实际位置映射，避免重复计算
    private val actualPositionCache = mutableMapOf<Int, Int>()
    private var totalActualItemsCache: Int? = null

    private val paint = Paint().apply {
        color = dividerColor
        style = Paint.Style.FILL
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        
        // 检查是否应该跳过该item
        if (shouldSkipItem?.invoke(position) == true) {
            // 对于跳过的item，不设置任何间距
            outRect.bottom = 0
            return
        }
        
        // 计算实际的item位置（排除跳过的item）
        val actualPosition = calculateActualPosition(position, parent)
        val totalActualItems = calculateTotalActualItems(parent)
        
        // 检查当前item后面是否还有非跳过的item
        val hasNextNonSkippedItem = hasNextNonSkippedItem(position, parent)
        
        // 如果后面还有非跳过的item，则添加底部间距
        if (hasNextNonSkippedItem) {
            outRect.bottom = dividerHeight
        } else {
            outRect.bottom = 0
        }
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // 当数据变化时清除缓存
        actualPositionCache.clear()
        totalActualItemsCache = null
        
        val layoutManager = parent.layoutManager
        if (layoutManager == null) return

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            
            // 检查是否应该跳过该item
            if (shouldSkipItem?.invoke(position) == true) {
                continue
            }
            
            // 检查当前item后面是否还有非跳过的item
            val hasNextNonSkippedItem = hasNextNonSkippedItem(position, parent)
            
            // 如果后面没有非跳过的item，则跳过绘制分割线
            if (!hasNextNonSkippedItem) {
                continue
            }

            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = parent.paddingLeft + marginStart
            val right = parent.width - parent.paddingRight - marginEnd
            val top = child.bottom + params.bottomMargin
            val bottom = top + dividerHeight

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // 如果需要绘制在内容之上，可以在这里实现
        super.onDrawOver(c, parent, state)
    }
    
    /**
     * 计算排除跳过item后的实际位置
     */
    private fun calculateActualPosition(position: Int, parent: RecyclerView): Int {
        if (shouldSkipItem == null) {
            return position
        }
        
        // 检查缓存
        actualPositionCache[position]?.let { return it }
        
        var actualPosition = 0
        for (i in 0 until position) {
            if (!shouldSkipItem.invoke(i)) {
                actualPosition++
            }
        }
        
        // 缓存结果
        actualPositionCache[position] = actualPosition
        return actualPosition
    }
    
    /**
     * 计算总的实际item数量（排除跳过的item）
     */
    private fun calculateTotalActualItems(parent: RecyclerView): Int {
        if (shouldSkipItem == null) {
            return parent.adapter?.itemCount ?: 0
        }
        
        // 检查缓存
        totalActualItemsCache?.let { return it }
        
        val totalItems = parent.adapter?.itemCount ?: 0
        var actualCount = 0
        for (i in 0 until totalItems) {
            if (!shouldSkipItem.invoke(i)) {
                actualCount++
            }
        }
        
        // 缓存结果
        totalActualItemsCache = actualCount
        return actualCount
    }
    
    /**
     * 检查指定位置后面是否还有非跳过的item
     */
    private fun hasNextNonSkippedItem(position: Int, parent: RecyclerView): Boolean {
        if (shouldSkipItem == null) {
            return position < (parent.adapter?.itemCount ?: 0) - 1
        }
        
        val totalItems = parent.adapter?.itemCount ?: 0
        for (i in position + 1 until totalItems) {
            if (!shouldSkipItem.invoke(i)) {
                return true
            }
        }
        return false
    }
} 