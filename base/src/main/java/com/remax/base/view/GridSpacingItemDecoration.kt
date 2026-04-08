package com.remax.base.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView网格布局间距装饰器
 * @param spanCount 列数
 * @param spacing 间距，单位dp
 * @param includeEdge 是否包含边缘间距
 * @param shouldSkipItem 判断是否跳过该item的函数
 */
open class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean = true,
    private val shouldSkipItem: ((Int) -> Boolean)? = null
) : RecyclerView.ItemDecoration() {
    
    // 缓存实际位置映射，避免重复计算
    private val actualPositionCache = mutableMapOf<Int, Int>()
    
    override fun onDraw(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        // 当数据变化时清除缓存
        actualPositionCache.clear()
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
            // 对于跳过的item，设置为全宽，不设置上下间距
            outRect.left = 0
            outRect.right = 0
            outRect.top = 0
            outRect.bottom = 0
            return
        }
        
        // 计算实际的列位置（排除跳过的item）
        val actualPosition = calculateActualPosition(position, parent)
        val column = actualPosition % spanCount
        val row = actualPosition / spanCount

        if (includeEdge) {
            // 包含边缘间距
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (row == 0) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            // 不包含边缘间距
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount

            if (row > 0) {
                outRect.top = spacing
            }
        }
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
}