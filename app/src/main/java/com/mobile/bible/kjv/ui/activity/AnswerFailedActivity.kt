package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ui.NativeAdStyleType
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityAnswerFailedBinding
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.ui.view.CircularCountdownView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import com.android.common.bill.ads.AdResult
import net.corekit.core.report.ReportDataManager

class AnswerFailedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_QUESTION = "extra_question"
        const val EXTRA_FAILED_INDEX = "extra_failed_index"
        const val ACTION_REVIVE = "action_revive"
        const val ACTION_FAILED = "action_failed"
    }

    private lateinit var binding: ActivityAnswerFailedBinding
    private lateinit var repository: KjvRepository
    private var currentLevel: Int = 1
    private var currentQuestion: String? = null
    private var failedIndex: Int = 0
    private var isNavigating = false
    private var isLoadingAd = false
    
    private val levelFailedLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Forward the result from LevelFailedActivity back to the caller (AnswerQuestionActivity)
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Check if this is a level-passed result (not ACTION_QUIT etc)
            val passedLevel = result.data?.getIntExtra(AnswerQuestionActivity.EXTRA_PASSED_LEVEL, -1) ?: -1
            if (passedLevel > 0) {
                // Forward the success result from AnswerQuestionActivity
                setResult(android.app.Activity.RESULT_OK, result.data)
                finish()
                return@registerForActivityResult
            }
        }
        // For ACTION_QUIT or other cases, just finish with ACTION_FAILED
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_FAILED))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_answer_failed)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        repository = KjvRepository(this)
        currentLevel = intent.getIntExtra(EXTRA_LEVEL, 1)
        currentQuestion = intent.getStringExtra(EXTRA_QUESTION)
        failedIndex = intent.getIntExtra(EXTRA_FAILED_INDEX, 0)

        setupCountdownTimer()
        setupClickListeners()
        loadCoins()
        loadNativeAd()
    }

    private fun loadNativeAd() {
        lifecycleScope.launch {
            val shown = AdShowExt.showNativeAdInContainer(
                this@AnswerFailedActivity,
                binding.nativeAdContainer,
                NativeAdStyleType.LARGE
            )
            if (shown) {
                binding.nativeAdShell.setCardBackgroundColor(Color.parseColor("#E62A231E"))
            }
        }
    }

    private fun loadCoins() {
        lifecycleScope.launch {
            repository.getCoinsFlow().collectLatest { coins ->
                binding.textCoin.text = (coins ?: 0).toString()
            }
        }
    }

    private fun setupCountdownTimer() {
        val timerView: CircularCountdownView = binding.circularTimer
        timerView.setDuration(10000L)
        timerView.setOnCountdownFinishedListener {
            // 倒计时结束，跳转到 LevelFailedActivity
            navigateToLevelFailed()
        }
        timerView.start()
    }

    private fun setupClickListeners() {
        // No thanks 按钮 - 跳转到 LevelFailedActivity
        binding.buttonNoThanks.setOnClickListener {
            val questionNumber = (failedIndex % 3) + 1
            ReportDataManager.reportData(
                "Nothanks_Click",
                mapOf("QuestionNumber" to questionNumber)
            )

            navigateToLevelFailed()
        }

        // Recovery with Ad 按钮 - 播放激励广告
        binding.buttonRecovery.setOnClickListener {
            if (isLoadingAd) return@setOnClickListener
            val questionNumber = (failedIndex % 3) + 1
            ReportDataManager.reportData(
                "Recoverywithad_Click",
                mapOf("QuestionNumber" to questionNumber)
            )
            isLoadingAd = true
            binding.circularTimer.pause()
            lifecycleScope.launch {
                var rewardEarned = false
                val result = AdShowExt.showRewardedAd(
                    activity = this@AnswerFailedActivity,
                    onRewardEarned = { rewardEarned = true }
                )
                if (rewardEarned) {
                    // 广告播放成功，复活
                    Log.d("AnswerFailedActivity", "Ad shown successfully, reviving")
                    finishWithRevive()
                } else {
                    // 广告播放失败或取消，恢复倒计时
                    val errorMsg = if (result is AdResult.Failure) result.error.message else "Unknown"
                    Log.e("AnswerFailedActivity", "Ad failed: $errorMsg")
                    isLoadingAd = false
                    binding.circularTimer.resume()
                }
            }
        }
    }

    private fun finishWithRevive() {
        if (isNavigating) return
        isNavigating = true
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION, ACTION_REVIVE))
        finish()
    }

    private fun navigateToLevelFailed() {
        if (isNavigating) return
        isNavigating = true
        binding.circularTimer.pause()
        val intent = Intent(this, LevelFailedActivity::class.java)
        intent.putExtra(LevelFailedActivity.EXTRA_LEVEL, currentLevel)
        intent.putExtra(LevelFailedActivity.EXTRA_FAILED_INDEX, failedIndex)
        currentQuestion?.let {
            intent.putExtra(LevelFailedActivity.EXTRA_QUESTION, it)
        }
        levelFailedLauncher.launch(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateToLevelFailed()
    }
}
