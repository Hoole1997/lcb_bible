package com.mobile.bible.kjv.ui.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityPlayerCreateLoadingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerCreateLoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerCreateLoadingBinding
    private var loadingAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player_create_loading)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.isAppearanceLightStatusBars = true

        val toolbarLp = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
        val baseTopMargin = toolbarLp.topMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbarLp.topMargin = baseTopMargin + sys.top
            binding.toolbar.layoutParams = toolbarLp
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        val density = resources.displayMetrics.density
        val radius = (16 * density).toInt()
        Glide.with(this)
            .load(R.mipmap.img_player_creating)
            .transform(CenterCrop(), RoundedCorners(radius))
            .into(binding.iconBig)

        loadingAnimator = ObjectAnimator.ofFloat(binding.loadingIcon, "rotation", 0f, 360f).apply {
            duration = 1200
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        lifecycleScope.launch {
            delay(1000)
            startActivity(Intent(this@PlayerCreateLoadingActivity, PlayerAddedHintActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        loadingAnimator?.cancel()
        super.onDestroy()
    }
}
