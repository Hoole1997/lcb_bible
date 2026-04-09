package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mobile.bible.kjv.ui.dialog.FloatingWindowBottomDialog
import com.mobile.bible.kjv.ui.fragment.KjvFragment
import com.mobile.bible.kjv.ui.fragment.MeFragment
import com.mobile.bible.kjv.ui.fragment.StudyFragment
import com.mobile.bible.kjv.ui.fragment.TodayFragment
import com.mobile.bible.kjv.ui.fragment.PlayerWallFragment
import com.mobile.bible.kjv.ui.vm.KjvHomeViewModel
import com.mobile.bible.kjv.constant.PrefKeys
import com.mobile.bible.kjv.BuildConfig
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityBibleHomeBinding
import com.remax.base.ext.KvBoolDelegate

class KjvHomeActivity : AppCompatActivity() {

    private lateinit var viewModel: KjvHomeViewModel
    private lateinit var binding: ActivityBibleHomeBinding
    private var hasShownFirstFloatingWindowDialog by KvBoolDelegate(PrefKeys.HAS_SHOWN_FIRST_FLOATING_WINDOW_DIALOG, false)


    private val selectedColor = "#DF9C67".toColorInt()
    private val defaultColor = "#CCCCCC".toColorInt()

    companion object {
        const val EXTRA_START_TAB = "extra_start_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[KjvHomeViewModel::class.java]
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bible_home)

        fitSystemUI()
        initPageUI()
        bindFlow()
    }

    private fun fitSystemUI() {
        val baseBottomPadding = binding.bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomBar)
    }

    private fun initPageUI() {
        binding.tabToday.setOnClickListener { selectTab(Tab.TODAY) }
        binding.tabBible.setOnClickListener { selectTab(Tab.BIBLE) }
        binding.tabChat.setOnClickListener { selectTab(Tab.PLAYER_WALL) }
        binding.tabStudy.setOnClickListener { selectTab(Tab.STUDY) }
        binding.tabMe.setOnClickListener { selectTab(Tab.ME) }

        if (BuildConfig.DEBUG) {
            binding.tabToday.setOnLongClickListener {
                startActivity(Intent(this, DebugActivity::class.java))
                true
            }
        }

        val startTab = parseStartTab(intent) ?: Tab.TODAY
        selectTab(startTab)

        // Temporarily disabled: floating window permission dialog during startup
        // if (!hasShownFirstFloatingWindowDialog) {
        //     FloatingWindowBottomDialog.show(this)
        //     hasShownFirstFloatingWindowDialog = true
        // }
    }

    private fun bindFlow() {

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseStartTab(intent)?.let { selectTab(it) }
    }

    private fun resetTabs() {
        binding.imageToday.setColorFilter(defaultColor)
        binding.imageBible.setColorFilter(defaultColor)
        binding.imageChat.setColorFilter(defaultColor)
        binding.imageStudy.setColorFilter(defaultColor)
        binding.imageMe.setColorFilter(defaultColor)
        binding.textToday.setTextColor(defaultColor)
        binding.textBible.setTextColor(defaultColor)
        binding.textChat.setTextColor(defaultColor)
        binding.textStudy.setTextColor(defaultColor)
        binding.textMe.setTextColor(defaultColor)
    }

    private fun selectTab(tab: Tab) {
        resetTabs()
        switchFragment(tab)
        when (tab) {
            Tab.TODAY -> {
                binding.imageToday.setColorFilter(selectedColor)
                binding.textToday.setTextColor(selectedColor)
            }
            Tab.BIBLE -> {
                binding.imageBible.setColorFilter(selectedColor)
                binding.textBible.setTextColor(selectedColor)
            }
            Tab.PLAYER_WALL -> {
                binding.imageChat.setColorFilter(selectedColor)
                binding.textChat.setTextColor(selectedColor)
            }
            Tab.STUDY -> {
                binding.imageStudy.setColorFilter(selectedColor)
                binding.textStudy.setTextColor(selectedColor)
            }
            Tab.ME -> {
                binding.imageMe.setColorFilter(selectedColor)
                binding.textMe.setTextColor(selectedColor)
            }
        }
    }

    fun navigateToTab(tab: Tab) {
        selectTab(tab)
    }

    private fun switchFragment(tab: Tab) {
        binding.contentText.visibility = View.GONE
        val tag = tab.name
        val fm = supportFragmentManager
        val existing = fm.findFragmentByTag(tag)
        val fragment: Fragment = existing ?: when (tab) {
            Tab.TODAY -> TodayFragment()
            Tab.BIBLE -> KjvFragment()
            Tab.PLAYER_WALL -> PlayerWallFragment()
            Tab.STUDY -> StudyFragment()
            Tab.ME -> MeFragment()
        }
        val transaction = fm.beginTransaction()
        fm.fragments.forEach { addedFragment ->
            if (addedFragment.isAdded && !addedFragment.isHidden) {
                transaction.hide(addedFragment)
            }
        }
        if (existing == null) {
            transaction.add(R.id.content_container, fragment, tag)
        } else {
            transaction.show(existing)
        }
        transaction.commit()
    }

    private fun parseStartTab(intent: Intent?): Tab? {
        val tabName = intent?.getStringExtra(EXTRA_START_TAB) ?: return null
        return Tab.entries.firstOrNull { it.name == tabName }
    }

    enum class Tab { TODAY, BIBLE, PLAYER_WALL, STUDY, ME }
}
