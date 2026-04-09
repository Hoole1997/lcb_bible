package com.mobile.bible.kjv.ui.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.mobile.bible.kjv.ui.adapter.GuidePage
import com.mobile.bible.kjv.ui.adapter.GuidePagerAdapter
import com.remax.base.ext.KvBoolDelegate
import com.mobile.bible.kjv.constant.PrefKeys
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityGuideBinding
import androidx.core.view.WindowCompat

class GuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuideBinding
    private lateinit var adapter: GuidePagerAdapter
    private var hasShownGuide by KvBoolDelegate(PrefKeys.HAS_SHOWN_GUIDE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_guide)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pages = listOf(
            GuidePage(
                page = 0,
                imageResId = R.mipmap.img_splash_guide_a
            ),
            GuidePage(
                page = 1,
                imageResId = R.mipmap.img_splash_guide_b
            ),
            GuidePage(
                page = 2,
                imageResId = R.mipmap.img_splash_guide_c
            ),
            GuidePage(
                page = 3,
                imageResId = R.mipmap.img_splash_guide_d
            )
        )

        adapter = GuidePagerAdapter(
            pages,
            onNext = { index ->
                val next = index + 1
                if (next < pages.size) {
                    binding.viewPager.setCurrentItem(next, true)
                } else {
                    onGuideEndNext()
                }
            },
            onSkip = { index ->
                onGuideSkip(index)
            }
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.isUserInputEnabled = false

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onGuideSkip(binding.viewPager.currentItem)
            }
        })
    }

    private fun onGuideEndNext() {
        hasShownGuide = true
        startActivity(Intent(this, KjvHomeActivity::class.java))
        finish()
    }

    private fun onGuideSkip(page: Int) {
        hasShownGuide = true
        startActivity(Intent(this, KjvHomeActivity::class.java))
        finish()
    }

}
