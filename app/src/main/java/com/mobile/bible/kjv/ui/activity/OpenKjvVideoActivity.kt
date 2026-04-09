package com.mobile.bible.kjv.ui.activity

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.mobile.bible.kjv.R
import java.io.File

class OpenKjvVideoActivity : AppCompatActivity() {

    private lateinit var videoContainer: FrameLayout
    private lateinit var videoView: VideoView
    private val assetFileName = "video/open_bible.mp4"
    private val cacheFileName = "open_bible.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_bible_video)
        overridePendingTransition(0, 0)
        videoContainer = findViewById(R.id.open_bible_video_container)
        videoView = findViewById(R.id.open_bible_video_view)
        enterFullscreen()
        playOpenVideo()
    }

    override fun onResume() {
        super.onResume()
        enterFullscreen()
    }

    override fun onBackPressed() {
        finishWithoutAnimation()
    }

    private fun playOpenVideo() {
        val videoUri = copyAssetToCacheAndGetUri() ?: run {
            finish()
            return
        }

        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = false
            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            adjustVideoBounds(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
            mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                adjustVideoBounds(width, height)
                enterFullscreen()
            }
            videoView.start()
        }
        videoView.setOnCompletionListener {
            finishWithoutAnimation()
        }
        videoView.setOnErrorListener { _, _, _ ->
            finishWithoutAnimation()
            true
        }
    }

    private fun adjustVideoBounds(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0) return
        val containerWidth = videoContainer.width
        val containerHeight = videoContainer.height
        if (containerWidth <= 0 || containerHeight <= 0) {
            videoContainer.post { adjustVideoBounds(videoWidth, videoHeight) }
            return
        }

        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val containerRatio = containerWidth.toFloat() / containerHeight.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (videoRatio > containerRatio) {
            targetWidth = containerWidth
            targetHeight = (containerWidth / videoRatio).toInt()
        } else {
            targetHeight = containerHeight
            targetWidth = (containerHeight * videoRatio).toInt()
        }

        val params = FrameLayout.LayoutParams(targetWidth, targetHeight)
        params.gravity = android.view.Gravity.CENTER
        videoView.layoutParams = params
    }

    private fun copyAssetToCacheAndGetUri(): Uri? {
        return try {
            val outputFile = File(cacheDir, cacheFileName)
            if (!outputFile.exists() || outputFile.length() == 0L) {
                assets.open(assetFileName).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Uri.fromFile(outputFile)
        } catch (_: Exception) {
            null
        }
    }

    private fun enterFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }
}
