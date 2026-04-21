package com.mobile.bible.kjv

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mobile.bible.kjv.analytics.CoreSdkTrackerBridge
import com.mobile.bible.kjv.noti.LocalNotificationService
import com.mobile.bible.kjv.utils.AppLifecycleObserver
import com.android.common.bill.BillConfig
import com.android.common.bill.ads.PreloadController
import com.android.common.bill.ads.bidding.AppOpenBiddingInitializer
import com.android.common.bill.ads.log.AdLogger
import com.android.common.bill.ads.renderer.AdLoadingDialogRenderer
import com.blankj.utilcode.util.LogUtils
import com.mobile.bible.kjv.ads.KjvAdmobNativeAdRenderer
import com.mobile.bible.kjv.ads.KjvPangleNativeAdRenderer
import com.mobile.bible.kjv.ads.KjvToponNativeAdRenderer
import com.mobile.bible.kjv.BuildConfig
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.ui.activity.AnswerFailedActivity
import com.mobile.bible.kjv.ui.activity.AnswerQuestionActivity
import com.mobile.bible.kjv.ui.activity.DebugActivity
import com.mobile.bible.kjv.ui.activity.GuideActivity
import com.mobile.bible.kjv.ui.activity.GuideFloatingWindowActivity
import com.mobile.bible.kjv.ui.activity.KjvHomeActivity
import com.mobile.bible.kjv.ui.activity.KjvSplashActivity
import com.mobile.bible.kjv.ui.activity.LevelFailedActivity
import com.mobile.bible.kjv.ui.activity.MyPostsActivity
import com.mobile.bible.kjv.ui.activity.OpenKjvVideoActivity
import com.mobile.bible.kjv.ui.activity.PlayerAddedHintActivity
import com.mobile.bible.kjv.ui.activity.PlayerCreateLoadingActivity
import com.mobile.bible.kjv.ui.activity.PrayerWallEditActivity
import com.mobile.bible.kjv.ui.activity.PrivacyPolicyActivity
import com.mobile.bible.kjv.ui.activity.VersePlayerActivity
import com.mobile.bible.kjv.ui.activity.VerseReadActivity
import com.remax.analytics.adjust.AdjustController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.log.CoreLogger
import kotlinx.coroutines.Dispatchers

class KjvApp : com.kjv.bible.read.study.verse.tool.Qdiwionr3a9w971f() {

    companion object {
        var kjvApp: KjvApp ?= null
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        ChannelUserController.setDefaultChannel(BuildConfig.DEFAULT_USER_CHANNEL)
        CoreLogger.setLogEnabled(BuildConfig.DEBUG)
        AdLogger.setLogEnabled(BuildConfig.DEBUG)
    }

    override fun onCreate() {
        super.onCreate()
        kjvApp = this
        AppLifecycleObserver.init(this)
        LocalNotificationService.bootstrap(this)
        CoreSdkTrackerBridge.initialize()
        this.hyperrestoresmartbattery {isOrganic, network, campaign, adgroup, creative, jsonResponse ->
            AdjustController.initialize(
                context = applicationContext,
                network = network,
                campaign = campaign,
                adgroup = adgroup,
                creative = creative,
                jsonResponse = jsonResponse
            )
            LogUtils.i("onCreate: isOrganic = $isOrganic , network = $network , campaign = $campaign , adgroup = $adgroup , creative = $creative , jsonResponse = $jsonResponse")
        }
        initAdSDK()
    }

    private fun initAdSDK() {
        applicationScope.launch {
            AppOpenBiddingInitializer.initialize(this@KjvApp, R.mipmap.ic_launcher) {
                admob = BillConfig.AdmobConfig(
                    applicationId = BuildConfig.ADMOB_APPLICATION_ID,
                    splashId = BuildConfig.ADMOB_SPLASH_ID,
                    bannerId = BuildConfig.ADMOB_BANNER_ID,
                    interstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
                    nativeId = BuildConfig.ADMOB_NATIVE_ID,
                    fullNativeId = BuildConfig.ADMOB_FULL_NATIVE_ID,
                    rewardedId = BuildConfig.ADMOB_REWARDED_ID
                )
                pangle = BillConfig.PangleConfig(
                    applicationId = BuildConfig.PANGLE_APPLICATION_ID,
                    splashId = BuildConfig.PANGLE_SPLASH_ID,
                    bannerId = BuildConfig.PANGLE_BANNER_ID,
                    interstitialId = BuildConfig.PANGLE_INTERSTITIAL_ID,
                    nativeId = BuildConfig.PANGLE_NATIVE_ID,
                    fullNativeId = BuildConfig.PANGLE_FULL_NATIVE_ID,
                    rewardedId = BuildConfig.PANGLE_REWARDED_ID
                )
                topon = BillConfig.ToponConfig(
                    applicationId = BuildConfig.TOPON_APPLICATION_ID,
                    appKey = BuildConfig.TOPON_APP_KEY,
                    splashId = BuildConfig.TOPON_SPLASH_ID,
                    bannerId = BuildConfig.TOPON_BANNER_ID,
                    interstitialId = BuildConfig.TOPON_INTERSTITIAL_ID,
                    nativeId = BuildConfig.TOPON_NATIVE_ID,
                    fullNativeId = BuildConfig.TOPON_FULL_NATIVE_ID,
                    rewardedId = BuildConfig.TOPON_REWARDED_ID
                )

                admobNativeRenderer = KjvAdmobNativeAdRenderer()
                pangleNativeRenderer = KjvPangleNativeAdRenderer()
                toponNativeRenderer = KjvToponNativeAdRenderer()
                adLoadingDialogRenderer = object : AdLoadingDialogRenderer {
                    override fun getLayoutResId(): Int = R.layout.dialog_ad_loading

                    override fun onViewCreated(view: View, onReady: () -> Unit) {
                        onReady()
                    }

                    override fun updateText(view: View, text: String) {
                        view.findViewById<TextView>(R.id.tv_ad_loading)?.text = text
                    }

                    override fun findCloseView(view: View): View? {
                        val context = view.context
                        val closeSizePx = (14 * context.resources.displayMetrics.density).toInt()
                        return ImageView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(closeSizePx, closeSizePx)
                            setImageResource(R.drawable.svg_close_white)
                        }
                    }

                    override fun onDestroy(view: View) = Unit
                }
            }
            CoreLogger.setLogEnabled(BuildConfig.DEBUG)
            AdLogger.setLogEnabled(BuildConfig.DEBUG)
            PreloadController.preloadAll(this@KjvApp)
        }
    }

    override fun repairsmartlocker(): Class<in Any>? {
        return KjvSplashActivity::class.java as Class<in Any>?
    }

    override fun proprocenter(): List<Class<in Any>?>? {
        return listOf(
            KjvSplashActivity::class.java,
            KjvHomeActivity::class.java,
            OpenKjvVideoActivity::class.java,
            DebugActivity::class.java,
            GuideActivity::class.java,
            GuideFloatingWindowActivity::class.java,
            VersePlayerActivity::class.java,
            VerseReadActivity::class.java,
            PrayerWallEditActivity::class.java,
            PlayerCreateLoadingActivity::class.java,
            PlayerAddedHintActivity::class.java,
            MyPostsActivity::class.java,
            AnswerQuestionActivity::class.java,
            AnswerFailedActivity::class.java,
            LevelFailedActivity::class.java,
            PrivacyPolicyActivity::class.java
        ) as List<Class<in Any>?>?
    }
}
