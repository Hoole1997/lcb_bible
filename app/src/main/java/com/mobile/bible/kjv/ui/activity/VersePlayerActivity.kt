package com.mobile.bible.kjv.ui.activity

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
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.ViewGroup
import android.widget.Toast
import com.mobile.bible.kjv.ui.vm.PlayerViewModel
import java.util.Locale


import android.media.MediaPlayer
import java.io.File
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import kotlinx.coroutines.MainScope

class VersePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var viewModel: PlayerViewModel
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var isTtsReady = false
    private var isPrepared = false
    private var isPlaying = false
    private var progressJob: kotlinx.coroutines.Job? = null


    companion object {
        const val EXTRA_STEP_ID = "extra_step_id"
        const val EXTRA_TRACK_TASK_COMPLETION = "extra_track_task_completion"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
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
                    // Check if content changed
                    if (state.stepId != lastStepId && lastStepId != null) {
                        // Stop current playback
                        stopPlayback()
                        // Update lastStepId
                        lastStepId = state.stepId
                        // Trigger new synthesis
                        startSynthesis()
                    } else if (lastStepId == null) {
                         lastStepId = state.stepId
                    }
                
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
                    
                    // If we have a real duration from MediaPlayer, don't overwrite it with estimate
                    if (!isPrepared) {
                        val estimatedTotalMs = getEstimatedDuration(state.quoteText)
                        binding.timeTotal.text = formatDuration(estimatedTotalMs)
                    }
                    
                    if (!isPlaying) {
                        binding.progressBar.progress = state.progress
                        if (binding.timeCurrent.text.isEmpty() || binding.timeCurrent.text == "00:00") {
                             binding.timeCurrent.text = "00:00"
                        }
                    }
                    binding.quote.text = state.quoteText
                }
            }
        }

        binding.back.setOnClickListener { finish() }

        binding.versePlayerShare.setOnClickListener {
            val pkg = "com.kjv.bible.audio.verse.read.study.tool"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            intent.setPackage("com.android.vending")
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
            }
        }

        initTts()
        binding.versePlayerPlay.setOnClickListener {
            togglePlay()
        }
        
        binding.versePlayerLast.setOnClickListener {
            showAdAndNavigate {
                viewModel.previousTask()
            }
        }
        
        
        binding.versePlayerNext.setOnClickListener {
            showAdAndNavigate {
                viewModel.nextTask()
            }
        }
        
        binding.versePlayerMode.setOnClickListener {
             toggleLoopMode()
        }
        
        // Ensure progress bar is visible (MediaPlayer handles it reliably)
        binding.progressBar.visibility = android.view.View.VISIBLE
    }
    
    private fun toggleLoopMode() {
        currentLoopMode = if (currentLoopMode == LoopMode.LIST_LOOP) {
            LoopMode.SINGLE_LOOP
        } else {
            LoopMode.LIST_LOOP
        }
        
        val iconRes = if (currentLoopMode == LoopMode.LIST_LOOP) {
            R.drawable.svg_verse_player_mode
        } else {
            R.drawable.svg_single_loop
        }
        binding.versePlayerMode.setImageResource(iconRes)
        
        val toastMsg = if (currentLoopMode == LoopMode.LIST_LOOP) "Loop List" else "Loop Single"
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }
    
    private var lastStepId: Int? = null

    private fun getEstimatedDuration(text: String): Long {
        // Approx 60ms per char
        return text.length * 60L
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS Language not supported", Toast.LENGTH_SHORT).show()
                } else {
                    isTtsReady = true
                    startSynthesis()
                }
            } else {
                val errorMsg = "TTS Initialization failed with error: $status"
                android.util.Log.e("VersePlayerActivity", errorMsg)
                showTtsErrorDialog()
            }
        }
        setupProgressListener()
    }

    private fun showTtsErrorDialog() {
        runOnUiThread {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("TTS Initialization Failed")
                    .setMessage("Text-to-Speech engine is not available or failed to initialize. Please check your system TTS settings.")
                    .setPositiveButton("Settings") { _, _ ->
                        try {
                            val intent = Intent()
                            intent.action = "com.android.settings.TTS_SETTINGS"
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Cannot open TTS settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                android.util.Log.d("VersePlayerActivity", "TTS Synthesis Started")
            }

            override fun onDone(utteranceId: String?) {
                android.util.Log.d("VersePlayerActivity", "TTS Synthesis Done")
                runOnUiThread {
                    prepareMediaPlayer()
                }
            }

            override fun onError(utteranceId: String?) {
                android.util.Log.e("VersePlayerActivity", "TTS Synthesis Error: $utteranceId")
                runOnUiThread {
                    Toast.makeText(this@VersePlayerActivity, "Error preparing audio", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun startSynthesis() {
        val text = viewModel.uiState.value.quoteText
        if (text.isEmpty()) return

        try {
            audioFile = File.createTempFile("tts_audio", ".wav", cacheDir)
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synthesis")
            tts?.synthesizeToFile(text, params, audioFile, "synthesis")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create audio file", Toast.LENGTH_SHORT).show()
        }
    }

    private enum class LoopMode {
        LIST_LOOP, SINGLE_LOOP
    }

    private var currentLoopMode = LoopMode.LIST_LOOP

    private fun prepareMediaPlayer() {
        try {
            if (audioFile == null || !audioFile!!.exists()) return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile!!.absolutePath)
                prepare()
                setOnCompletionListener {
                    // Check loop mode
                    if (currentLoopMode == LoopMode.SINGLE_LOOP) {
                        // Replay immediately without ad
                        start()
                        return@setOnCompletionListener
                    }
                    
                    // List Loop: Default behavior (Next task with ad)
                    this@VersePlayerActivity.isPlaying = false
                    binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_play)
                    binding.progressBar.progress = 100
                    binding.timeCurrent.text = binding.timeTotal.text
                    progressJob?.cancel()

                    // Check if we need to track task completion
                    if (intent.getBooleanExtra(EXTRA_TRACK_TASK_COMPLETION, false)) {
                        val currentStepId = viewModel.uiState.value.stepId
                        if (currentStepId != null) {
                            viewModel.markStepAsComplete(currentStepId)
                            Toast.makeText(this@VersePlayerActivity, "Daily task completed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    // Auto-advance to next task in list loop mode
                    showAdAndNavigate {
                        viewModel.nextTask()
                    }
                }
            }

            isPrepared = true
            binding.timeTotal.text = formatDuration(mediaPlayer?.duration?.toLong() ?: 0L)
            binding.timeCurrent.text = "00:00"
            
            // Auto start playing only if activity is in foreground
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mediaPlayer?.start()
                isPlaying = true
                binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_pause)
                startProgressUpdater()
            }

        } catch (e: Exception) {
            android.util.Log.e("VersePlayerActivity", "MediaPlayer prepare failed", e)
        }
    }

    private fun togglePlay() {
        if (!isTtsReady) {
             Toast.makeText(this, "Initializing TTS...", Toast.LENGTH_SHORT).show()
             return
        }
        
        if (!isPrepared) {
            Toast.makeText(this, "Preparing audio, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
                progressJob?.cancel()
                binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_play)
            } else {
                mp.start()
                isPlaying = true
                binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_pause)
                startProgressUpdater()
            }
        }
    }
    
    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                if (total > 0) {
                    val progress = (current.toFloat() / total * 100).toInt()
                    binding.progressBar.progress = progress
                    binding.timeCurrent.text = formatDuration(current.toLong())
                }
                kotlinx.coroutines.delay(200) // Update every 200ms
            }
        }
    }

    private fun stopPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
            
            isPlaying = false
            isPrepared = false
            progressJob?.cancel()
            
            binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_play)
            binding.progressBar.progress = 0
            binding.timeCurrent.text = "00:00"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAdAndNavigate(action: () -> Unit) {
        // Pause playback before showing ad
        if(isPlaying) {
             togglePlay()
        }
        
        // Toast.makeText(this, "Loading Ad...", Toast.LENGTH_SHORT).show()
        MainScope().launch {
            var rewardEarned = false
            val result = AdShowExt.showRewardedAd(
                activity = this@VersePlayerActivity,
                onRewardEarned = { rewardEarned = true }
            )
            if (rewardEarned) {
                action()
            } else if (result is AdResult.Failure) {
                action()
            } else {
                Toast.makeText(this@VersePlayerActivity, "You need to watch the ad to proceed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPlaying = false
            progressJob?.cancel()
            binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_play)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPlaying = false
            progressJob?.cancel()
            binding.versePlayerPlay.setImageResource(R.drawable.svg_verse_player_play)
        }
    }

    override fun onDestroy() {
        progressJob?.cancel()
        mediaPlayer?.release()
        audioFile?.delete()
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
