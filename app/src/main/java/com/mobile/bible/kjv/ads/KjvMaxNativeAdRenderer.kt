package com.mobile.bible.kjv.ads

import android.content.Context
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.android.common.bill.ads.renderer.MaxNativeAdRenderer
import com.mobile.bible.kjv.R

/**
 * MAX 原生需在 [com.android.common.bill.BillConfig.maxNativeRenderer] 中注册。
 */
class KjvMaxNativeAdRenderer : MaxNativeAdRenderer {

    override fun createNativeAdView(context: Context): MaxNativeAdView {
        val binder = MaxNativeAdViewBinder.Builder(R.layout.native_ad_max)
            .setTitleTextViewId(R.id.native_title)
            .setBodyTextViewId(R.id.native_body)
            .setCallToActionButtonId(R.id.native_cta_button)
            .setIconImageViewId(R.id.native_icon)
            .setMediaContentViewGroupId(R.id.native_media_view)
            .build()
        return MaxNativeAdView(binder, context)
    }
}
