package com.remax.analytics.revenue

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.remax.analytics.log.AnalyticsLogger
import com.remax.analytics.provider.AnalyticsModuleProvider
import com.remax.base.ads.AdRevenueData
import com.remax.base.ads.AdRevenueReporter
import com.remax.base.utils.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import java.io.IOException

/**
 * Firebase广告收益上报器实现
 * 将广告收益数据上报到Firebase Analytics平台
 */
class FirebaseAdRevenueReporter : AdRevenueReporter {
    
    private val gson = Gson()
    private var revenueConfigs: List<RevenueConfigItem> = emptyList()
    private var isConfigLoaded = false
    
    /**
     * 构造函数，异步获取Firebase Remote Config配置
     */
    init {
        loadRevenueConfig()
    }
    
    /**
     * 异步加载收益配置
     */
    private fun loadRevenueConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 首先尝试从Firebase Remote Config获取
                val remoteConfigJson = RemoteConfigManager.getString("rev_fir", "")
                
                if (remoteConfigJson != null && remoteConfigJson.isNotEmpty()) {
                    // 使用Remote Config的配置
                    revenueConfigs = gson.fromJson(remoteConfigJson, Array<RevenueConfigItem>::class.java).toList()
                    AnalyticsLogger.d("Firebase从Remote Config加载收益配置成功: $revenueConfigs")
                } else {
                    // 如果Remote Config没有配置，从assets文件读取
                    val assetsConfigJson = loadConfigFromAssets()
                    if (assetsConfigJson != null) {
                        revenueConfigs = gson.fromJson(assetsConfigJson, Array<RevenueConfigItem>::class.java).toList()
                        AnalyticsLogger.d("Firebase从assets加载收益配置成功: $revenueConfigs")
                    } else {
                        // 如果assets文件也读取失败，使用硬编码默认配置
                        revenueConfigs = getDefaultConfig()
                        AnalyticsLogger.d("Firebase使用硬编码默认收益配置: $revenueConfigs")
                    }
                }
                
                isConfigLoaded = true
                
            } catch (e: Exception) {
                AnalyticsLogger.e("Firebase加载收益配置失败", e)
                // 使用默认配置
                revenueConfigs = getDefaultConfig()
                isConfigLoaded = true
            }
        }
    }
    
    /**
     * 从assets文件读取配置
     * @return JSON字符串，读取失败返回null
     */
    private fun loadConfigFromAssets(): String? {
        return try {
            val context = AnalyticsModuleProvider.getApplicationContext()
            if (context != null) {
                context.assets.open("firebase_revenue_config.json").use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
            } else {
                AnalyticsLogger.w("Firebase无法获取Context，跳过从assets读取配置")
                null
            }
        } catch (e: IOException) {
            AnalyticsLogger.e("Firebase从assets读取firebase_revenue_config.json失败", e)
            null
        } catch (e: Exception) {
            AnalyticsLogger.e("Firebase读取assets配置异常", e)
            null
        }
    }
    
    /**
     * 获取默认配置
     * @return 默认配置列表
     */
    private fun getDefaultConfig(): List<RevenueConfigItem> {
        return listOf(
            RevenueConfigItem("ad_impression", 80),
            RevenueConfigItem("ad_other", 20)
        )
    }
    
    /**
     * 根据随机数选择收益配置
     * @return 选中的配置项名称
     */
    private fun selectRevenueConfig(): String {
        if (!isConfigLoaded || revenueConfigs.isEmpty()) {
            AnalyticsLogger.w("Firebase收益配置未加载或为空，使用默认值")
            return "ad_impression"
        }
        
        val randomValue = Random.nextInt(0, 101) // 0-100随机数
        var cumulativeRate = 0
        
        for (config in revenueConfigs) {
            cumulativeRate += config.rate
            if (randomValue <= cumulativeRate) {
                AnalyticsLogger.d("Firebase随机数: $randomValue, 选中配置: ${config.name}")
                return config.name
            }
        }
        
        // 如果没有匹配到，返回最后一个配置
        val lastConfig = revenueConfigs.last()
        AnalyticsLogger.d("Firebase随机数: $randomValue, 未匹配到配置，使用最后一个: ${lastConfig.name}")
        return lastConfig.name
    }
    
    /**
     * 获取Firebase Analytics实例，如果为空则异步等待
     * @return Firebase Analytics实例，可能为null
     */
    private fun getFirebaseAnalytics(): FirebaseAnalytics? {
        return try {
            val context = AnalyticsModuleProvider.getApplicationContext()
            if (context != null) {
                FirebaseAnalytics.getInstance(context)
            } else {
                AnalyticsLogger.w("无法获取应用上下文，Firebase Analytics获取失败")
                null
            }
        } catch (e: Exception) {
            AnalyticsLogger.e("获取Firebase Analytics实例失败", e)
            null
        }
    }

    /**
     * 异步等待获取Firebase Analytics实例
     * @return Firebase Analytics实例
     */
    private suspend fun waitForFirebaseAnalytics(): FirebaseAnalytics? {
        var attempts = 0
        val maxAttempts = 10 // 最多尝试10次
        val delayMs = 100L // 每次等待100ms

        while (attempts < maxAttempts) {
            val analytics = getFirebaseAnalytics()
            if (analytics != null) {
                return analytics
            }
            
            AnalyticsLogger.d("Firebase Analytics未就绪，等待中... (${attempts + 1}/$maxAttempts)")
            delay(delayMs)
            attempts++
        }
        
        AnalyticsLogger.e("等待Firebase Analytics超时，无法获取实例")
        return null
    }
    

    override fun reportAdRevenue(adRevenueData: AdRevenueData) {
        try {
            // 先尝试直接获取Firebase Analytics实例
            var analytics = getFirebaseAnalytics()
            
            // 如果获取为空，则异步等待阻塞获取
            if (analytics == null) {
                AnalyticsLogger.d("Firebase Analytics未就绪，开始异步等待...")
                analytics = runBlocking {
                    waitForFirebaseAnalytics()
                }
            }
            
            if (analytics == null) {
                AnalyticsLogger.w("无法获取Firebase Analytics实例，跳过广告收益上报")
                return
            }
            
            // 根据随机数选择收益配置
            val selectedConfigName = selectRevenueConfig()
            
            // 创建Bundle参数
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_PLATFORM, "Admob")
                putString(FirebaseAnalytics.Param.AD_SOURCE, adRevenueData.adRevenueNetwork)
                putString(FirebaseAnalytics.Param.AD_FORMAT, adRevenueData.adFormat)
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adRevenueData.adRevenueUnit)
                putDouble(FirebaseAnalytics.Param.VALUE, adRevenueData.revenue.value)
                putString(FirebaseAnalytics.Param.CURRENCY, adRevenueData.revenue.currencyCode)
            }

            // 上报到Firebase Analytics
            analytics.logEvent(selectedConfigName, bundle)
            
            AnalyticsLogger.d("Firebase广告收益数据已上报: $adRevenueData, 使用配置: $selectedConfigName")
            
        } catch (e: Exception) {
            AnalyticsLogger.e("Firebase上报广告收益数据失败", e)
        }
    }
}
