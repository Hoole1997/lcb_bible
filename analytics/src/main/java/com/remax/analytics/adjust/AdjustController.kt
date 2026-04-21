package com.remax.analytics.adjust

import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.OnAttributionChangedListener
import com.adjust.sdk.OnEventTrackingSucceededListener
import com.adjust.sdk.OnEventTrackingFailedListener
import com.adjust.sdk.OnSessionTrackingSucceededListener
import com.adjust.sdk.OnSessionTrackingFailedListener
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustEventFailure
import com.adjust.sdk.AdjustEventSuccess
import com.adjust.sdk.AdjustSessionFailure
import com.adjust.sdk.AdjustSessionSuccess
import com.adjust.sdk.LogLevel
import com.adjust.sdk.OnAdidReadListener
import com.remax.base.controller.UserChannelController
import com.remax.analytics.BuildConfig
import com.remax.analytics.log.AnalyticsLogger
import com.remax.analytics.report.CommonParamsManager
import com.remax.base.report.DataReportManager

/**
 * Adjust归因控制器
 * 负责Adjust SDK的初始化和归因数据获取
 */
object AdjustController {

    private var isInitialized = false
    private var attributionData: AdjustAttribution? = null
    private var initStartTime: Long = 0

    /**
     * 初始化Adjust SDK
     * @param context 应用上下文
     */
    fun initialize(context: Context,network: String?,campaign: String?,adgroup: String?,creative: String?,jsonResponse: String?) {
        if (isInitialized) {
            AnalyticsLogger.w("Adjust SDK 已经初始化过了")
            return
        }

        // 记录初始化开始时间
        initStartTime = System.currentTimeMillis()
        AnalyticsLogger.d("Adjust SDK 初始化开始")

        // 初始化登录相关参数
        CommonParamsManager.initLoginParams()
        AnalyticsLogger.d("登录参数初始化完成 - login_day: ${CommonParamsManager.loginDay ?: ""}, is_new: ${CommonParamsManager.isNew ?: ""}")

        // 先设置登录参数到DataReportManager
        val loginParams = CommonParamsManager.getAllCommonParams()
        val userParams = CommonParamsManager.getUserCommonParams()
        DataReportManager.setCommonParams(loginParams)
        DataReportManager.setUserParams(userParams)
        AnalyticsLogger.d("登录参数已设置到DataReportManager: $loginParams")

        // 初始化埋点
        DataReportManager.reportData("adjust_init", mapOf())
        try {
            // 设置公共参数，并限制长度
            CommonParamsManager.adNetwork = (network ?: "").take(10)
            CommonParamsManager.campaign = (campaign ?: "").take(20)
            CommonParamsManager.adgroup = (adgroup ?: "").take(10)
            CommonParamsManager.creative = (creative ?: "").take(20)

            AnalyticsLogger.d("公共参数设置完成 - ad_network: ${CommonParamsManager.adNetwork}, campaign: ${CommonParamsManager.campaign}, adgroup: ${CommonParamsManager.adgroup}, creative: ${CommonParamsManager.creative}")

            // 将公共参数设置到DataReportManager
            val commonParams = CommonParamsManager.getAllCommonParams()
            val userParams = CommonParamsManager.getUserCommonParams()
            DataReportManager.setCommonParams(commonParams)
            DataReportManager.setUserParams(userParams)
            AnalyticsLogger.d("公共参数已设置到DataReportManager: $commonParams")

            // 计算从初始化开始到归因回调的总耗时（秒数，向上取整）
            val totalDurationSeconds = kotlin.math.ceil((System.currentTimeMillis() - initStartTime) / 1000.0).toInt()
            AnalyticsLogger.d("Adjust初始化到归因回调总耗时: ${totalDurationSeconds}秒")
            DataReportManager.reportData("adjust_get_success", mapOf("pass_time" to totalDurationSeconds))

            // 设置当前用户渠道类型
            val userChannelType = if (AnalyticsLogger.isLogEnabled()) {
                // 内部版本强制设置为买量类型
                AnalyticsLogger.d("内部版本强制设置为买量类型")
                UserChannelController.UserChannelType.PAID
            } else {
                determineUserChannelType(network,campaign,adgroup,creative,jsonResponse)
            }
            AnalyticsLogger.d("根据归因数据判断用户渠道类型: $userChannelType")

            // 设置用户渠道类型
            val success = UserChannelController.setChannel(userChannelType)
            if (success) {
                AnalyticsLogger.i("用户渠道类型设置成功: $userChannelType")
            } else {
                AnalyticsLogger.w("用户渠道类型已经设置过，无法修改")
            }
//            val appToken = BuildConfig.ADJUST_APP_TOKEN
//            if (appToken.isBlank() || appToken == "your_adjust_app_token_here") {
//                AnalyticsLogger.e("Adjust App Token 未配置或使用默认值")
//                return
//            }

            // 创建Adjust配置
//            val adjustConfig = AdjustConfig(
//                context,
//                appToken,
//                if (AnalyticsLogger.isLogEnabled()) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
//            )
//            adjustConfig.setLogLevel(LogLevel.VERBOSE)

            // 启用成本数据在归因信息中
//            adjustConfig.enableCostDataInAttribution()

            // 设置归因回调
//            adjustConfig.setOnAttributionChangedListener(object : OnAttributionChangedListener {
//                override fun onAttributionChanged(attribution: AdjustAttribution?) {
//
//                    attributionData = attribution
//                    AnalyticsLogger.d("Adjust归因数据更新: $attribution")
//
//                    // 设置公共参数
//                    attribution?.let { attr ->
//
//                    }
//                }
//            })
//
//            // 设置事件跟踪成功回调
//            adjustConfig.setOnEventTrackingSucceededListener(object :
//                OnEventTrackingSucceededListener {
//                override fun onEventTrackingSucceeded(eventSuccessResponseData: AdjustEventSuccess?) {
//                    AnalyticsLogger.d("Adjust事件跟踪成功: ${eventSuccessResponseData?.message}")
//                }
//            })
//
//            // 设置事件跟踪失败回调
//            adjustConfig.setOnEventTrackingFailedListener(object : OnEventTrackingFailedListener {
//                override fun onEventTrackingFailed(eventFailureResponseData: AdjustEventFailure?) {
//                    AnalyticsLogger.e("Adjust事件跟踪失败: ${eventFailureResponseData?.message}")
//                }
//            })
//
//            // 设置会话跟踪成功回调
//            adjustConfig.setOnSessionTrackingSucceededListener(object :
//                OnSessionTrackingSucceededListener {
//                override fun onSessionTrackingSucceeded(sessionSuccessResponseData: AdjustSessionSuccess?) {
//                    AnalyticsLogger.d("Adjust会话跟踪成功: ${sessionSuccessResponseData?.message}")
//                }
//            })
//
//            // 设置会话跟踪失败回调
//            adjustConfig.setOnSessionTrackingFailedListener(object :
//                OnSessionTrackingFailedListener {
//                override fun onSessionTrackingFailed(sessionFailureResponseData: AdjustSessionFailure?) {
//                    AnalyticsLogger.e("Adjust会话跟踪失败: ${sessionFailureResponseData?.message}")
//                }
//            })
//
//            // 启动Adjust SDK
//            Adjust.initSdk(adjustConfig)

            isInitialized = true
            AnalyticsLogger.i("Adjust SDK 初始化成功")

        } catch (e: Exception) {
            AnalyticsLogger.e("Adjust SDK 初始化失败", e)
        }
    }

