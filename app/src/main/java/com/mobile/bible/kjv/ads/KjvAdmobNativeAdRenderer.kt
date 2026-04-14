package com.mobile.bible.kjv.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.android.common.bill.ads.renderer.AdmobNativeAdRenderer
import com.mobile.bible.kjv.R

/**
 * Bill 要求注册 [com.android.common.bill.BillConfig.admobNativeAdRenderer]，否则 AdMob 原生绑定会抛错并展示失败。
 * 参考 GMA Next-Gen [Display your ad](https://developers.google.com/ad-manager/mobile-ads-sdk/android/next-gen/native/advanced)。
 */
class KjvAdmobNativeAdRenderer : AdmobNativeAdRenderer {

    override fun createLayout(context: Context): NativeAdView {
        return LayoutInflater.from(context).inflate(R.layout.bill_admob_native_ad, null, false) as NativeAdView
    }

    override fun bindData(adView: NativeAdView, nativeAd: NativeAd) {
        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        val body = adView.findViewById<TextView>(R.id.ad_body)
        val cta = adView.findViewById<TextView>(R.id.ad_call_to_action)
        val advertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
        val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val media = adView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.ad_media)

        adView.headlineView = headline
        adView.bodyView = body
        adView.callToActionView = cta
        adView.advertiserView = advertiser
        adView.iconView = icon

        headline.text = nativeAd.headline
        body.text = nativeAd.body
        cta.text = nativeAd.callToAction
        advertiser.text = nativeAd.advertiser

        nativeAd.icon?.drawable?.let { icon.setImageDrawable(it) }

        headline.visibility = assetVisibility(nativeAd.headline)
        body.visibility = assetVisibility(nativeAd.body)
        cta.visibility = assetVisibility(nativeAd.callToAction)
        advertiser.visibility = assetVisibility(nativeAd.advertiser)
        icon.visibility = if (nativeAd.icon != null) View.VISIBLE else View.GONE

        adView.registerNativeAd(nativeAd, media)
    }

    private fun assetVisibility(text: String?): Int =
        if (text.isNullOrBlank()) View.GONE else View.VISIBLE
}
