package com.mobile.bible.kjv.ui.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowCompat
import com.blankj.utilcode.util.LogUtils
import com.mobile.bible.kjv.permission.NotificationPermission
import com.mobile.bible.kjv.ui.vm.KjvSplashViewModel
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivitySplashBinding
import com.mobile.bible.kjv.BuildConfig
import com.remax.base.ext.KvBoolDelegate
import com.mobile.bible.kjv.constant.PrefKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class KjvSplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: KjvSplashViewModel
    private var hasShownGuide by KvBoolDelegate(PrefKeys.HAS_SHOWN_GUIDE, false)
    private var splashTaskReady = false
    private var notificationPermissionResolved = false
    private var hasNavigated = false

    companion object {
        const val EXTRA_SHOW_PREVIOUS_PAGE = "extra_show_previous_page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        viewModel = ViewModelProvider(this)[KjvSplashViewModel::class.java]
        
        simulateProgress()
        navigateNextPage()
        requestNotificationPermission()

        binding.tvPrivacyPolicy.setOnClickListener {
            PrivacyPolicyActivity.start(this, BuildConfig.PRIVACY_POLICY)
        }
    }
    
    private fun simulateProgress() {
        val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 100)
        animator.duration = 3000L // 总时长3秒
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            binding.textProgress.text = "$progress%"
        }
        animator.start()
    }
    
    private fun navigateNextPage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToHome.collect {
                    splashTaskReady = true
                    tryNavigateWhenReady()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (NotificationPermission.isGranted(this)) {
            notificationPermissionResolved = true
            tryNavigateWhenReady()
            return
        }
        NotificationPermission.request(this, object : NotificationPermission.Callback {
            override fun onGranted() {
                notificationPermissionResolved = true
                tryNavigateWhenReady()
            }

            override fun onDenied() {
                notificationPermissionResolved = true
                tryNavigateWhenReady()
            }

            override fun onDeniedPermanently() {
                notificationPermissionResolved = true
                tryNavigateWhenReady()
            }
        })
    }

    private fun tryNavigateWhenReady() {
        if (hasNavigated) return
        if (!splashTaskReady || !notificationPermissionResolved) return
        hasNavigated = true
        val showPrevious = intent.getBooleanExtra(EXTRA_SHOW_PREVIOUS_PAGE, false)
        if (showPrevious) {
            finish()
            return
        }
        val target = if (hasShownGuide) KjvHomeActivity::class.java else GuideActivity::class.java
        startActivity(Intent(this@KjvSplashActivity, target))
        finish()
    }

}
