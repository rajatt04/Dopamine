package com.google.android.piyush.dopamine.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class YouTubePlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "youtube_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.google.android.piyush.dopamine.ACTION_PLAY"
        const val ACTION_PAUSE = "com.google.android.piyush.dopamine.ACTION_PAUSE"
        const val ACTION_STOP = "com.google.android.piyush.dopamine.ACTION_STOP"
        const val ACTION_NEXT = "com.google.android.piyush.dopamine.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.google.android.piyush.dopamine.ACTION_PREVIOUS"

        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_CHANNEL_TITLE = "channel_title"
        const val EXTRA_THUMBNAIL_URL = "thumbnail_url"

        private var instance: YouTubePlaybackService? = null

        fun getInstance(): YouTubePlaybackService? = instance

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying

        private val _currentVideoId = MutableStateFlow<String?>(null)
        val currentVideoId: StateFlow<String?> = _currentVideoId

        private val _currentVideoTitle = MutableStateFlow<String?>(null)
        val currentVideoTitle: StateFlow<String?> = _currentVideoTitle

        private val _currentPosition = MutableStateFlow(0f)
        val currentPosition: StateFlow<Float> = _currentPosition

        private val _videoDuration = MutableStateFlow(0f)
        val videoDuration: StateFlow<Float> = _videoDuration
    }

    private val binder = LocalBinder()
    var youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var notificationManager: NotificationManager? = null
    private var playbackState = PlayerConstants.PlayerState.UNSTARTED

    private var videoTitle: String = ""
    private var channelTitle: String = ""
    private var thumbnailUrl: String = ""

    inner class LocalBinder : Binder() {
        fun getService(): YouTubePlaybackService = this@YouTubePlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        initializeMediaSession()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                youTubePlayer?.play()
                _isPlaying.value = true
                updatePlaybackState()
                updateNotification()
            }
            ACTION_PAUSE -> {
                youTubePlayer?.pause()
                _isPlaying.value = false
                updatePlaybackState()
                updateNotification()
            }
            ACTION_STOP -> {
                youTubePlayer?.pause()
                _isPlaying.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        val videoId = intent?.getStringExtra(EXTRA_VIDEO_ID)
        videoTitle = intent?.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Unknown"
        channelTitle = intent?.getStringExtra(EXTRA_CHANNEL_TITLE) ?: "Unknown"
        thumbnailUrl = intent?.getStringExtra(EXTRA_THUMBNAIL_URL) ?: ""

        if (videoId != null) {
            _currentVideoId.value = videoId
            _currentVideoTitle.value = videoTitle
            startForeground(NOTIFICATION_ID, createNotification())
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayer?.pause()
        mediaSession?.release()
        instance = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YouTube Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "YouTube video playback controls"
                setShowBadge(false)
            }
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "YouTubePlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    youTubePlayer?.play()
                    _isPlaying.value = true
                    updatePlaybackState()
                }

                override fun onPause() {
                    youTubePlayer?.pause()
                    _isPlaying.value = false
                    updatePlaybackState()
                }

                override fun onStop() {
                    youTubePlayer?.pause()
                    _isPlaying.value = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }

                override fun onSeekTo(pos: Long) {
                    youTubePlayer?.seekTo(pos.toFloat() / 1000f)
                }
            })
            isActive = true
        }
    }

    fun getYouTubePlayerListener(): YouTubePlayerListener {
        return object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                this@YouTubePlaybackService.youTubePlayer = youTubePlayer
                _currentVideoId.value?.let { videoId ->
                    youTubePlayer.loadVideo(videoId, _currentPosition.value)
                }
            }

            override fun onStateChange(
                youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                playbackState = state
                _isPlaying.value = state == PlayerConstants.PlayerState.PLAYING

                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        updatePlaybackState()
                        updateNotification()
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        _isPlaying.value = false
                        updatePlaybackState()
                        updateNotification()
                    }
                    PlayerConstants.PlayerState.ENDED -> {
                        _isPlaying.value = false
                        updatePlaybackState()
                        updateNotification()
                    }
                    else -> {}
                }
            }

            override fun onCurrentSecond(
                youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                second: Float
            ) {
                _currentPosition.value = second
            }

            override fun onVideoDuration(
                youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                duration: Float
            ) {
                _videoDuration.value = duration
            }
        }
    }

    fun getIFramePlayerOptions(): IFramePlayerOptions {
        return IFramePlayerOptions.Builder(this)
            .controls(0)
            .fullscreen(0)
            .rel(0)
            .build()
    }

    fun playVideo(videoId: String, title: String, channel: String, thumbnail: String, startTime: Float = 0f) {
        _currentVideoId.value = videoId
        _currentVideoTitle.value = title
        videoTitle = title
        channelTitle = channel
        thumbnailUrl = thumbnail
        _currentPosition.value = startTime

        youTubePlayer?.loadVideo(videoId, startTime)
        updateNotification()
    }

    fun seekTo(position: Float) {
        youTubePlayer?.seekTo(position)
        _currentPosition.value = position
    }

    private fun updatePlaybackState() {
        val state = if (_isPlaying.value) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, (_currentPosition.value * 1000).toLong(), 1f)
                .build()
        )

        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, videoTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, channelTitle)
                .build()
        )
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, YoutubePlayer::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (_isPlaying.value) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                createActionIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                createActionIntent(ACTION_PLAY)
            )
        }

        val stopAction = NotificationCompat.Action(
            R.drawable.ic_close,
            "Stop",
            createActionIntent(ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(videoTitle)
            .setContentText(channelTitle)
            .setSmallIcon(R.drawable.ic_youtube)
            .setContentIntent(contentIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .setOngoing(_isPlaying.value)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        if (_currentVideoId.value != null) {
            notificationManager?.notify(NOTIFICATION_ID, createNotification())
        }
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, YouTubePlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
