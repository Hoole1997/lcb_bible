package com.remax.analytics.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.remax.analytics.adjust.AdjustController
import com.remax.analytics.data.FirebaseDataController
import com.remax.analytics.data.ThinkingDataController
import com.remax.analytics.log.AnalyticsLogger
import com.remax.analytics.revenue.AdjustAdRevenueReporter
import com.remax.analytics.revenue.FirebaseAdRevenueReporter
import com.remax.base.ads.AdRevenueManager
import com.remax.base.report.DataReportManager

/**
 * 分析统计模块内容提供者
 * 用于在模块初始化时获取 Context 并初始化 AdjustController
 */
class AnalyticsModuleProvider : ContentProvider() {
    
    companion object {
        private var applicationContext: android.content.Context? = null
        
        /**
         * 获取应用上下文
         */
        fun getApplicationContext(): android.content.Context? = applicationContext
    }
    
    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        applicationContext?.let { ctx ->
            try {
                // 初始化数数SDK控制器
//                ThinkingDataController.initialize(ctx)

                // 初始化基础数据上报器，保证 Adjust 初始化阶段事件可被上报
                DataReportManager.setReporters(
                    listOf(
                        ThinkingDataController(),
                        FirebaseDataController()
                    )
                )

                // 初始化Adjust控制器
//                AdjustController.initialize(ctx)
                
                // 注册广告收益上报器
                AdRevenueManager.setReporters(
                    listOf(
                        FirebaseAdRevenueReporter(),
                        AdjustAdRevenueReporter()
                    )
                )
                
                AnalyticsLogger.d("AnalyticsModuleProvider 初始化完成")
            } catch (e: Exception) {
                AnalyticsLogger.e("AnalyticsModuleProvider 初始化失败", e)
            }
        }

        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null
    
    override fun getType(uri: Uri): String? = null
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
