package com.google.android.piyush.dopamine.player

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Custom YouTube player using NewPipe Extractor + ExoPlayer.
 * Extracts stream URLs from video IDs and plays them natively.
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
    }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)

        exoPlayer = ExoPlayer.Builder(context).build()

        playerView = PlayerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            player = exoPlayer
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        addView(playerView)

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        playerCallback?.onReady()
                        if (exoPlayer?.playWhenReady == true) {
                            playerCallback?.onPlaying()
                        }
                    }
                    Player.STATE_BUFFERING -> playerCallback?.onBuffering()
                    Player.STATE_ENDED -> playerCallback?.onEnded()
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerCallback?.onPlaying()
                } else {
                    playerCallback?.onPaused()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                playerCallback?.onError(error.message ?: "Unknown error")
            }
        })
    }

    fun setCallback(callback: PlayerCallback) {
        this.playerCallback = callback
    }

    /**
     * Show or hide ExoPlayer's built-in controls.
     */
    fun setShowControls(show: Boolean) {
        playerView.useController = show
    }

    /**
     * Load and auto-play a YouTube video by its ID.
     * Extracts the stream URL in background, then plays.
     */
    fun loadVideo(videoId: String) {
        if (videoId == currentVideoId && exoPlayer?.isPlaying == true) return
        currentVideoId = videoId

        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            try {
                playerCallback?.onBuffering()
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading video: $videoId", e)
                playerCallback?.onError(e.message ?: "Error loading video")
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun onResume() {
        // Player will resume if playWhenReady was true
    }

    fun onPause() {
        exoPlayer?.pause()
    }

    /**
     * Release the player. Call when the host is being destroyed.
     */
    fun release() {
        currentLoadJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
    }
}
