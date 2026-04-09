package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityLevelFailedBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import net.corekit.core.report.ReportDataManager

class LevelFailedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COIN = "extra_coin"
        const val EXTRA_QUESTION = "extra_question"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_FAILED_INDEX = "extra_failed_index"

        const val EXTRA_ACTION = "extra_action"
        const val ACTION_SKIP_LEVEL = "action_skip_level"
        const val ACTION_TRY_AGAIN = "action_try_again"
        const val ACTION_QUIT = "action_quit"
    }

    private lateinit var binding: ActivityLevelFailedBinding
    private lateinit var repository: KjvRepository
    private var currentLevel: Int = 1
    private var isLoadingAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_level_failed)
        repository = KjvRepository(this)
        currentLevel = intent.getIntExtra(EXTRA_LEVEL, 1)

        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        lifecycleScope.launch {
            repository.getCoinsFlow().collectLatest { coins ->
                binding.textCoin.text = (coins ?: 0).toString()
            }
        }

        val question = intent.getStringExtra(EXTRA_QUESTION)
            ?: "On the ________day, God createdman in his own image."
        binding.textQuestion.text = question

        // Update dots to highlight the failed question position
        val failedIndex = intent.getIntExtra(EXTRA_FAILED_INDEX, 0)
        updateDotsForFailedQuestion(failedIndex)

        updateGiftDistances()


        binding.buttonSkipLevel.setOnClickListener {
            ReportDataManager.reportData("SkipLevel_Click", mapOf("LevelNumber" to currentLevel))

            if (isLoadingAd) return@setOnClickListener
            isLoadingAd = true
            lifecycleScope.launch {
                var rewardEarned = false
                val result = AdShowExt.showRewardedAd(
                    activity = this@LevelFailedActivity,
                    onRewardEarned = { rewardEarned = true }
                )
                if (rewardEarned) {
                    Log.d("LevelFailedActivity", "Ad shown successfully, skipping level $currentLevel")
                    repository.setHighestPassedLevel(currentLevel)
                    finishWithAction(ACTION_SKIP_LEVEL)
                } else {
                    val errorMsg = if (result is AdResult.Failure) result.error.message else "Unknown"
                    Log.e("LevelFailedActivity", "Ad failed: $errorMsg")
                    isLoadingAd = false
                }
            }
        }

        binding.buttonTryAgain.setOnClickListener {
            ReportDataManager.reportData("TryAgain_Click", mapOf("LevelNumber" to currentLevel))

            val intent = Intent(this, AnswerQuestionActivity::class.java)
            intent.putExtra(AnswerQuestionActivity.EXTRA_LEVEL, currentLevel)
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(intent)
            finish()
        }

        binding.textQuit.setOnClickListener {
            ReportDataManager.reportData("Quit_Click", mapOf("LevelNumber" to currentLevel))

            finishWithAction(ACTION_QUIT)
        }
    }

    private fun finishWithAction(action: String) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, action))
        finish()
    }

    private fun updateDotsForFailedQuestion(failedIndex: Int) {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { index, dot ->
            if (index == failedIndex) {
                dot.setBackgroundResource(R.drawable.bg_level_failed_dot_active)
            } else {
                dot.setBackgroundResource(R.drawable.bg_level_failed_dot_inactive)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishWithAction(ACTION_QUIT)
    }

    private fun updateGiftDistances() {
        val nextRewards = mutableListOf<Int>()
        var checkLevel = currentLevel
        
        // Find next 3 reward levels
        while (nextRewards.size < 3) {
            // Check current level first if it's a reward level? 
            // The requirement says "distance calculated from current level".
            // If current level IS a reward level, distance is 0.
            // So we start checking from currentLevel.
            if (repository.isRewardLevel(checkLevel)) {
                nextRewards.add(checkLevel)
            }
            checkLevel++
        }

        val textViews = listOf(binding.textGift1, binding.textGift2, binding.textGift3)
        
        nextRewards.forEachIndexed { index, rewardLevel ->
            if (index < textViews.size) {
                val distance = rewardLevel - currentLevel
                val text = if (distance <= 0) {
                    getString(R.string.reward_ready)
                } else {
                    distance.toString()
                }
                textViews[index].text = text
            }
        }
    }
}
