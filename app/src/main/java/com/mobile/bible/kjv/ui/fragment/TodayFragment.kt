package com.mobile.bible.kjv.ui.fragment

import android.graphics.Color
import android.os.Bundle
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
import androidx.recyclerview.widget.RecyclerView
import com.mobile.bible.kjv.ui.activity.KjvHomeActivity
import com.mobile.bible.kjv.ui.adapter.TodayAdapter
import com.mobile.bible.kjv.ui.adapter.TodayItem
import com.mobile.bible.kjv.ui.vm.PlayerWallViewModel
import com.mobile.bible.kjv.ui.vm.TodayViewModel
import com.mobile.bible.kjv.databinding.FragmentTodayBinding
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager

class TodayFragment : Fragment() {
    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TodayViewModel
    private lateinit var playerWallViewModel: PlayerWallViewModel
    private lateinit var adapter: TodayAdapter
    private var toolbarThresholdPx: Int = 0
    private var hasReportedHomeShowInVisibleSession: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TodayViewModel::class.java]
        playerWallViewModel = ViewModelProvider(this)[PlayerWallViewModel::class.java]
        playerWallViewModel.ensureDataReady()
        fitSystemUI()
        initPageUI()
        bindFlow()
        return binding.root
    }

    private fun fitSystemUI() {
        toolbarThresholdPx = dpToPx(100f)
        updateToolbarBackground()
        binding.toadyRcy.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateToolbarBackground()
            }
        })
        binding.toadyRcy.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            binding.toadyRcy.post { updateToolbarBackground() }
        }
        val baseTopPadding = binding.toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, baseTopPadding + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.toolbar)
    }

    private fun initPageUI() {
        binding.toadyRcy.layoutManager = LinearLayoutManager(requireContext())
        adapter = TodayAdapter()
        binding.toadyRcy.adapter = adapter
        adapter.registerAdapterDataObserver(listObserver)

        adapter.onJourneyCardExpanded = { expandedView ->
            val rvLocation = IntArray(2)
            binding.toadyRcy.getLocationOnScreen(rvLocation)
            val rvBottom = rvLocation[1] + binding.toadyRcy.height

            val viewLocation = IntArray(2)
            expandedView.getLocationOnScreen(viewLocation)
            val viewBottom = viewLocation[1] + expandedView.height

            if (viewBottom > rvBottom) {
                val extraPadding = dpToPx(12f)
                binding.toadyRcy.smoothScrollBy(0, viewBottom - rvBottom + extraPadding)
            }
        }
        adapter.onPrayerWallBadgeClick = {
            ReportDataManager.reportData("ViewMore_Click", emptyMap())

            (activity as? KjvHomeActivity)?.navigateToTab(KjvHomeActivity.Tab.PLAYER_WALL)
        }

        adapter.submitItems(
            listOf(
                TodayItem.Fixed,
                TodayItem.TodaysJourney(emptyList()),
                TodayItem.NewsReadingPlans,
//                TodayItem.ListenLearn
            )
        )
        
    }

    private fun bindFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect daily verse
                launch {
                    viewModel.dailyVerse.collect { dailyVerse ->
                        dailyVerse?.let {
                            adapter.updateDailyVerse(it.text, it.getPosition())
                        }
                    }
                }
                
                // Collect today's journey items
                launch {
                    viewModel.todaysJourneyItems.collect { items ->
                        if (items.isNotEmpty()) {
                            adapter.submitItems(
                                listOf(
                                    TodayItem.Fixed,
                                    TodayItem.TodaysJourney(items),
                                    TodayItem.NewsReadingPlans,
                                    // TodayItem.ListenLearn
                                )
                            )
                        }
                    }
                }

                launch {
                    playerWallViewModel.windowState.collect { window ->
                        adapter.updatePrayerWallItems(window.items)
                    }
                }
            }
        }
    }

    private val listObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            binding.toadyRcy.post { updateToolbarBackground() }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            binding.toadyRcy.post { updateToolbarBackground() }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            binding.toadyRcy.post { updateToolbarBackground() }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            binding.toadyRcy.post { updateToolbarBackground() }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            binding.toadyRcy.post { updateToolbarBackground() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure Fixed card re-calculates verse after returning from DebugActivity.
        viewModel.loadDailyVerse()
        binding.toadyRcy.post { updateToolbarBackground() }
        reportHomeShowIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        hasReportedHomeShowInVisibleSession = false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) {
            // Refresh when tab is shown again via hide/show transactions.
            viewModel.loadDailyVerse()
            binding.toadyRcy.post { updateToolbarBackground() }
            reportHomeShowIfNeeded()
        } else if (hidden) {
            hasReportedHomeShowInVisibleSession = false
        }
    }

    override fun onDestroyView() {
        adapter.unregisterAdapterDataObserver(listObserver)
        super.onDestroyView()
        _binding = null
    }

    private fun updateToolbarBackground() {
        val offset = binding.toadyRcy.computeVerticalScrollOffset()
        val fraction = (offset.toFloat() / toolbarThresholdPx).coerceIn(0f, 1f)
        val alpha = (fraction * 255).toInt()
        val r = 255
        val g = 255
        val b = 255
        binding.toolbar.setBackgroundColor(Color.argb(alpha, r, g, b))
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun reportHomeShowIfNeeded() {
        if (hasReportedHomeShowInVisibleSession || isHidden || view == null) return
        ReportDataManager.reportData("Home_Show", emptyMap())
        hasReportedHomeShowInVisibleSession = true
    }

}
