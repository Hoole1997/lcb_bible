package com.mobile.bible.kjv.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.util.Log
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.ui.activity.AnswerQuestionActivity
import com.mobile.bible.kjv.ui.adapter.StudyLevelAdapter
import com.mobile.bible.kjv.ui.adapter.StudyLevelSide
import com.mobile.bible.kjv.ui.adapter.StudyLevelUiModel
import com.mobile.bible.kjv.ui.dialog.LotteryDialog
import com.mobile.bible.kjv.ui.dialog.LotteryType
import com.mobile.bible.kjv.R
import kotlinx.coroutines.launch
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import net.corekit.core.report.ReportDataManager

class StudyFragment : Fragment() {

    private lateinit var repository: KjvRepository
    private lateinit var adapter: StudyLevelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCoin: TextView
    private var isInitialLoad: Boolean = true

    // Lottery prize types (basic types only)
    private val lotteryPrizeTypes = listOf(
        LotteryType.COIN,
        LotteryType.ERASER,
        LotteryType.DELAY,
        LotteryType.COIN_MORE,
        LotteryType.ERASER_DELAY
    )

    // Store pending lottery for 2x claim
    private var pendingLotteryType: LotteryType? = null
    private var pendingLotteryAmount: Int = 0

    private val answerQuestionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("StudyFragment", "Activity result received: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val isRewardLevel = result.data?.getBooleanExtra(AnswerQuestionActivity.EXTRA_IS_REWARD_LEVEL, false) ?: false
            Log.d("StudyFragment", "isRewardLevel=$isRewardLevel")
            if (isRewardLevel) {
                showLotteryDialog()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_study, container, false)
        repository = KjvRepository(requireContext())
        recyclerView = root.findViewById(R.id.rcy_study)
        tvCoin = root.findViewById(R.id.tv_coin)
        setupLevelList()
        return root
    }

    override fun onResume() {
        super.onResume()
        loadLevels()
        loadCoins()
    }

    private fun setupLevelList() {
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        adapter = StudyLevelAdapter { item ->
            if (item.unlocked) {
                ReportDataManager.reportData("Level_Click", mapOf("LevelNumber" to (item.level)))

                val context = requireContext()
                val intent = Intent(context, AnswerQuestionActivity::class.java)
                intent.putExtra(AnswerQuestionActivity.EXTRA_LEVEL, item.level)
                answerQuestionLauncher.launch(intent)
            }
        }
        recyclerView.adapter = adapter

        loadLevels()
    }

    private fun loadLevels() {
        lifecycleScope.launch {
            val levelCount = repository.getLevelCount()
            val totalLevels = if (levelCount > 0) levelCount else 10
            val highestPassedLevel = repository.getHighestPassedLevel()
            val currentLevel = highestPassedLevel + 1
            val levels = createLevels(totalLevels = totalLevels, highestPassedLevel = highestPassedLevel)
            adapter.submit(levels)

            // 仅首次加载时滑动到当前进行中的关卡位置，后续刷新保持用户当前滚动位置
            if (isInitialLoad) {
                isInitialLoad = false
                // 由于 reverseLayout = true，position 0 对应 level 1
                val currentLevelPosition = currentLevel - 1
                recyclerView.post {
                    (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        currentLevelPosition,
                        recyclerView.height / 3
                    )
                }
            }
        }
    }

    private fun createLevels(totalLevels: Int, highestPassedLevel: Int): List<StudyLevelUiModel> {
        return (1..totalLevels).map { level ->
            val side = if (level % 2 == 1) StudyLevelSide.LEFT else StudyLevelSide.RIGHT
            val isPassed = level <= highestPassedLevel
            val isCurrent = level == highestPassedLevel + 1
            val unlocked = isPassed || isCurrent
            val label = if (unlocked) level.toString() else null
            StudyLevelUiModel(
                level = level,
                side = side,
                unlocked = unlocked,
                isCurrent = isCurrent,
                iconRes = null,
                label = label,
                showGift = shouldShowGift(level)
            )
        }
    }

    private fun shouldShowGift(level: Int): Boolean {
        return repository.isRewardLevel(level)
    }

