package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobile.bible.kjv.data.entity.MyPrayerEntity
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.ui.adapter.MyPostListItem
import com.mobile.bible.kjv.ui.adapter.MyPostsAdapter
import com.mobile.bible.kjv.ui.dialog.DeletePrayerDialog
import com.mobile.bible.kjv.ui.dialog.PrayerEditMenuBottomDialog
import com.mobile.bible.kjv.ui.dialog.PrayerVisibleBottomDialog
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityMyPostsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.corekit.core.report.ReportDataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPostsBinding
    private lateinit var repository: KjvRepository
    private var selectedPrayerIdForMenu: Long? = null
    private var isShowingStickyAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_posts)
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

        binding.back.setOnClickListener { finish() }
        binding.buttonPost.setOnClickListener {
            startActivity(Intent(this, PrayerWallEditActivity::class.java))
        }

        binding.rcyMyPosts.layoutManager = LinearLayoutManager(this)
        val adapter = MyPostsAdapter()
        adapter.setOnMoreClickListener { item ->
            selectedPrayerIdForMenu = item.id
            PrayerEditMenuBottomDialog.show(
                activity = this,
                visibleText = item.visibilityText
            )
        }
        adapter.setOnStickyClickListener { item ->
            ReportDataManager.reportData("StickyPost_Click", emptyMap())

            showStickyRewardThenFinish(item.id)
        }
        binding.rcyMyPosts.adapter = adapter
        setupResultListeners()
        collectMyPrayers(adapter)
    }

    private fun setupResultListeners() {
        supportFragmentManager.setFragmentResultListener(
            PrayerEditMenuBottomDialog.REQUEST_KEY,
            this
        ) { _, bundle ->
            val prayerId = selectedPrayerIdForMenu ?: return@setFragmentResultListener
            when (bundle.getString(PrayerEditMenuBottomDialog.RESULT_ACTION)) {
                PrayerEditMenuBottomDialog.ACTION_STICKY_POST -> {
                    showStickyRewardThenFinish(prayerId)
                }
                PrayerEditMenuBottomDialog.ACTION_DELETE -> {
                    DeletePrayerDialog.show(this)
                }
                else -> Unit
            }
        }

        supportFragmentManager.setFragmentResultListener(
            PrayerVisibleBottomDialog.REQUEST_KEY,
            this
        ) { _, bundle ->
            val selectedScope = bundle.getString(
                PrayerVisibleBottomDialog.RESULT_SELECTED_SCOPE,
                PrayerVisibleBottomDialog.SCOPE_ANYONE
            ) ?: PrayerVisibleBottomDialog.SCOPE_ANYONE
            val prayerId = selectedPrayerIdForMenu ?: return@setFragmentResultListener
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    repository.updateMyPrayerVisibility(prayerId, selectedScope)
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            DeletePrayerDialog.REQUEST_KEY,
            this
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(DeletePrayerDialog.RESULT_CONFIRMED, false)
            val prayerId = selectedPrayerIdForMenu ?: return@setFragmentResultListener
            if (!confirmed) return@setFragmentResultListener
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    repository.deleteMyPrayerById(prayerId)
                }
                selectedPrayerIdForMenu = null
            }
        }
    }

    private fun showStickyRewardThenFinish(prayerId: Long) {
        if (isShowingStickyAd) return
        isShowingStickyAd = true
        lifecycleScope.launch {
            try {
                var rewardEarned = false
                val adResult = AdShowExt.showRewardedAd(
                    activity = this@MyPostsActivity,
                    onRewardEarned = { rewardEarned = true }
                )
                val adSucceeded = rewardEarned || adResult !is AdResult.Failure
                if (adSucceeded) {
                    withContext(Dispatchers.IO) {
                        repository.stickyMyPrayerToTop(prayerId)
                    }
                    finish()
                }
            } finally {
                isShowingStickyAd = false
            }
        }
    }

    private fun collectMyPrayers(adapter: MyPostsAdapter) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getMyPrayersFlow().collect { prayers ->
                    val items = prayers.map { it.toUiItem() }
                    if (items.isEmpty()) {
                        binding.rcyMyPosts.visibility = View.GONE
                        binding.emptyContent.visibility = View.VISIBLE
                    } else {
                        binding.rcyMyPosts.visibility = View.VISIBLE
                        binding.emptyContent.visibility = View.GONE
                        adapter.submitItems(items)
                    }
                }
            }
        }
    }

    private fun MyPrayerEntity.toUiItem(): MyPostListItem {
        val dateText = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date(createdAt))
        val visibilityText = if (visibility == PrayerVisibleBottomDialog.SCOPE_JUST_ME) {
            getString(R.string.prayer_wall_edit_visible_just_me)
        } else {
            getString(R.string.prayer_wall_edit_visible_anyone)
        }
        return MyPostListItem(
            id = id,
            username = username,
            content = content,
            createdAtText = dateText,
            visibilityText = visibilityText,
            visibilityScope = visibility
        )
    }
}
