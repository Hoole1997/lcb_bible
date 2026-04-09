package com.mobile.bible.kjv.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobile.bible.kjv.ui.activity.PrayerWallEditActivity
import com.mobile.bible.kjv.ui.activity.MyPostsActivity
import com.mobile.bible.kjv.ui.adapter.PlayerWallAdapter
import com.mobile.bible.kjv.ui.adapter.PlayerWallListItem
import com.mobile.bible.kjv.ui.vm.PlayerWallViewModel
import com.mobile.bible.kjv.ui.vm.PrayerWallItem
import com.mobile.bible.kjv.ui.vm.PrayerWallWindow
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.FragmentPlayerWallBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager

class PlayerWallFragment : Fragment() {

    private var _binding: FragmentPlayerWallBinding? = null
    private val binding get() = _binding!!
    private lateinit var playerWallViewModel: PlayerWallViewModel
    private lateinit var playerWallAdapter: PlayerWallAdapter
    private var latestWindow: PrayerWallWindow? = null
    private val likedCardKeys = mutableSetOf<String>()
    private var countdownSecondsLeft: Int = PlayerWallViewModel.SHIFT_INTERVAL_SECONDS
    private var lastWindowToken: String? = null
    private var windowCycleStartElapsedMs: Long = 0L
    private var isShowingStickyAd = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerWallBinding.inflate(inflater, container, false)
        fitSystemUI()
        initPageUI()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDataLayer()
    }

    private fun fitSystemUI() {
        val baseTopPadding = binding.toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, baseTopPadding + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.toolbar)
    }

    private fun initPageUI() {
        binding.rcyPlayers.layoutManager = LinearLayoutManager(requireContext())
        playerWallAdapter = PlayerWallAdapter()
        playerWallAdapter.setOnCardLikeClickListener { card ->
            ReportDataManager.reportData("Like_Click", emptyMap())

            if (likedCardKeys.add(card.itemKey)) {
                latestWindow?.let { renderWindow(it) }
            }
        }
        playerWallAdapter.setOnTitleStickyClickListener {
            showStickyRewardForWaitingPrayer()
        }
        binding.rcyPlayers.adapter = playerWallAdapter

        binding.actionMyPosts.setOnClickListener {
            ReportDataManager.reportData("MyPosts_Click", emptyMap())

            startActivity(Intent(requireContext(), MyPostsActivity::class.java))
        }

        binding.playerFab.setOnClickListener {
            ReportDataManager.reportData("Write_entry_Click", emptyMap())

            startActivity(Intent(requireContext(), PrayerWallEditActivity::class.java))
        }
    }

    private fun initDataLayer() {
        playerWallViewModel = ViewModelProvider(this)[PlayerWallViewModel::class.java]
        playerWallViewModel.ensureDataReady()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerWallViewModel.windowState.collect { window ->
                        val token = buildWindowToken(window)
                        if (token != lastWindowToken) {
                            countdownSecondsLeft = PlayerWallViewModel.SHIFT_INTERVAL_SECONDS
                            windowCycleStartElapsedMs = SystemClock.elapsedRealtime()
                            lastWindowToken = token
                        }
                        logWindow(window)
                        renderWindow(window)
                    }
                }
                launch {
                    while (isActive) {
                        val currentWindow = latestWindow ?: continue
                        if (currentWindow.items.isEmpty()) continue
                        val elapsedSeconds = ((SystemClock.elapsedRealtime() - windowCycleStartElapsedMs) / 1000L).toInt()
                        val remaining = (PlayerWallViewModel.SHIFT_INTERVAL_SECONDS - elapsedSeconds).coerceAtLeast(0)
                        if (remaining <= 0) {
                            countdownSecondsLeft = 0
                            renderWindow(currentWindow)
                            playerWallViewModel.shiftWindowByOne()
                            delay(80L)
                            continue
                        }
                        if (remaining != countdownSecondsLeft) {
                            countdownSecondsLeft = remaining
                            renderWindow(currentWindow)
                        }
                        delay(1000L)
                    }
                }
            }
        }
    }

    private fun logWindow(window: PrayerWallWindow) {
        Log.d(
            TAG,
            "window start=${window.startIndex}, size=${window.items.size}/${window.windowSize}, interval=${window.intervalSeconds}s"
        )
        window.items.forEachIndexed { index, item ->
            Log.d(
                TAG,
                "[${window.startIndex + index}] user=${item.username}, likes=${item.likes}, content=${item.content}"
            )
        }
    }

    private fun renderWindow(window: PrayerWallWindow) {
        latestWindow = window
        if (window.items.isEmpty()) {
            playerWallAdapter.submitItems(emptyList())
            return
        }

        val first = window.items.first()
        val firstKey = buildItemKey(first)
        val firstLiked = likedCardKeys.contains(firstKey)
        val waitingItems = window.items.drop(1)
        val waitingCount = waitingItems.size
        val hasWaitingLocalPrayer = waitingItems.any { it.localPrayerId != null }
        val listItems = mutableListOf<PlayerWallListItem>(
            PlayerWallListItem.Card(
                itemKey = firstKey,
                userName = first.username,
                content = first.content,
                timeLeft = formatTimeLeft(countdownSecondsLeft),
                likeCount = first.likes + if (firstLiked) 1 else 0,
                isLiked = firstLiked
            ),
            PlayerWallListItem.Title(
                text = getString(R.string.player_wall_waiting_prayers, waitingCount),
                canSticky = hasWaitingLocalPrayer
            )
        )

        waitingItems.forEach { item ->
            listItems.add(
                PlayerWallListItem.PrayerItem(
                    name = item.username,
                    content = item.content
                )
            )
        }

        playerWallAdapter.submitItems(listItems)
    }

    private fun showStickyRewardForWaitingPrayer() {
        if (isShowingStickyAd) return
        isShowingStickyAd = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var rewardEarned = false
                val adResult = AdShowExt.showRewardedAd(
                    activity = requireActivity(),
                    onRewardEarned = { rewardEarned = true }
                )
                val adSucceeded = rewardEarned || adResult !is AdResult.Failure
                if (adSucceeded) {
                    playerWallViewModel.stickyFirstWaitingLocalPrayer()
                }
            } finally {
                isShowingStickyAd = false
            }
        }
    }

    private fun buildItemKey(item: PrayerWallItem): String {
        return item.localPrayerId?.let { "local_$it" } ?: "${item.username}|${item.content.hashCode()}"
    }

    private fun buildWindowToken(window: PrayerWallWindow): String {
        val first = window.items.firstOrNull()
        val firstKey = first?.localPrayerId?.toString() ?: first?.let { "${it.username}|${it.content.hashCode()}" }.orEmpty()
        return "${window.startIndex}|$firstKey|${window.items.size}"
    }

    private fun formatTimeLeft(secondsLeft: Int): String {
        return if (secondsLeft >= 60) {
            getString(R.string.player_wall_time_left_minute)
        } else {
            getString(R.string.player_wall_time_left_seconds, secondsLeft.coerceAtLeast(0))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "PlayerWallFragment"
    }
}