    private fun loadCoins() {
        lifecycleScope.launch {
            repository.initUserPropsIfNeeded()
            val coins = repository.getCoins()
            tvCoin.text = coins.toString()
        }
    }

    private fun showLotteryDialog() {
        Log.d("StudyFragment", "showLotteryDialog called")
        val lotteryType = lotteryPrizeTypes.random()
        val amount = getAmountForLotteryType(lotteryType)
        Log.d("StudyFragment", "Lottery type: $lotteryType, amount: $amount")
        
        pendingLotteryType = lotteryType
        pendingLotteryAmount = amount

        LotteryDialog.show(requireActivity(), lotteryType, amount, object : LotteryDialog.OnLotteryListener {
            override fun onConfirm(coinAmount: Int) {
                claimReward(lotteryType, amount)
            }

            override fun onClaim2x(coinAmount: Int) {
                showRewardedAdFor2x()
            }

            override fun onDismiss() {
                pendingLotteryType = null
                pendingLotteryAmount = 0
            }
        })
    }

    private fun getAmountForLotteryType(type: LotteryType): Int {
        return when (type) {
            LotteryType.COIN -> 100
            LotteryType.COIN_MORE -> 150
            LotteryType.ERASER -> 2
            LotteryType.DELAY -> 2
            LotteryType.ERASER_DELAY -> 1
            else -> 100
        }
    }

    private fun claimReward(lotteryType: LotteryType, amount: Int) {
        lifecycleScope.launch {
            when (lotteryType) {
                LotteryType.COIN, LotteryType.COIN_MORE -> {
                    repository.addCoins(amount)
                }
                LotteryType.ERASER, LotteryType.ERASER_DOUBLE -> {
                    repository.addEraserProp(amount)
                }
                LotteryType.DELAY, LotteryType.DELAY_DOUBLE -> {
                    repository.addTimeProp(amount)
                }
                LotteryType.ERASER_DELAY, LotteryType.ERASER_DELAY_DOUBLE -> {
                    repository.addTimePropAndEraserProp(amount)
                }
                LotteryType.COIN_DOUBLE, LotteryType.COIN_MORE_DOUBLE -> {
                    repository.addCoins(amount)
                }
            }
            loadCoins()
            pendingLotteryType = null
            pendingLotteryAmount = 0
        }
    }

    private fun showRewardedAdFor2x() {
        val originalType = pendingLotteryType ?: return
        val originalAmount = pendingLotteryAmount
        val doubleAmount = originalAmount * 2
        val doubleType = getDoubleType(originalType)

        lifecycleScope.launch {
            var rewardEarned = false
            val result = AdShowExt.showRewardedAd(
                activity = requireActivity(),
                onRewardEarned = { rewardEarned = true }
            )
            if (rewardEarned) {
                // Show the double version dialog
                showDoubledLotteryDialog(doubleType, doubleAmount)
            } else {
                // Ad failed, just claim the original reward
                claimReward(originalType, originalAmount)
            }
        }
    }

    private fun showDoubledLotteryDialog(doubleType: LotteryType, doubleAmount: Int) {
        LotteryDialog.show(requireActivity(), doubleType, doubleAmount, object : LotteryDialog.OnLotteryListener {
            override fun onConfirm(coinAmount: Int) {
                claimReward(doubleType, doubleAmount)
            }

            override fun onClaim2x(coinAmount: Int) {
                // Should not be called for double type, but handle just in case
                claimReward(doubleType, doubleAmount)
            }

            override fun onDismiss() {
                // User dismissed, still give the reward
                claimReward(doubleType, doubleAmount)
            }
        })
    }

    private fun getDoubleType(type: LotteryType): LotteryType {
        return when (type) {
            LotteryType.COIN -> LotteryType.COIN_DOUBLE
            LotteryType.COIN_MORE -> LotteryType.COIN_MORE_DOUBLE
            LotteryType.ERASER -> LotteryType.ERASER_DOUBLE
            LotteryType.DELAY -> LotteryType.DELAY_DOUBLE
            LotteryType.ERASER_DELAY -> LotteryType.ERASER_DELAY_DOUBLE
            else -> type
        }
    }
}