    /**
     * 根据归因数据判断用户渠道类型
     * @param attribution Adjust归因数据
     * @return 用户渠道类型
     */
    private fun determineUserChannelType(network: String?,campaign: String?,adgroup: String?,creative: String?,jsonResponse: String?): UserChannelController.UserChannelType {
        // 获取归因数据的关键字段
        val network = network?.lowercase()
        val campaign = campaign?.lowercase()

        AnalyticsLogger.d("归因数据 - network: $network, campaign: $campaign")

        // 判断是否为自然渠道的条件
        val isOrganic = when {
            // 1. Organic - 有机渠道
            network == "organic" -> {
                AnalyticsLogger.d("检测到Organic渠道")
                true
            }
            // 2. Untrusted Devices - 不可信设备
            network == "untrusted devices" -> {
                AnalyticsLogger.d("检测到Untrusted Devices渠道")
                true
            }
            // 3. Google Organic Search - Google有机搜索
            network == "google organic search" -> {
                AnalyticsLogger.d("检测到Google Organic Search渠道")
                true
            }
            // 4. 其他情况都认为是买量渠道
            else -> {
                AnalyticsLogger.d("检测到买量渠道 - network: $network")
                false
            }
        }

        return if (isOrganic) {
            UserChannelController.UserChannelType.NATURAL
        } else {
            UserChannelController.UserChannelType.PAID
        }
    }

