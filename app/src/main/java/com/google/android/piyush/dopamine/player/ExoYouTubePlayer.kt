package com.google.android.piyush.dopamine.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.piyush.dopamine.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.google.android.piyush.dopamine.utilities.FormatUtils

/**
 * Custom YouTube-style player using NewPipe Extractor + ExoPlayer.
 * Features: custom controls overlay, auto-hide, double-tap seek,
 * speed/quality settings, fullscreen, skip next/prev.
 */
@OptIn(UnstableApi::class)
class ExoYouTubePlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val playerView: PlayerView
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentLoadJob: Job? = null

    private var playerCallback: PlayerCallback? = null
    private var currentVideoId: String? = null

    // Controls overlay views
    private val controlsRoot: View
    private val controlsScrim: View
    private val topBar: View
    private val centerControls: View
    private val bottomBar: View
    private val controlsTitle: TextView
    private val btnSettings: ImageButton
    private val btnSkipPrev: ImageButton
    private val btnRewind: ImageButton
    private val btnPlayPause: ImageButton
    private val btnForward: ImageButton
    private val btnSkipNext: ImageButton
    private val seekBar: SeekBar
    private val textCurrentTime: TextView
    private val textDuration: TextView
    private val btnFullscreen: ImageButton
    private val bufferingIndicator: ProgressBar
    private val doubleTapLeft: View
    private val doubleTapRight: View
    private val seekLeftIcon: ImageView
    private val seekRightIcon: ImageView

    // State
    private var controlsVisible = true
    private var showControlsEnabled = true
    private val hideHandler = Handler(Looper.getMainLooper())
    private val progressHandler = Handler(Looper.getMainLooper())
    private var isSeeking = false
    private var isFullscreen = false
    private var currentSpeed = 1.0f

    // Listeners
    private var onNextListener: (() -> Unit)? = null
    private var onPrevListener: (() -> Unit)? = null
    private var onFullscreenListener: ((Boolean) -> Unit)? = null
    private var onBackListener: (() -> Unit)? = null
    private var onSettingsListener: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = exoPlayer?.isPlaying == true

    val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: 0L

    val duration: Long
        get() = exoPlayer?.duration ?: 0L

    interface PlayerCallback {
        fun onReady() {}
        fun onPlaying() {}
        fun onPaused() {}
        fun onBuffering() {}
        fun onEnded() {}
        fun onError(error: String) {}
    }

    companion object {
        private const val TAG = "ExoYouTubePlayer"
        private const val HIDE_DELAY_MS = 3000L
        private const val PROGRESS_UPDATE_MS = 200L
        private const val SEEK_INCREMENT_MS = 10_000L
    }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)

        // 1. Create PlayerView
        exoPlayer = ExoPlayer.Builder(context).build()
        playerView = PlayerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            player = exoPlayer
            useController = false  // We use our custom controls
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // We handle buffering ourselves
        }
        addView(playerView)

        // 2. Inflate custom controls overlay
        val controlsView = LayoutInflater.from(context)
            .inflate(R.layout.exo_custom_controls, this, false)
        addView(controlsView)

        // 3. Bind views
        controlsRoot = controlsView.findViewById(R.id.controlsRoot)
        controlsScrim = controlsView.findViewById(R.id.controlsScrim)
        topBar = controlsView.findViewById(R.id.topBar)
        centerControls = controlsView.findViewById(R.id.centerControls)
        bottomBar = controlsView.findViewById(R.id.bottomBar)
        controlsTitle = controlsView.findViewById(R.id.controlsTitle)
        btnSettings = controlsView.findViewById(R.id.btnSettings)
        btnSkipPrev = controlsView.findViewById(R.id.btnSkipPrev)
        btnRewind = controlsView.findViewById(R.id.btnRewind)
        btnPlayPause = controlsView.findViewById(R.id.btnPlayPause)
        btnForward = controlsView.findViewById(R.id.btnForward)
        btnSkipNext = controlsView.findViewById(R.id.btnSkipNext)
        seekBar = controlsView.findViewById(R.id.seekBar)
        textCurrentTime = controlsView.findViewById(R.id.textCurrentTime)
        textDuration = controlsView.findViewById(R.id.textDuration)
        btnFullscreen = controlsView.findViewById(R.id.btnFullscreen)
        bufferingIndicator = controlsView.findViewById(R.id.bufferingIndicator)
        doubleTapLeft = controlsView.findViewById(R.id.doubleTapLeft)
        doubleTapRight = controlsView.findViewById(R.id.doubleTapRight)
        seekLeftIcon = controlsView.findViewById(R.id.seekLeftIcon)
        seekRightIcon = controlsView.findViewById(R.id.seekRightIcon)

        // 4. Setup
        setupPlayerListener()
        setupControlButtons()
        setupSeekBar()
        setupGestures()

        // Initially show controls then auto-hide
        showControls()
    }

    // ---- PUBLIC API ----

    fun setCallback(callback: PlayerCallback) {
        this.playerCallback = callback
    }

    fun setTitle(title: String) {
        controlsTitle.text = title
    }

    fun setOnNextListener(listener: () -> Unit) {
        onNextListener = listener
        btnSkipNext.alpha = 1.0f
    }

    fun setOnPrevListener(listener: () -> Unit) {
        onPrevListener = listener
        btnSkipPrev.alpha = 1.0f
    }

    fun setOnFullscreenListener(listener: (Boolean) -> Unit) {
        onFullscreenListener = listener
    }

    fun setOnBackListener(listener: () -> Unit) {
        onBackListener = listener
    }

    fun setOnSettingsListener(listener: () -> Unit) {
        onSettingsListener = listener
    }

    /**
     * Show or hide the custom controls overlay.
     */
    fun setShowControls(show: Boolean) {
        showControlsEnabled = show
        if (!show) {
            hideControlsImmediate()
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen
        btnFullscreen.setImageResource(
            if (fullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun getPlaybackSpeed(): Float = currentSpeed

    /**
     * Switch to a different stream URL (e.g. quality change) while preserving position.
     */
    fun switchStream(streamUrl: String) {
        val pos = exoPlayer?.currentPosition ?: 0L
        val wasPlaying = exoPlayer?.isPlaying == true
        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            seekTo(pos)
            playWhenReady = wasPlaying
        }
    }

    /**
     * Load and auto-play a YouTube video by its ID.
     */
    fun loadVideo(videoId: String) {
        if (videoId == currentVideoId && exoPlayer?.isPlaying == true) return
        currentVideoId = videoId

        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            try {
                playerCallback?.onBuffering()
                showBuffering(true)
                val streamUrl = NewPipeStreamExtractor.extractStreamUrl(videoId)
                if (streamUrl != null) {
                    val mediaItem = MediaItem.fromUri(streamUrl)
                    exoPlayer?.apply {
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                    Log.d(TAG, "Playing video: $videoId")
                } else {
                    Log.e(TAG, "Failed to extract stream for: $videoId")
                    playerCallback?.onError("Failed to extract video stream")
                    showBuffering(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading video: $videoId", e)
                playerCallback?.onError(e.message ?: "Error loading video")
                showBuffering(false)
            }
        }
    }

    fun play() { exoPlayer?.play() }
    fun pause() { exoPlayer?.pause() }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun onResume() {
        startProgressUpdates()
    }

    fun onPause() {
        exoPlayer?.pause()
        stopProgressUpdates()
    }

    fun release() {
        currentLoadJob?.cancel()
        hideHandler.removeCallbacksAndMessages(null)
        stopProgressUpdates()
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
    }

    // ---- PRIVATE IMPLEMENTATION ----

    private fun setupPlayerListener() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        showBuffering(false)
                        playerCallback?.onReady()
                        updateDuration()
                        if (exoPlayer?.playWhenReady == true) {
                            playerCallback?.onPlaying()
                            startProgressUpdates()
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        showBuffering(true)
                        playerCallback?.onBuffering()
                    }
                    Player.STATE_ENDED -> {
                        showBuffering(false)
                        playerCallback?.onEnded()
                        showControls()
                        // Don't auto-hide after video ends
                        hideHandler.removeCallbacksAndMessages(null)
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon()
                if (isPlaying) {
                    playerCallback?.onPlaying()
                    startProgressUpdates()
                    scheduleHideControls()
                } else {
                    playerCallback?.onPaused()
                    stopProgressUpdates()
                    // Don't auto-hide when paused
                    hideHandler.removeCallbacksAndMessages(null)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                showBuffering(false)
                playerCallback?.onError(error.message ?: "Unknown error")
            }
        })
    }

    private fun setupControlButtons() {
        btnPlayPause.setOnClickListener {
            togglePlayPause()
            scheduleHideControls()
        }

        btnRewind.setOnClickListener {
            seekRelative(-SEEK_INCREMENT_MS)
            scheduleHideControls()
        }

        btnForward.setOnClickListener {
            seekRelative(SEEK_INCREMENT_MS)
            scheduleHideControls()
        }

        btnSkipPrev.setOnClickListener {
            onPrevListener?.invoke()
            scheduleHideControls()
        }

        btnSkipNext.setOnClickListener {
            onNextListener?.invoke()
            scheduleHideControls()
        }

        btnFullscreen.setOnClickListener {
            isFullscreen = !isFullscreen
            btnFullscreen.setImageResource(
                if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            )
            onFullscreenListener?.invoke(isFullscreen)
            scheduleHideControls()
        }

        btnSettings.setOnClickListener {
            onSettingsListener?.invoke()
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = exoPlayer?.duration ?: 0L
                    val newPos = (duration * progress / 1000L)
                    textCurrentTime.text = FormatUtils.formatTime(newPos)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isSeeking = true
                hideHandler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                isSeeking = false
                val duration = exoPlayer?.duration ?: 0L
                val progress = sb?.progress ?: 0
                val newPos = (duration * progress / 1000L)
                exoPlayer?.seekTo(newPos)
                scheduleHideControls()
            }
        })
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (showControlsEnabled) {
                    toggleControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!showControlsEnabled) return false
                val viewWidth = width
                val tapX = e.x
                if (tapX < viewWidth / 2) {
                    // Double tap left → rewind
                    seekRelative(-SEEK_INCREMENT_MS)
                    showSeekAnimation(seekLeftIcon)
                } else {
                    // Double tap right → forward
                    seekRelative(SEEK_INCREMENT_MS)
                    showSeekAnimation(seekRightIcon)
                }
                return true
            }
        })

        controlsRoot.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun showSeekAnimation(icon: ImageView) {
        icon.alpha = 1f
        icon.scaleX = 0.5f
        icon.scaleY = 0.5f
        icon.animate()
            .alpha(0f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(600)
            .start()
    }

    private fun seekRelative(deltaMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        val duration = exoPlayer?.duration ?: 0L
        val newPos = (current + deltaMs).coerceIn(0L, duration)
        exoPlayer?.seekTo(newPos)
        updateProgress()
    }

    // ---- CONTROLS VISIBILITY ----

    private fun showControls() {
        if (!showControlsEnabled) return
        controlsVisible = true
        controlsScrim.animate().alpha(1f).setDuration(200).start()
        topBar.animate().alpha(1f).translationY(0f).setDuration(200).start()
        centerControls.animate().alpha(1f).setDuration(200).start()
        bottomBar.animate().alpha(1f).translationY(0f).setDuration(200).start()
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        controlsScrim.animate().alpha(0f).setDuration(300).start()
        topBar.animate().alpha(0f).translationY(-topBar.height.toFloat()).setDuration(300).start()
        centerControls.animate().alpha(0f).setDuration(300).start()
        bottomBar.animate().alpha(0f).translationY(bottomBar.height.toFloat()).setDuration(300).start()
    }

    private fun hideControlsImmediate() {
        controlsVisible = false
        controlsScrim.alpha = 0f
        topBar.alpha = 0f
        centerControls.alpha = 0f
        bottomBar.alpha = 0f
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun scheduleHideControls() {
        hideHandler.removeCallbacksAndMessages(null)
        if (exoPlayer?.isPlaying == true) {
            hideHandler.postDelayed({ hideControls() }, HIDE_DELAY_MS)
        }
    }

    // ---- PROGRESS UPDATES ----

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, PROGRESS_UPDATE_MS)
        }
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    private fun updateProgress() {
        if (isSeeking) return
        val position = exoPlayer?.currentPosition ?: 0L
        val duration = exoPlayer?.duration ?: 0L
        val buffered = exoPlayer?.bufferedPercentage ?: 0

        if (duration > 0) {
            seekBar.progress = (position * 1000 / duration).toInt()
            seekBar.secondaryProgress = buffered * 10
        }
        textCurrentTime.text = FormatUtils.formatTime(position)
    }

    private fun updateDuration() {
        val dur = exoPlayer?.duration ?: 0L
        textDuration.text = FormatUtils.formatTime(dur)
        seekBar.max = 1000
    }

    private fun updatePlayPauseIcon() {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
    }

    private fun showBuffering(show: Boolean) {
        bufferingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            centerControls.visibility = View.INVISIBLE
        } else {
            centerControls.visibility = View.VISIBLE
        }
    }
}
