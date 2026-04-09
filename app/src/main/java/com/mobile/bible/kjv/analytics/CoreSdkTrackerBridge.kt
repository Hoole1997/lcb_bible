package com.mobile.bible.kjv.analytics

import com.mobile.bible.kjv.BuildConfig
import com.remax.analytics.data.FirebaseDataController
import com.remax.analytics.data.ThinkingDataController
import com.remax.analytics.log.AnalyticsLogger
import com.remax.analytics.revenue.AdjustAdRevenueReporter
import com.remax.analytics.revenue.FirebaseAdRevenueReporter
import com.remax.base.ads.AdRevenueData as BaseAdRevenueData
import com.remax.base.ads.AdRevenueManager as BaseAdRevenueManager
import com.remax.base.ads.AdRevenueReporter as BaseAdRevenueReporter
import com.remax.base.ads.RevenueInfo as BaseRevenueInfo
import com.remax.base.report.DataReporter
import net.corekit.core.ads.RevenueAdData as CoreRevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueAdReporter
import net.corekit.core.report.ReportDataManager
import net.corekit.core.report.ReporterData

/**
 * Bridge core/bill tracker reporters into project analytics reporters.
 */
object CoreSdkTrackerBridge {
    private val adDebugKeys = listOf(
        "ad_format",
        "ad_platform",
        "ad_source",
        "ad_unit_name",
        "ad_unique_id",
        "number",
        "position",
        "pass_time",
        "reason",
        "session_id",
        "request_id",
        "is_preload",
        "value",
        "currency",
        "reward_label",
        "reward_amount"
    )

    fun initialize() {
        try {
            val dataReporters = listOf<DataReporter>(
                ThinkingDataController(),
                FirebaseDataController()
            )
            ReportDataManager.setReporters(dataReporters.map(::CoreReporterAdapter))

            val revenueReporters = listOf<BaseAdRevenueReporter>(
                FirebaseAdRevenueReporter(),
                AdjustAdRevenueReporter()
            )
            BaseAdRevenueManager.setReporters(revenueReporters)
            RevenueAdManager.setReporters(revenueReporters.map(::CoreRevenueReporterAdapter))

            AnalyticsLogger.d("CoreSdkTrackerBridge 初始化完成")
        } catch (e: Exception) {
            AnalyticsLogger.e("CoreSdkTrackerBridge 初始化失败", e)
        }
    }

    private class CoreReporterAdapter(
        private val delegate: DataReporter
    ) : ReporterData {
        override fun getName(): String = delegate.getName()

        override fun reportData(eventName: String, data: Map<String, Any>) {
            logAdEventIfDebug(eventName, data)
            delegate.reportData(eventName, data)
        }

        override fun setCommonParams(params: Map<String, Any>) {
            delegate.setCommonParams(params)
        }

        override fun setUserParams(params: Map<String, Any>) {
            delegate.setUserParams(params)
        }

        private fun logAdEventIfDebug(eventName: String, data: Map<String, Any>) {
            if (!BuildConfig.DEBUG || !eventName.startsWith("ad_")) return

            val keyParams = linkedMapOf<String, Any>()
            adDebugKeys.forEach { key ->
                data[key]?.let { value -> keyParams[key] = value }
            }

            AnalyticsLogger.d(
                "AdDebug event=%s, params=%s",
                eventName,
                if (keyParams.isEmpty()) "{}" else keyParams.toString()
            )
        }
    }

    private class CoreRevenueReporterAdapter(
        private val delegate: BaseAdRevenueReporter
    ) : RevenueAdReporter {
        override fun reportAdRevenue(adRevenueData: CoreRevenueAdData) {
            val mappedData = BaseAdRevenueData(
                revenue = BaseRevenueInfo(
                    value = adRevenueData.revenue.value,
                    currencyCode = adRevenueData.revenue.currencyCode
                ),
                adRevenueNetwork = adRevenueData.adRevenueNetwork,
                adRevenueUnit = adRevenueData.adRevenueUnit,
                adRevenuePlacement = adRevenueData.adRevenuePlacement,
                adFormat = adRevenueData.adFormat
            )
            delegate.reportAdRevenue(mappedData)
        }
    }
}
