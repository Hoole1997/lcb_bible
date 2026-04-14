package com.mobile.bible.kjv.ads

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.android.common.bill.ads.renderer.ToponNativeAdRenderer
import com.mobile.bible.kjv.R
import com.thinkup.nativead.api.TUNativeMaterial
import com.thinkup.nativead.api.TUNativePrepareInfo

/**
 * TopOn 自渲染原生需在 [com.android.common.bill.BillConfig.toponNativeRenderer] 中注册（模板渲染不需要）。
 */
class KjvToponNativeAdRenderer : ToponNativeAdRenderer {

    override fun createLayout(context: Context): ViewGroup {
        return LayoutInflater.from(context).inflate(R.layout.bill_topon_native_ad, null, false) as ViewGroup
    }

    override fun bindData(adView: ViewGroup, material: TUNativeMaterial) {
        val title = adView.findViewById<TextView>(R.id.topon_title)
        val desc = adView.findViewById<TextView>(R.id.topon_desc)
        val cta = adView.findViewById<TextView>(R.id.topon_cta)
        val icon = adView.findViewById<ImageView>(R.id.topon_icon)
        val mainImage = adView.findViewById<ImageView>(R.id.topon_main_image)

        title.text = material.title
        desc.text = material.descriptionText
        cta.text = material.callToActionText

        if (!TextUtils.isEmpty(material.iconImageUrl)) {
            Glide.with(adView.context).load(material.iconImageUrl).into(icon)
            icon.visibility = View.VISIBLE
        } else {
            icon.visibility = View.GONE
        }

        if (!TextUtils.isEmpty(material.mainImageUrl)) {
            Glide.with(adView.context).load(material.mainImageUrl).into(mainImage)
            mainImage.visibility = View.VISIBLE
        } else {
            mainImage.visibility = View.GONE
        }
    }

    override fun createPrepareInfo(adView: ViewGroup): TUNativePrepareInfo {
        return TUNativePrepareInfo().apply {
            setTitleView(adView.findViewById(R.id.topon_title))
            setDescView(adView.findViewById(R.id.topon_desc))
            setCtaView(adView.findViewById(R.id.topon_cta))
            setIconView(adView.findViewById(R.id.topon_icon))
            setMainImageView(adView.findViewById(R.id.topon_main_image))
        }
    }
}
