package com.mobile.bible.kjv.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mobile.bible.kjv.data.entity.QuestionEntity
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.prefs.AudioSettings
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityAnswerQuestionBinding
import com.mobile.bible.kjv.constant.RemoteConfigKeys
import com.remax.base.utils.RemoteConfigManager
import kotlinx.coroutines.launch
import com.android.common.bill.ads.ext.AdShowExt
import com.mobile.bible.kjv.ui.dialog.TokenTradeDialog
import com.mobile.bible.kjv.ui.dialog.AnswerPauseDialog
import net.corekit.core.report.ReportDataManager

class AnswerQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_PASSED_LEVEL = "extra_passed_level"
        const val EXTRA_IS_REWARD_LEVEL = "extra_is_reward_level"
    }

    private lateinit var binding: ActivityAnswerQuestionBinding
    private lateinit var repository: KjvRepository

    private var currentLevel: Int = 1
    private var questions: List<QuestionEntity> = emptyList()
    private var currentQuestionIndex: Int = 0
    private var correctAnswerCount: Int = 0
    private var isAnswering: Boolean = true
    private val handler = Handler(Looper.getMainLooper())
    
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var backgroundMediaPlayer: MediaPlayer? = null
    private val COUNTDOWN_DURATION = 30000L
    private val COUNTDOWN_AUDIO_THRESHOLD = 10000L
    private val COUNTDOWN_INTERVAL = 16L
    private var remainingCountdownTime: Long = COUNTDOWN_DURATION
    private var currentCountdownTotal: Long = COUNTDOWN_DURATION
    private var isBackgroundMusicPlaying: Boolean = false
    private var isCardBlurred: Boolean = false
    private var cardBlurRadiusPx: Float = 0f
    
    private var skipAnswerFailedOnFirstQuestion: Boolean = false

    private val levelFailedLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val passedLevel = result.data?.getIntExtra(EXTRA_PASSED_LEVEL, -1) ?: -1
            if (passedLevel > 0) {
                // Forward the success result to StudyFragment
                setResult(Activity.RESULT_OK, result.data)
                finish()
                return@registerForActivityResult
            }

            val action = result.data?.getStringExtra(LevelFailedActivity.EXTRA_ACTION)
            when (action) {
                LevelFailedActivity.ACTION_SKIP_LEVEL -> {
                    setResult(Activity.RESULT_OK, result.data)
                    finish()
                }
                LevelFailedActivity.ACTION_QUIT -> {
                    finish()
                }
            }
        }
    }

    private val answerFailedLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Check if this is a forwarded level-passed result from nested AnswerQuestionActivity
            val passedLevel = result.data?.getIntExtra(EXTRA_PASSED_LEVEL, -1) ?: -1
            if (passedLevel > 0) {
                // Forward the success result to StudyFragment
                setResult(Activity.RESULT_OK, result.data)
                finish()
                return@registerForActivityResult
            }
            
            // Otherwise, handle normal AnswerFailedActivity actions
            val action = result.data?.getStringExtra(AnswerFailedActivity.EXTRA_ACTION)
            when (action) {
                AnswerFailedActivity.ACTION_REVIVE -> {
                    // 复活成功，继续答题
                    onRevive()
                }
                AnswerFailedActivity.ACTION_FAILED -> {
                    // 用户放弃，关闭答题页面，返回关卡选择
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_answer_question)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        repository = KjvRepository(this)
        currentLevel = intent.getIntExtra(EXTRA_LEVEL, 1)

        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        loadQuestions()
        loadPropsCount()
        loadRemoteConfig()
        updateCountdown(30)
        setupOptionClickListeners()
        setupAdClickListener()
        setupBuyPropsClickListeners()
        setupUsePropsClickListeners()
        setupPauseClickListener()
        
        // Start background music
        startBackgroundMusic()
    }
    
    private fun setupBuyPropsClickListeners() {
        binding.buyDelay.setOnClickListener {
            showBuyDelayDialog()
        }
        
        binding.buyEraser.setOnClickListener {
            showBuyEraserDialog()
        }
    }
    
    private fun setupUsePropsClickListeners() {
        binding.actionAddTime.setOnClickListener {
            lifecycleScope.launch {
                val timePropCount = repository.getTimePropCount()
                if (timePropCount > 0) {
                    useDelayProp()
                } else {
                    showBuyDelayDialog()
                }
            }
        }
        
        binding.actionReselect.setOnClickListener {
            lifecycleScope.launch {
                val eraserPropCount = repository.getEraserPropCount()
                if (eraserPropCount > 0) {
                    useEraserProp()
                } else {
                    showBuyEraserDialog()
                }
            }
        }
    }
    
    private fun useDelayProp() {
        lifecycleScope.launch {
            if (repository.useTimeProp()) {
                val questionNumber = (currentQuestionIndex % 3) + 1
                ReportDataManager.reportData(
                    "Extra_Time_Use",
                    mapOf(
                        "QuestionNumber" to questionNumber,
                        "LevelNumber" to currentLevel
                    )
                )
                remainingCountdownTime += 20000L
                currentCountdownTotal = remainingCountdownTime
                pauseCountdownTimer()
                refreshPropsAndCoins()
                resumeCountdownTimer()
            }
        }
    }
    
    private fun useEraserProp() {
        if (questions.isEmpty() || !isAnswering) return
        
        lifecycleScope.launch {
            if (repository.useEraserProp()) {
                val questionNumber = (currentQuestionIndex % 3) + 1
                ReportDataManager.reportData(
                    "Eliminate_Wrong_Use",
                    mapOf(
                        "QuestionNumber" to questionNumber,
                        "LevelNumber" to currentLevel
                    )
                )
                refreshPropsAndCoins()
                removeOneWrongOption()
            }
        }
    }
    
    private fun removeOneWrongOption() {
        val question = questions[currentQuestionIndex]
        val correctAnswer = question.answerIndex
        val wrongOptions = listOf("A", "B", "C", "D").filter { it != correctAnswer }
        
        val optionLayouts = mapOf(
            "A" to binding.optionA,
            "B" to binding.optionB,
            "C" to binding.optionC,
            "D" to binding.optionD
        )
        
        val visibleWrongOptions = wrongOptions.filter { optionLayouts[it]?.visibility == View.VISIBLE }
        if (visibleWrongOptions.isNotEmpty()) {
            val optionToHide = visibleWrongOptions.random()
            val viewToHide = optionLayouts[optionToHide] ?: return
            
            viewToHide.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    val transition = AutoTransition().apply {
                        duration = 150
                    }
                    TransitionManager.beginDelayedTransition(binding.cardContainer, transition)
                    viewToHide.visibility = if (optionToHide == "D") View.INVISIBLE else View.GONE
                }
                .start()
        }
    }
    
    private fun showBuyDelayDialog() {
        pauseCountdownTimer()
        var didTapConfirm = false
        var didReportExtraTimeBuy = false
        TokenTradeDialog.show(this, TokenTradeDialog.PropType.DELAY, 25, 10, object : TokenTradeDialog.OnTokenTradeListener {
            override fun onConfirmExchange(quantity: Int, totalCost: Int) {
                didTapConfirm = true
                lifecycleScope.launch {
                    val questionNumber = (currentQuestionIndex % 3) + 1
                    if (repository.buyTimeProp(quantity)) {
                        ReportDataManager.reportData(
                            "Extra_Time_Buy",
                            mapOf(
                                "QuestionNumber" to questionNumber,
                                "LevelNumber" to currentLevel,
                                "count" to quantity,
                                "result" to "confirm"
                            )
                        )
                        didReportExtraTimeBuy = true
                        refreshPropsAndCoins()
                    } else {
                        ReportDataManager.reportData(
                            "Extra_Time_Buy",
                            mapOf(
                                "QuestionNumber" to questionNumber,
                                "LevelNumber" to currentLevel,
                                "count" to quantity,
                                "result" to "close"
                            )
                        )
                        didReportExtraTimeBuy = true
                        Toast.makeText(this@AnswerQuestionActivity, R.string.insufficient_coins, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }) { finalCount ->
            if (!didTapConfirm && !didReportExtraTimeBuy) {
                val questionNumber = (currentQuestionIndex % 3) + 1
                ReportDataManager.reportData(
                    "Extra_Time_Buy",
                    mapOf(
                        "QuestionNumber" to questionNumber,
                        "LevelNumber" to currentLevel,
                        "count" to finalCount,
                        "result" to "close"
                    )
                )
            }
            resumeCountdownTimer()
        }
    }
    
    private fun showBuyEraserDialog() {
        pauseCountdownTimer()
        var didTapConfirm = false
        var didReportEliminateWrongBuy = false
        TokenTradeDialog.show(this, TokenTradeDialog.PropType.ERASER, 25, 10, object : TokenTradeDialog.OnTokenTradeListener {
            override fun onConfirmExchange(quantity: Int, totalCost: Int) {
                didTapConfirm = true
                lifecycleScope.launch {
                    val questionNumber = (currentQuestionIndex % 3) + 1
                    if (repository.buyEraserProp(quantity)) {
                        ReportDataManager.reportData(
                            "Eliminate_Wrong_Buy",
                            mapOf(
                                "QuestionNumber" to questionNumber,
                                "LevelNumber" to currentLevel,
                                "count" to quantity,
                                "result" to "confirm"
                            )
                        )
                        didReportEliminateWrongBuy = true
                        refreshPropsAndCoins()
                    } else {
                        ReportDataManager.reportData(
                            "Eliminate_Wrong_Buy",
                            mapOf(
                                "QuestionNumber" to questionNumber,
                                "LevelNumber" to currentLevel,
                                "count" to quantity,
                                "result" to "close"
                            )
                        )
                        didReportEliminateWrongBuy = true
                        Toast.makeText(this@AnswerQuestionActivity, R.string.insufficient_coins, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }) { finalCount ->
            if (!didTapConfirm && !didReportEliminateWrongBuy) {
                val questionNumber = (currentQuestionIndex % 3) + 1
                ReportDataManager.reportData(
                    "Eliminate_Wrong_Buy",
                    mapOf(
                        "QuestionNumber" to questionNumber,
                        "LevelNumber" to currentLevel,
                        "count" to finalCount,
                        "result" to "close"
                    )
                )
            }
            resumeCountdownTimer()
        }
    }
    
    private fun refreshPropsAndCoins() {
        lifecycleScope.launch {
            val coins = repository.getCoins()
            val timePropCount = repository.getTimePropCount()
            val eraserPropCount = repository.getEraserPropCount()
            binding.textCoin.text = coins.toString()
            binding.propsACount.text = timePropCount.toString()
            binding.propsBCount.text = eraserPropCount.toString()
        }
    }
    
    private fun setupAdClickListener() {
        binding.viewAd.setOnClickListener {
            val questionNumber = (currentQuestionIndex % 3) + 1
            val answerEventParams = mapOf(
                "LevelNumber" to currentLevel,
                "QuestionNumber" to questionNumber
            )

            ReportDataManager.reportData("CoinsAD_Click", answerEventParams)

            stopCountdownTimer()
            releaseMediaPlayer()
            stopBackgroundMusic()
            lifecycleScope.launch {
                var rewardEarned = false
                AdShowExt.showRewardedAd(
                    activity = this@AnswerQuestionActivity,
                    onRewardEarned = { rewardEarned = true }
                )
                if (rewardEarned) {
                    repository.addCoins(50)
                    val coins = repository.getCoins()
                    binding.textCoin.text = coins.toString()
                }
                // Don't start timer here; onResume will show the pause dialog,
                // and the timer resumes when the user dismisses it.
            }
        }
    }
    
    private fun setupPauseClickListener() {
        binding.actionPause.setOnClickListener {
            pauseCountdownTimer()
            setCardBlurEnabled(true)
            AnswerPauseDialog.show(this, object : AnswerPauseDialog.OnActionListener {
                override fun onMusicClick() {
                    if (!AudioSettings.isMusicEnabled) {
                        stopBackgroundMusic()
                    }
                }
                
                override fun onSoundClick() {
                    // Sound toggle already handled by AudioSettings in the dialog
                    if (!com.mobile.bible.kjv.prefs.AudioSettings.isSoundEnabled) {
                        releaseMediaPlayer()
                    }
                }
                
                override fun onQuitClick() {
                    // Dismiss dialog and treat as wrong answer
                    setCardBlurEnabled(false)
                    stopCountdownTimer()
                    releaseMediaPlayer()
                    releaseBackgroundMediaPlayer()
                    isAnswering = false
                    navigateToFailedScreen()
                }
                
                override fun onResumeClick() {
                    // Resume countdown timer
                    resumeCountdownTimer()
                }
                
                override fun onDialogDismissed() {
                    setCardBlurEnabled(false)
                }
            })
        }
    }

    private fun setCardBlurEnabled(enabled: Boolean) {
        if (enabled == isCardBlurred) return
        isCardBlurred = enabled
        if (enabled) {
            if (cardBlurRadiusPx == 0f) {
                cardBlurRadiusPx = resources.displayMetrics.density * 12f
            }
            binding.cardBlurOverlay.setBlurTargetView(binding.cardContainer)
            binding.cardBlurOverlay.setBlurRadiusPx(cardBlurRadiusPx)
        }
        binding.cardBlurOverlay.setBlurEnabled(enabled)
    }
    
    private var showPauseDialogOnResume: Boolean = false

    private val showPauseDialogRunnable = Runnable {
        if (isAnswering && remainingCountdownTime > 0) {
            binding.actionPause.performClick()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(showPauseDialogRunnable)
        if (isAnswering && remainingCountdownTime > 0) {
            pauseCountdownTimer()
            showPauseDialogOnResume = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (showPauseDialogOnResume) {
            showPauseDialogOnResume = false
            // If the pause dialog is already showing, no need to create a new one
            val existingDialog = supportFragmentManager.findFragmentByTag("AnswerPauseDialog")
            if (existingDialog == null) {
                handler.post(showPauseDialogRunnable)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdownTimer()
        releaseMediaPlayer()
        releaseBackgroundMediaPlayer()
    }
    
    private fun startCountdownTimer() {
        stopCountdownTimer()
        remainingCountdownTime = COUNTDOWN_DURATION
        currentCountdownTotal = COUNTDOWN_DURATION
        startCountdownTimerInternal(COUNTDOWN_DURATION)
    }
    
    private fun startCountdownTimerInternal(duration: Long) {
        if (duration <= COUNTDOWN_AUDIO_THRESHOLD) {
            playCountdownAudio()
        }
        
        binding.countdownProgress.max = 10000
        val initialProgress = (duration * 10000 / currentCountdownTotal).toInt()
        binding.countdownProgress.progress = initialProgress
        
        countDownTimer = object : CountDownTimer(duration, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                remainingCountdownTime = millisUntilFinished
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                updateCountdown(secondsRemaining)
                
                val progress = (millisUntilFinished * 10000 / currentCountdownTotal).toInt()
                binding.countdownProgress.progress = progress
                
                if (millisUntilFinished <= COUNTDOWN_AUDIO_THRESHOLD && mediaPlayer == null) {
                    playCountdownAudio()
                }
            }
            
            override fun onFinish() {
                remainingCountdownTime = 0
                updateCountdown(0)
                binding.countdownProgress.progress = 0
                releaseMediaPlayer()
                
                if (isAnswering) {
                    isAnswering = false
                    onTimeOut()
                }
            }
        }.start()
    }
    
    private fun stopCountdownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
    
    private fun pauseCountdownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        releaseMediaPlayer()
        stopBackgroundMusic(true)
    }
    
    private fun resumeCountdownTimer() {
        if (remainingCountdownTime > 0) {
            startCountdownTimerInternal(remainingCountdownTime)
        }
        // Resume background music if it was playing
        if (AudioSettings.isMusicEnabled) {
            startBackgroundMusic()
        }
    }
    
    private fun onTimeOut() {
        releaseBackgroundMediaPlayer()
        navigateToFailedScreen()
    }
    
    private fun loadRemoteConfig() {
        lifecycleScope.launch {
            val configValue = RemoteConfigManager.getInt(
                RemoteConfigKeys.KEY_SKIP_ANSWER_FAILED_ON_FIRST_QUESTION,
                0
            ) ?: 0
            skipAnswerFailedOnFirstQuestion = configValue == 1
        }
    }
    
    private fun navigateToFailedScreen() {
        val isFirstQuestion = currentQuestionIndex == 0
        if (skipAnswerFailedOnFirstQuestion && isFirstQuestion) {
            val intent = Intent(this, LevelFailedActivity::class.java)
            intent.putExtra(LevelFailedActivity.EXTRA_LEVEL, currentLevel)
            intent.putExtra(LevelFailedActivity.EXTRA_FAILED_INDEX, currentQuestionIndex)
            if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
                intent.putExtra(LevelFailedActivity.EXTRA_QUESTION, questions[currentQuestionIndex].questionText)
            }
            levelFailedLauncher.launch(intent)
        } else {
            val intent = Intent(this, AnswerFailedActivity::class.java)
            intent.putExtra(AnswerFailedActivity.EXTRA_LEVEL, currentLevel)
            intent.putExtra(AnswerFailedActivity.EXTRA_FAILED_INDEX, currentQuestionIndex)
            if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
                intent.putExtra(AnswerFailedActivity.EXTRA_QUESTION, questions[currentQuestionIndex].questionText)
            }
            answerFailedLauncher.launch(intent)
        }
    }

    private fun onRevive() {
        // 复活后重置状态，继续当前题目
        resetOptionsState()
        isAnswering = true
        startCountdownTimer()
    }
    
    private fun playCountdownAudio() {
        if (!AudioSettings.isSoundEnabled) return
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer()
            val afd: AssetFileDescriptor = assets.openFd("audio/wav_countdown.wav")
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer?.isLooping = true
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playCorrectAudio() {
        if (!AudioSettings.isSoundEnabled) return
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer()
            val afd: AssetFileDescriptor = assets.openFd("audio/correct.mp3")
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playErrorAudio() {
        if (!AudioSettings.isSoundEnabled) return
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer()
            val afd: AssetFileDescriptor = assets.openFd("audio/erroneous.mp3")
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    private fun startBackgroundMusic() {
        if (!AudioSettings.isMusicEnabled || isBackgroundMusicPlaying) return
        
        try {
            backgroundMediaPlayer = MediaPlayer()
            val afd: AssetFileDescriptor = assets.openFd("audio/audio_study.mp3")
            backgroundMediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            backgroundMediaPlayer?.isLooping = true
            backgroundMediaPlayer?.setVolume(0f, 0f) // Start with volume 0 for fade in
            backgroundMediaPlayer?.prepare()
            backgroundMediaPlayer?.start()
            isBackgroundMusicPlaying = true
            
            // Fade in effect (1 second)
            val fadeInDuration = 1000L
            val fadeInSteps = 20
            val fadeInStepDuration = fadeInDuration / fadeInSteps
            var currentStep = 0
            
            val handler = Handler(Looper.getMainLooper())
            val fadeInRunnable = object : Runnable {
                override fun run() {
                    if (currentStep < fadeInSteps && backgroundMediaPlayer != null) {
                        currentStep++
                        val volume = currentStep.toFloat() / fadeInSteps
                        backgroundMediaPlayer?.setVolume(volume, volume)
                        handler.postDelayed(this, fadeInStepDuration)
                    } else {
                        isBackgroundMusicPlaying = backgroundMediaPlayer != null
                    }
                }
            }
            handler.post(fadeInRunnable)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopBackgroundMusic(immediate: Boolean = false) {
        if (backgroundMediaPlayer == null) return
        if (immediate) {
            releaseBackgroundMediaPlayer()
            return
        }
        
        // Fade out effect (1 second)
        val fadeOutDuration = 1000L
        val fadeOutSteps = 20
        val fadeOutStepDuration = fadeOutDuration / fadeOutSteps
        var currentStep = 0
        
        val handler = Handler(Looper.getMainLooper())
        val fadeOutRunnable = object : Runnable {
            override fun run() {
                if (currentStep < fadeOutSteps && backgroundMediaPlayer != null) {
                    currentStep++
                    val volume = 1f - (currentStep.toFloat() / fadeOutSteps)
                    backgroundMediaPlayer?.setVolume(volume, volume)
                    handler.postDelayed(this, fadeOutStepDuration)
                } else {
                    releaseBackgroundMediaPlayer()
                }
            }
        }
        handler.post(fadeOutRunnable)
    }
    
    private fun releaseBackgroundMediaPlayer() {
        backgroundMediaPlayer?.stop()
        backgroundMediaPlayer?.release()
        backgroundMediaPlayer = null
        isBackgroundMusicPlaying = false
    }

    private fun setupOptionClickListeners() {
        binding.optionA.setOnClickListener { onOptionSelected("A") }
        binding.optionB.setOnClickListener { onOptionSelected("B") }
        binding.optionC.setOnClickListener { onOptionSelected("C") }
        binding.optionD.setOnClickListener { onOptionSelected("D") }
    }

    private fun onOptionSelected(selectedAnswer: String) {
        if (questions.isEmpty() || !isAnswering) return
        isAnswering = false
        stopCountdownTimer()
        releaseMediaPlayer()
        stopBackgroundMusic(true)

        val question = questions[currentQuestionIndex]
        val isCorrect = selectedAnswer == question.answerIndex
        val questionNumber = (currentQuestionIndex % 3) + 1
        val answerEventParams = mapOf(
            "LevelNumber" to currentLevel,
            "QuestionNumber" to questionNumber
        )

        if (isCorrect) {
            ReportDataManager.reportData("Answer_correct", answerEventParams)
        } else {
            ReportDataManager.reportData("Answer_wrong", answerEventParams)
        }

        // 显示选中选项的反馈
        showOptionFeedback(selectedAnswer, isCorrect)

        // 1秒后处理结果
        handler.postDelayed({
            if (isCorrect) {
                // 答对了
                correctAnswerCount++
                val requiredCorrect = repository.getCorrectAnswersToPass()
                
                if (correctAnswerCount >= requiredCorrect) {
                    // 答对3题，关卡通过，解锁下一关
                    lifecycleScope.launch {
                        repository.setHighestPassedLevel(currentLevel)
                        val isRewardLevel = repository.isRewardLevel(currentLevel)
                        android.util.Log.d("AnswerQuestionActivity", "Level passed: $currentLevel, isRewardLevel: $isRewardLevel")
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_PASSED_LEVEL, currentLevel)
                            putExtra(EXTRA_IS_REWARD_LEVEL, isRewardLevel)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    // 继续答题
                    currentQuestionIndex++
                    if (currentQuestionIndex < questions.size) {
                        resetOptionsState()
                        displayCurrentQuestion()
                        isAnswering = true
                        startCountdownTimer()
                        startBackgroundMusic()
                    } else {
                        // 题目用完但还没答对3题，关卡失败
                        navigateToFailedScreen()
                    }
                }
            } else {
                // 答错了，跳转到失败页面
                navigateToFailedScreen()
            }
        }, 1000)
    }

    private fun showOptionFeedback(selectedAnswer: String, isCorrect: Boolean) {
        val (optionLayout, optionText, optionIcon) = when (selectedAnswer) {
            "A" -> Triple(binding.optionA, binding.optionAText, binding.optionAIcon)
            "B" -> Triple(binding.optionB, binding.optionBText, binding.optionBIcon)
            "C" -> Triple(binding.optionC, binding.optionCText, binding.optionCIcon)
            "D" -> Triple(binding.optionD, binding.optionDText, binding.optionDIcon)
            else -> return
        }

        if (isCorrect) {
            optionLayout.setBackgroundResource(R.drawable.bg_answer_option_correct)
            optionText.setTextColor(Color.parseColor("#00D047"))
            optionIcon.setImageResource(R.drawable.svg_select_true)
            playCorrectAudio()
        } else {
            optionLayout.setBackgroundResource(R.drawable.bg_answer_option_wrong)
            optionText.setTextColor(Color.parseColor("#FF8220"))
            optionIcon.setImageResource(R.drawable.svg_select_false)
            playErrorAudio()
        }
        optionIcon.visibility = View.VISIBLE
    }

    private fun resetOptionsState() {
        val options = listOf(
            Triple(binding.optionA, binding.optionAText, binding.optionAIcon),
            Triple(binding.optionB, binding.optionBText, binding.optionBIcon),
            Triple(binding.optionC, binding.optionCText, binding.optionCIcon),
            Triple(binding.optionD, binding.optionDText, binding.optionDIcon)
        )
        for ((layout, text, icon) in options) {
            layout.visibility = View.VISIBLE
            layout.alpha = 1f
            layout.setBackgroundResource(R.drawable.bg_answer_card_item)
            text.setTextColor(Color.parseColor("#333333"))
            icon.visibility = View.GONE
        }
    }

    private fun loadQuestions() {
        lifecycleScope.launch {
            questions = repository.getQuestionsByLevelList(currentLevel)
            if (questions.isNotEmpty()) {
                currentQuestionIndex = 0
                displayCurrentQuestion()
                startCountdownTimer()
            }
        }
    }

    private fun displayCurrentQuestion() {
        if (questions.isEmpty()) return

        val question = questions[currentQuestionIndex]
        val totalQuestions = questions.size
        val questionNumber = (currentQuestionIndex % 3) + 1

        ReportDataManager.reportData(
            "Question_Show",
            mapOf(
                "LevelNumber" to currentLevel,
                "QuestionNumber" to questionNumber
            )
        )

        // 更新进度显示 (当前题目/总题目数)
        binding.textProgress.text = "${currentQuestionIndex + 1}/$totalQuestions"

        // 显示问题和选项
        binding.textQuestion.text = question.questionText
        binding.optionAText.text = question.optionA
        binding.optionBText.text = question.optionB
        binding.optionCText.text = question.optionC
        binding.optionDText.text = question.optionD
    }

    private fun updateCountdown(seconds: Int) {
        val text = getString(R.string.answer_countdown_template, seconds)
        val spannable = SpannableString(text)
        val numberString = seconds.toString()
        val numberStart = text.indexOf(numberString)
        if (numberStart >= 0) {
            val numberEnd = numberStart + numberString.length
            spannable.setSpan(
                AbsoluteSizeSpan(18, true),
                numberStart,
                numberEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.textCountdown.text = spannable
    }

    private fun loadPropsCount() {
        lifecycleScope.launch {
            repository.initUserPropsIfNeeded()
            val coins = repository.getCoins()
            val timePropCount = repository.getTimePropCount()
            val eraserPropCount = repository.getEraserPropCount()
            binding.textCoin.text = coins.toString()
            binding.propsACount.text = timePropCount.toString()
            binding.propsBCount.text = eraserPropCount.toString()
        }
    }
}
