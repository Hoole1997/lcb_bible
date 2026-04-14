package com.mobile.bible.kjv.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGViewBinder
import com.android.common.bill.ads.renderer.PangleNativeAdRenderer
import com.mobile.bible.kjv.R

/**
 * Pangle 原生需在 [com.android.common.bill.BillConfig.pangleNativeRenderer] 中注册。
 */
class KjvPangleNativeAdRenderer : PangleNativeAdRenderer {

    override fun createLayout(context: Context): ViewGroup {
        return LayoutInflater.from(context).inflate(R.layout.bill_pangle_native_ad, null, false) as ViewGroup
    }

    override fun bindData(context: Context, adView: ViewGroup, nativeAdData: PAGNativeAdData) {
        adView.findViewById<TextView>(R.id.pangle_title).text = nativeAdData.title
        adView.findViewById<TextView>(R.id.pangle_desc).text = nativeAdData.description
        adView.findViewById<Button>(R.id.pangle_cta).text = nativeAdData.buttonText

        val iconIv = adView.findViewById<ImageView>(R.id.pangle_icon)
        nativeAdData.icon?.imageUrl?.let { url ->
            Glide.with(context).load(url).into(iconIv)
            iconIv.visibility = View.VISIBLE
        } ?: run { iconIv.visibility = View.GONE }

        val mediaContainer = adView.findViewById<FrameLayout>(R.id.pangle_media_container)
        mediaContainer.removeAllViews()
        val mediaView = nativeAdData.mediaView
        if (mediaView != null) {
            (mediaView.parent as? ViewGroup)?.removeView(mediaView)
            mediaContainer.addView(
                mediaView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        val logoWrap = adView.findViewById<FrameLayout>(R.id.pangle_logo_container)
        logoWrap.removeAllViews()
        val logoView = nativeAdData.adLogoView
        if (logoView != null) {
            (logoView.parent as? ViewGroup)?.removeView(logoView)
            logoWrap.addView(logoView)
            logoWrap.visibility = View.VISIBLE
        } else {
            logoWrap.visibility = View.GONE
        }
    }

    override fun createViewBinder(container: ViewGroup, adView: ViewGroup): PAGViewBinder {
        return PAGViewBinder.Builder(adView)
            .titleTextView(adView.findViewById(R.id.pangle_title))
            .descriptionTextView(adView.findViewById(R.id.pangle_desc))
            .callToActionButtonView(adView.findViewById(R.id.pangle_cta))
            .iconImageView(adView.findViewById(R.id.pangle_icon))
            .mediaContentViewGroup(adView.findViewById(R.id.pangle_media_container))
            .logoViewGroup(adView.findViewById(R.id.pangle_logo_container))
            .build()
    }

    override fun getClickViews(adView: ViewGroup): List<View> {
        return listOf(
            adView.findViewById(R.id.pangle_cta),
            adView.findViewById(R.id.pangle_media_container),
            adView.findViewById(R.id.pangle_title)
        )
    }
}
