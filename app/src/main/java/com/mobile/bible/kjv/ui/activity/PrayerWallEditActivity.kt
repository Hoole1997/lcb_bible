package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.ui.dialog.PrayerVisibleBottomDialog
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityPrayerWallEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.corekit.core.report.ReportDataManager

class PrayerWallEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrayerWallEditBinding
    private lateinit var repository: KjvRepository
    private var selectedVisibleScope = PrayerVisibleBottomDialog.SCOPE_ANYONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_prayer_wall_edit)
        repository = KjvRepository(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.isAppearanceLightStatusBars = true

        val toolbarLp = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
        val buttonLp = binding.buttonContinue.layoutParams as ViewGroup.MarginLayoutParams
        val baseTopMargin = toolbarLp.topMargin
        val baseBottomMargin = buttonLp.bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbarLp.topMargin = baseTopMargin + sys.top
            buttonLp.bottomMargin = baseBottomMargin + sys.bottom
            binding.toolbar.layoutParams = toolbarLp
            binding.buttonContinue.layoutParams = buttonLp
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        binding.back.setOnClickListener { finish() }
        binding.rowVisibleTo.setOnClickListener {
            PrayerVisibleBottomDialog.show(this, selectedVisibleScope)
        }

        supportFragmentManager.setFragmentResultListener(
            PrayerVisibleBottomDialog.REQUEST_KEY,
            this
        ) { _, bundle ->
            selectedVisibleScope = bundle.getString(
                PrayerVisibleBottomDialog.RESULT_SELECTED_SCOPE,
                PrayerVisibleBottomDialog.SCOPE_ANYONE
            ) ?: PrayerVisibleBottomDialog.SCOPE_ANYONE
            updateVisibleToText()
        }

        val maxLength = 200
        updateCharacterLimitText(0, maxLength)
        updateVisibleToText()

        binding.inputName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                updateContinueButtonState()
            }
        })

        binding.inputContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s ?: return
                if (text.length > maxLength) {
                    text.delete(maxLength, text.length)
                }
                updateCharacterLimitText(text.length, maxLength)
                updateContinueButtonState()
            }
        })

        updateContinueButtonState()

        binding.buttonContinue.setOnClickListener {
            ReportDataManager.reportData("PrayerWall_Continue_Click", emptyMap())

            savePrayerAndContinue()
        }
    }

    private fun updateCharacterLimitText(current: Int, max: Int) {
        binding.characterLimit.text = getString(R.string.prayer_wall_edit_character_limit, current, max)
    }

    private fun updateContinueButtonState() {
        val hasName = binding.inputName.text?.isNotBlank() == true
        val hasContent = binding.inputContent.text?.isNotBlank() == true
        val enabled = hasName && hasContent
        binding.buttonContinue.isEnabled = enabled
        val bgRes = if (enabled) {
            R.drawable.bg_player_edit_continue_enable
        } else {
            R.drawable.bg_player_edit_continue_disable
        }
        binding.buttonContinue.setBackgroundResource(bgRes)
    }

    private fun updateVisibleToText() {
        binding.textVisibleToValue.text = if (selectedVisibleScope == PrayerVisibleBottomDialog.SCOPE_JUST_ME) {
            getString(R.string.prayer_wall_edit_visible_just_me)
        } else {
            getString(R.string.prayer_wall_edit_visible_anyone)
        }
    }

    private fun savePrayerAndContinue() {
        val username = binding.inputName.text?.toString()?.trim().orEmpty()
        val content = binding.inputContent.text?.toString()?.trim().orEmpty()
        if (username.isBlank() || content.isBlank()) return

        binding.buttonContinue.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.saveMyPrayer(
                        username = username,
                        content = content,
                        visibility = selectedVisibleScope
                    )
                }
                startActivity(Intent(this@PrayerWallEditActivity, PlayerCreateLoadingActivity::class.java))
                finish()
            } finally {
                if (!isFinishing && !isDestroyed) {
                    binding.buttonContinue.isEnabled = true
                    updateContinueButtonState()
                }
            }
        }
    }
}
