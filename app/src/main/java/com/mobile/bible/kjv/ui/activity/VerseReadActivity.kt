package com.mobile.bible.kjv.ui.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.animation.PathInterpolator
import android.view.ViewGroup
import android.widget.ImageView
import com.mobile.bible.kjv.ui.vm.ReadViewModel
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityVerseReadBinding
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager

class VerseReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerseReadBinding
    private lateinit var viewModel: ReadViewModel
    private var isTaskSwitchAnimating = false
    private var currentTaskOrder: Int = 0
    private var currentCardNumber: Int = 1
    companion object {
        const val EXTRA_STEP_ID = "extra_step_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_verse_read)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[ReadViewModel::class.java]
        val stepIdRaw = intent.getIntExtra(EXTRA_STEP_ID, -1)
        val stepId = if (stepIdRaw == -1) null else stepIdRaw
        val quoteText = intent.getStringExtra("extra_quote_text")
        val trackTitle = intent.getStringExtra("extra_track_title")
        val verseReference = intent.getStringExtra("extra_verse_reference")
        viewModel.setStepId(stepId, quoteText, trackTitle, verseReference)

        val insetsController = WindowInsetsControllerCompat(window, binding.root)
        insetsController.setAppearanceLightStatusBars(false)

        val toolbarLp = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
        val playerLp = binding.playerControls.layoutParams as ViewGroup.MarginLayoutParams
        val baseTopMargin = toolbarLp.topMargin
        val baseBottomMargin = playerLp.bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbarLp.topMargin = baseTopMargin + sys.top
            playerLp.bottomMargin = baseBottomMargin + sys.bottom
            binding.toolbar.layoutParams = toolbarLp
            binding.playerControls.layoutParams = playerLp
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    currentTaskOrder = state.taskOrder
                    currentCardNumber = cardNumberFromTaskOrder(state.taskOrder)
                    binding.taskBg.setImageResource(resolveTaskBackground(state.taskOrder))
                    binding.progressTodayPercent.text = "${state.progressToday}%"
                    binding.progressTodayBar.progress = state.progressToday
                    binding.progressTodaySubtitle.text = state.trackTitle
                    
                    if (state.verseReference.isEmpty()) {
                        binding.versePosition.visibility = android.view.View.GONE
                        binding.imgVersePosition.visibility = android.view.View.GONE
                    } else {
                        binding.versePosition.visibility = android.view.View.VISIBLE
                        binding.imgVersePosition.visibility = android.view.View.VISIBLE
                        binding.versePosition.text = state.verseReference
                    }
                    
                    binding.quote.text = state.quoteText
                }
            }
        }

        binding.back.setOnClickListener { finish() }

        binding.readNext.setOnClickListener {
            val nextCardNumber = cardNumberFromTaskOrder(currentTaskOrder + 1)
            ReportDataManager.reportData("Next_Click", mapOf("cardNumber" to nextCardNumber))

            animateToNextTask()
        }

        binding.readShare.setOnClickListener {
            ReportDataManager.reportData("Share_Click", mapOf("cardNumber" to currentCardNumber))

            val pkg = "com.kjv.bible.audio.verse.read.study.tool"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            intent.setPackage("com.android.vending")
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
            }
        }
    }

    private fun animateToNextTask() {
        if (isTaskSwitchAnimating) return

        isTaskSwitchAnimating = true
        binding.readNext.isEnabled = false

        val pageHeight = binding.root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val contentParent = findViewById<ViewGroup>(android.R.id.content)
        val currentSnapshot = createCurrentPageSnapshot(pageHeight)

        if (currentSnapshot != null) {
            contentParent.addView(currentSnapshot)
        }

        val nextJob = viewModel.nextTask()
        nextJob.invokeOnCompletion {
            runOnUiThread {
                val duration = 260L
                val motionInterpolator = PathInterpolator(0.18f, 0.72f, 0.2f, 1f)
                val distance = pageHeight.toFloat()

                binding.root.animate().cancel()
                currentSnapshot?.animate()?.cancel()

                binding.root.translationY = distance
                currentSnapshot?.translationY = 0f

                ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = duration
                    interpolator = motionInterpolator
                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Float
                        val currentY = -distance * progress
                        val nextY = distance * (1f - progress)
                        currentSnapshot?.translationY = currentY
                        binding.root.translationY = nextY
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            binding.root.translationY = 0f
                            if (currentSnapshot != null) {
                                currentSnapshot.translationY = -distance
                                contentParent.removeView(currentSnapshot)
                            }
                            finishTaskSwitchAnimation()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            binding.root.translationY = 0f
                            if (currentSnapshot != null) {
                                contentParent.removeView(currentSnapshot)
                            }
                            finishTaskSwitchAnimation()
                        }
                    })
                    start()
                }
            }
        }
    }

    private fun resolveTaskBackground(taskOrder: Int): Int {
        return when (taskOrder % 3) {
            0 -> R.mipmap.task_bg_1
            1 -> R.mipmap.task_bg_2
            else -> R.mipmap.task_bg_3
        }
    }

    private fun cardNumberFromTaskOrder(taskOrder: Int): Int {
        return (taskOrder.mod(3)) + 1
    }

    private fun createCurrentPageSnapshot(pageHeight: Int): ImageView? {
        val pageWidth = binding.root.width
        if (pageWidth <= 0 || pageHeight <= 0) return null

        val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.root.draw(canvas)

        return ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun finishTaskSwitchAnimation() {
        isTaskSwitchAnimating = false
        binding.readNext.isEnabled = true
    }
}
