package com.remax.base.view

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * 为RecyclerView添加线性分割线
 * @param dividerHeight 分割线高度，单位dp
 * @param dividerColorRes 分割线颜色资源ID
 * @param marginStart 分割线左边距，单位dp
 * @param marginEnd 分割线右边距，单位dp
 * @param shouldSkipItem 判断是否跳过该item的函数
 */
fun RecyclerView.addLinearDivider(
    dividerHeight: Int,
    dividerColorRes: Int,
    marginStart: Int = 0,
    marginEnd: Int = 0,
    shouldSkipItem: ((Int) -> Boolean)? = null
) {
    val dividerColor = ContextCompat.getColor(context, dividerColorRes)
    addItemDecoration(
        LinearDividerItemDecoration(
            dividerHeight = dividerHeight,
            dividerColor = dividerColor,
            marginStart = marginStart,
            marginEnd = marginEnd,
            shouldSkipItem = shouldSkipItem
        )
    )
}

/**
 * 为RecyclerView添加网格间距
 * @param spanCount 列数
 * @param spacing 间距，单位dp
 * @param includeEdge 是否包含边缘间距
 * @param shouldSkipItem 判断是否跳过该item的函数
 */
fun RecyclerView.addGridSpacing(
    spanCount: Int,
    spacing: Int,
    includeEdge: Boolean = true,
    shouldSkipItem: ((Int) -> Boolean)? = null
) {
    addItemDecoration(
        GridSpacingItemDecoration(
            spanCount = spanCount,
            spacing = spacing,
            includeEdge = includeEdge,
            shouldSkipItem = shouldSkipItem
        )
    )
}

/**
 * 为RecyclerView添加简单的分割线
 * @param dividerColorRes 分割线颜色资源ID
 * @param dividerHeight 分割线高度，单位dp
 */
fun RecyclerView.addSimpleDivider(
    dividerColorRes: Int,
    dividerHeight: Int = 1
) {
    addLinearDivider(
        dividerHeight = dividerHeight,
        dividerColorRes = dividerColorRes
    )
} 