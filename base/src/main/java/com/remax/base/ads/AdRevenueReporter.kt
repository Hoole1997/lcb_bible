package com.remax.base.ads

/**
 * 广告收益数据
 * @param revenue 单次展示收益（值和货币）
 * @param adRevenueNetwork 广告平台名称
 * @param adRevenueUnit 广告位ID
 * @param adRevenuePlacement 广告源ID
 * @param adFormat 广告格式
 */
data class AdRevenueData(
    val revenue: RevenueInfo,
    val adRevenueNetwork: String,
    val adRevenueUnit: String,
    val adRevenuePlacement: String,
    val adFormat: String
)

/**
 * 收益信息
 * @param value 收益值（USD）
 * @param currencyCode 货币代码
 */
data class RevenueInfo(
    val value: Double,
    val currencyCode: String
)

/**
 * 广告收益上报管理器接口
 * 用于上报广告收益数据到第三方分析平台
 */
interface AdRevenueReporter {
    
    /**
     * 上报广告收益数据
     * @param adRevenueData 广告收益数据
     */
    fun reportAdRevenue(adRevenueData: AdRevenueData)
}

/**
 * 广告收益上报管理器
 * 单例模式，支持外部注入具体实现
 */
object AdRevenueManager {
    
    private val reporters = mutableListOf<AdRevenueReporter>()
    
    /**
     * 设置广告收益上报器实现
     * @param reporter 具体的上报器实现
     */
    fun setReporter(reporter: AdRevenueReporter) {
        reporters.clear()
        reporters.add(reporter)
    }
    
    /**
     * 设置多个广告收益上报器实现
     * @param reporters 上报器实现集合
     */
    fun setReporters(reporters: Collection<AdRevenueReporter>) {
        this.reporters.clear()
        this.reporters.addAll(reporters)
    }
    
    /**
     * 添加广告收益上报器实现
     * @param reporter 具体的上报器实现
     */
    fun addReporter(reporter: AdRevenueReporter) {
        if (!reporters.contains(reporter)) {
            reporters.add(reporter)
        }
    }
    
    /**
     * 移除广告收益上报器实现
     * @param reporter 要移除的上报器实现
     */
    fun removeReporter(reporter: AdRevenueReporter) {
        reporters.remove(reporter)
    }
    
    /**
     * 上报广告收益数据
     * @param adRevenueData 广告收益数据
     */
    fun reportAdRevenue(adRevenueData: AdRevenueData) {
        try {
            reporters.forEach { reporter ->
                try {
                    reporter.reportAdRevenue(adRevenueData)
                } catch (e: Exception) {
                    // 单个上报器异常不影响其他上报器
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // 静默处理异常，避免影响广告正常展示
            e.printStackTrace()
        }
    }
    
    /**
     * 清除所有上报器
     */
    fun clearReporters() {
        reporters.clear()
    }
    
    /**
     * 获取当前上报器数量
     * @return 上报器数量
     */
    fun getReporterCount(): Int = reporters.size
    
    /**
     * 检查是否有上报器
     * @return 是否有上报器
     */
    fun hasReporters(): Boolean = reporters.isNotEmpty()
}
