package com.mobile.bible.kjv.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper

class NetAudioPlayer(private val context: Context) {
    enum class State {
        IDLE, PREPARING, READY, PLAYING, PAUSED, BUFFERING, COMPLETED, STOPPED, ERROR, RELEASED
    }

    interface Listener {
        fun onStateChanged(state: State)
        fun onPrepared(durationMs: Int)
        fun onBuffering(percent: Int)
        fun onProgress(positionMs: Int, durationMs: Int)
        fun onCompletion()
        fun onError(what: Int, extra: Int)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var listener: Listener? = null
    private var state: State = State.IDLE
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var ducked = false
    private var resumeOnFocusGain = false
    private var noisyRegistered = false
    private var currentUrl: String? = null
    private var currentHeaders: Map<String, String>? = null
    private var progressIntervalMs = 500L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && state == State.PLAYING) {
                listener?.onProgress(mp.currentPosition, mp.duration)
                mainHandler.postDelayed(this, progressIntervalMs)
            }
        }
    }

    private var maxRetry = 2
    private var retryDelayMs = 2000L
    private var retriesRemaining = 0

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (ducked) setVolume(1f)
                if (resumeOnFocusGain && state == State.PAUSED) resume()
                resumeOnFocusGain = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) {
                    resumeOnFocusGain = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying()) {
                    setVolume(0.2f)
                    ducked = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                if (isPlaying()) pause()
                abandonAudioFocus()
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                if (isPlaying()) pause()
            }
        }
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    fun configureRetry(maxRetry: Int, retryDelayMs: Long) {
        this.maxRetry = maxRetry.coerceAtLeast(0)
        this.retryDelayMs = retryDelayMs.coerceAtLeast(0)
    }

    fun play(url: String, headers: Map<String, String>? = null, startPositionMs: Int = 0, autoStart: Boolean = true) {
        currentUrl = url
        currentHeaders = headers
        retriesRemaining = maxRetry
        ensureMediaPlayer()
        requestAudioFocus()
        registerNoisyReceiver()
        state = State.PREPARING
        listener?.onStateChanged(state)
        val mp = mediaPlayer ?: return
        mp.reset()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        if (headers != null) {
            mp.setDataSource(context, Uri.parse(url), headers)
        } else {
            mp.setDataSource(url)
        }
        mp.setOnPreparedListener {
            state = State.READY
            listener?.onStateChanged(state)
            listener?.onPrepared(it.duration)
            if (startPositionMs > 0) it.seekTo(startPositionMs)
            if (autoStart) startInternal()
        }
        mp.setOnBufferingUpdateListener { _, percent ->
            listener?.onBuffering(percent)
        }
        mp.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                state = State.BUFFERING
                listener?.onStateChanged(state)
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                if (isPlaying()) {
                    state = State.PLAYING
                    listener?.onStateChanged(state)
                }
            }
            false
        }
        mp.setOnCompletionListener {
            stopProgressUpdates()
            state = State.COMPLETED
            listener?.onStateChanged(state)
            listener?.onCompletion()
        }
        mp.setOnErrorListener { _, what, extra ->
            stopProgressUpdates()
            state = State.ERROR
            listener?.onStateChanged(state)
            listener?.onError(what, extra)
            if (shouldRetry(what)) scheduleRetry()
            true
        }
        mp.prepareAsync()
    }

    fun pause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            stopProgressUpdates()
            state = State.PAUSED
            listener?.onStateChanged(state)
        }
    }

    fun resume() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying && (state == State.READY || state == State.PAUSED || state == State.BUFFERING)) {
            requestAudioFocus()
            startInternal()
        }
    }

    fun stop() {
        val mp = mediaPlayer ?: return
        if (state == State.PLAYING || state == State.PAUSED || state == State.BUFFERING || state == State.READY) {
            try {
                mp.stop()
            } catch (_: Throwable) {}
            stopProgressUpdates()
            state = State.STOPPED
            listener?.onStateChanged(state)
        }
        abandonAudioFocus()
        unregisterNoisyReceiver()
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        try {
            mp.seekTo(positionMs)
        } catch (_: Throwable) {}
    }

    fun setLooping(looping: Boolean) {
        mediaPlayer?.isLooping = looping
    }

    fun setPlaybackSpeed(speed: Float) {
        val mp = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams
                mp.playbackParams = params.setSpeed(speed)
            } catch (_: Throwable) {}
        }
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
        if (volume >= 1f) ducked = false
    }

    fun isPlaying(): Boolean {
        val mp = mediaPlayer
        return mp != null && mp.isPlaying
    }

    fun getDuration(): Int {
        val mp = mediaPlayer
        return if (mp != null) mp.duration else 0
    }

    fun getCurrentPosition(): Int {
        val mp = mediaPlayer
        return if (mp != null) mp.currentPosition else 0
    }

    fun release() {
        stopProgressUpdates()
        unregisterNoisyReceiver()
        abandonAudioFocus()
        try {
            mediaPlayer?.release()
        } catch (_: Throwable) {}
        mediaPlayer = null
        state = State.RELEASED
        listener?.onStateChanged(state)
    }

    private fun startInternal() {
        val mp = mediaPlayer ?: return
        try {
            mp.start()
            state = State.PLAYING
            listener?.onStateChanged(state)
            startProgressUpdates()
        } catch (_: Throwable) {}
    }

    private fun ensureMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        mainHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressRunnable)
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        audioFocusRequest = req
        val res = audioManager.requestAudioFocus(req)
        hasAudioFocus = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val req = audioFocusRequest
        if (req != null) {
            audioManager.abandonAudioFocusRequest(req)
        }
        hasAudioFocus = false
        ducked = false
    }

    private fun registerNoisyReceiver() {
        if (noisyRegistered) return
        context.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        noisyRegistered = true
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyRegistered) return
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Throwable) {}
        noisyRegistered = false
    }

    private fun shouldRetry(what: Int): Boolean {
        return retriesRemaining > 0 && (
            what == MediaPlayer.MEDIA_ERROR_IO ||
                what == MediaPlayer.MEDIA_ERROR_TIMED_OUT ||
                what == MediaPlayer.MEDIA_ERROR_UNKNOWN
            )
    }

    private fun scheduleRetry() {
        val url = currentUrl ?: return
        val headers = currentHeaders
        val delay = retryDelayMs
        retriesRemaining -= 1
        mainHandler.postDelayed({ play(url, headers, 0, true) }, delay)
    }
}
