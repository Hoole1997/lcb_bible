package com.mobile.bible.kjv.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import com.mobile.bible.kjv.databinding.ActivityFloatingWindowGuideBinding

class GuideFloatingWindowActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundResource(android.R.color.transparent)
        setContentView(root)

        val binding = ActivityFloatingWindowGuideBinding.inflate(layoutInflater, root, false)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM }
        root.addView(binding.root, params)

        binding.close.setOnClickListener { finish() }
        binding.rootLayout.setOnClickListener { finish() }
        binding.panelLayout.setOnClickListener {  }

        // Set images folder first to prevent "You must set an images folder" error
        binding.lottieView.setImageAssetsFolder("lottie_switch/images/")

        // 手动处理图片加载，解决路径解析问题
        binding.lottieView.setImageAssetDelegate { asset ->
            try {
                val fileName = asset.fileName
                val dirName = asset.dirName
                val path = "lottie_switch/$dirName$fileName"
                val inputStream = assets.open(path)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }

        binding.lottieView.setAnimation("lottie_switch/data.json")
        binding.lottieView.repeatCount = -1
        binding.lottieView.playAnimation()
    }
}