    /**
     * 获取归因数据
     * @return AdjustAttribution对象，如果未初始化或没有数据则返回null
     */
    fun getAttribution(): AdjustAttribution? {
        if (!isInitialized) {
            AnalyticsLogger.w("Adjust SDK 未初始化，无法获取归因数据")
            return null
        }

        return attributionData
    }

    /**
     * 获取设备的Adjust ID（异步）
     * @param callback 回调函数，参数为adid字符串
     */
    fun getAdid(callback: (String?) -> Unit) {
        if (!isInitialized) {
            AnalyticsLogger.w("Adjust SDK 未初始化，无法获取Adid")
            callback(null)
            return
        }

        Adjust.getAdid(object : OnAdidReadListener {
            override fun onAdidRead(adid: String?) {
                AnalyticsLogger.d("获取到Adjust Adid: $adid")
                callback(adid)
            }
        })
    }

    /**
     * 获取归因信息（简化版本）
     * 注意：adid字段需要通过异步回调获取，这里返回null
     * @return 包含归因信息的Map
     */
    fun getAttributionInfo(): Map<String, String?> {
        val attribution = getAttribution() ?: return emptyMap()

        return mapOf(
            "trackerToken" to attribution.trackerToken,
            "trackerName" to attribution.trackerName,
            "network" to attribution.network,
            "campaign" to attribution.campaign,
            "adgroup" to attribution.adgroup,
            "creative" to attribution.creative,
            "clickLabel" to attribution.clickLabel,
            "adid" to null, // adid需要通过getAdid(callback)异步获取
            "costType" to attribution.costType,
            "costAmount" to attribution.costAmount?.toString(),
            "costCurrency" to attribution.costCurrency,
            "fbInstallReferrer" to attribution.fbInstallReferrer,
            "jsonResponse" to attribution.jsonResponse
        )
    }

    /**
     * 获取完整的归因信息（包括异步adid）
     * @param callback 回调函数，参数为包含adid的完整归因信息Map
     */
    fun getCompleteAttributionInfo(callback: (Map<String, String?>) -> Unit) {
        val attribution = getAttribution() ?: return callback(emptyMap())

        // 先获取基本归因信息
        val baseInfo = mapOf(
            "trackerToken" to attribution.trackerToken,
            "trackerName" to attribution.trackerName,
            "network" to attribution.network,
            "campaign" to attribution.campaign,
            "adgroup" to attribution.adgroup,
            "creative" to attribution.creative,
            "clickLabel" to attribution.clickLabel,
            "costType" to attribution.costType,
            "costAmount" to attribution.costAmount?.toString(),
            "costCurrency" to attribution.costCurrency,
            "fbInstallReferrer" to attribution.fbInstallReferrer,
            "jsonResponse" to attribution.jsonResponse
        )

        // 异步获取adid
        getAdid { adid ->
            val completeInfo = baseInfo + ("adid" to adid)
            callback(completeInfo)
        }
    }

    /**
     * 跟踪事件
     * @param eventToken 事件Token
     * @param revenue 收入（可选）
     * @param currency 货币（可选）
     * @param callbackParams 回调参数（可选）
     * @param partnerParams 合作伙伴参数（可选）
     */
    fun trackEvent(
        eventToken: String,
        revenue: Double? = null,
        currency: String? = null,
        callbackParams: Map<String, String>? = null,
        partnerParams: Map<String, String>? = null
    ) {
        if (!isInitialized) {
            AnalyticsLogger.w("Adjust SDK 未初始化，无法跟踪事件")
            return
        }

        try {
            val adjustEvent = AdjustEvent(eventToken)

            // 设置收入
            revenue?.let { adjustEvent.setRevenue(it, currency) }

            // 设置回调参数
            callbackParams?.forEach { (key, value) ->
                adjustEvent.addCallbackParameter(key, value)
            }

            // 设置合作伙伴参数
            partnerParams?.forEach { (key, value) ->
                adjustEvent.addPartnerParameter(key, value)
            }

            Adjust.trackEvent(adjustEvent)
            AnalyticsLogger.d("Adjust事件跟踪: $eventToken")

        } catch (e: Exception) {
            AnalyticsLogger.e("Adjust事件跟踪失败: $eventToken", e)
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}
