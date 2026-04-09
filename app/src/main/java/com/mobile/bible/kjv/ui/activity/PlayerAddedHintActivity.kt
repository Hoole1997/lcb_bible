package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityPlayerAddedHintBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.corekit.core.report.ReportDataManager

class PlayerAddedHintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerAddedHintBinding
    private lateinit var repository: KjvRepository
    private var isShowingRewardAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player_added_hint)
        repository = KjvRepository(this)
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

        binding.back.setOnClickListener {
            ReportDataManager.reportData("DoItNow_Close_Click", emptyMap())

            finish()
        }
        binding.buttonDoItNow.setOnClickListener {
            ReportDataManager.reportData("DoItNow_Click", emptyMap())

            showRewardAdAndStickyLatestPrayer()
        }
    }

    private fun showRewardAdAndStickyLatestPrayer() {
        if (isShowingRewardAd) return
        isShowingRewardAd = true
        lifecycleScope.launch {
            try {
                var rewardEarned = false
                val adResult = AdShowExt.showRewardedAd(
                    activity = this@PlayerAddedHintActivity,
                    onRewardEarned = { rewardEarned = true }
                )
                val adSucceeded = rewardEarned || adResult !is AdResult.Failure
                if (adSucceeded) {
                    withContext(Dispatchers.IO) {
                        repository.stickyLatestMyPrayerToTop()
                    }
                    navigateToPlayerWallPage()
                }
            } finally {
                isShowingRewardAd = false
            }
        }
    }

    private fun navigateToPlayerWallPage() {
        startActivity(
            Intent(this, KjvHomeActivity::class.java).apply {
                putExtra(KjvHomeActivity.EXTRA_START_TAB, KjvHomeActivity.Tab.PLAYER_WALL.name)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }

    override fun onBackPressed() {
        ReportDataManager.reportData("DoItNow_Close_Click", emptyMap())
        super.onBackPressed()
    }
}

