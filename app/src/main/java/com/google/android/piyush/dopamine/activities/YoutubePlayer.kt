package com.google.android.piyush.dopamine.activities

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.databinding.ActivityYoutubePlayerBinding
import com.google.android.piyush.dopamine.fragments.AddToPlaylistSheet
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModelFactory
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.channelDetails.Item as ChannelItem
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import java.text.DecimalFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Suppress("DEPRECATION")
class YoutubePlayer : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityYoutubePlayerBinding

    // ViewModels
    private lateinit var youtubePlayerViewModel: YoutubePlayerViewModel
    private lateinit var databaseViewModel: DatabaseViewModel

    // Data
    private var currentVideoId: String = ""
    private var currentChannelId: String = ""
    
    // YouTube Player reference for PIP
    private var youTubePlayerInstance: YouTubePlayer? = null

    // Cached formatters for performance
    private val decimalFormatter by lazy { DecimalFormat("#0.0") }
    private val integerFormatter by lazy { DecimalFormat("#,##0") }

    companion object {
        private const val TAG = "YoutubePlayer"
        private const val PREFS_NAME = "customPlaylist"

        // Animation constants
        private const val ANIM_SCALE_DOWN = 0.7f
        private const val ANIM_DURATION_DOWN = 100L
        private const val ANIM_DURATION_UP = 200L
        private const val ANIM_OVERSHOOT = 2f

        // Intent keys
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_CHANNEL_ID = "channelId"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModels()
        setupWindowInsets()
        getIntentData()
        setupPlayer()
        setupClickListeners()
        setupObservers()
    }

    private fun initViewModels() {
        val repository = YoutubeRepositoryImpl()
        val factory = YoutubePlayerViewModelFactory(repository)
        youtubePlayerViewModel = ViewModelProvider(this, factory)[YoutubePlayerViewModel::class.java]
        databaseViewModel = DatabaseViewModel(applicationContext)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getIntentData() {
        currentVideoId = intent?.getStringExtra(KEY_VIDEO_ID).orEmpty()
        currentChannelId = intent?.getStringExtra(KEY_CHANNEL_ID).orEmpty()
    }

    private fun setupPlayer() {
        binding.YtPlayer.apply {
            enableBackgroundPlayback(true)

            val iFramePlayerOptions = IFramePlayerOptions.Builder(this@YoutubePlayer)
                .rel(0)
                .controls(0)
                .build()

            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayerInstance = youTubePlayer
                    if (currentVideoId.isNotEmpty()) {
                        youTubePlayer.loadVideo(currentVideoId, 0f)
                        Log.d(TAG, "Loading video: $currentVideoId")
                    }
                }
            }, true, iFramePlayerOptions)

            addFullscreenListener(object : FullscreenListener {
                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                    enterFullscreenMode(fullscreenView)
                }

                override fun onExitFullscreen() {
                    exitFullscreenMode()
                }
            })
        }
    }

    private fun enterFullscreenMode(fullscreenView: View) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        binding.nestedScrollView.visibility = View.GONE
        binding.appBarLayout.visibility = View.GONE

        if (fullscreenView.parent == null) {
            binding.main.addView(fullscreenView)
        }
    }

    private fun exitFullscreenMode() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        binding.nestedScrollView.visibility = View.VISIBLE
        binding.appBarLayout.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnPip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                enterPipMode()
            }
        }

        binding.btnCustomPlaylist.setOnClickListener {
            AddToPlaylistSheet().show(supportFragmentManager, AddToPlaylistSheet.TAG)
        }

        // Show More/Less for description
        var isDescriptionExpanded = false
        binding.btnShowMore.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                binding.textDescription.maxLines = Integer.MAX_VALUE
                binding.btnShowMore.text = "Show Less"
            } else {
                binding.textDescription.maxLines = 3
                binding.btnShowMore.text = "Show More"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        // Video Details
        youtubePlayerViewModel.getVideoDetails(currentVideoId)
        youtubePlayerViewModel.videoDetails.observe(this) { resource ->
            when (resource) {
                is YoutubeResource.Success -> {
                    resource.data.items?.firstOrNull()?.let { updateVideoUI(it) }
                }
                is YoutubeResource.Error -> {
                    Log.e(TAG, "Video error: ${resource.exception.message}")
                }
                else -> { /* Loading state */ }
            }
        }

        // Channel Details
        youtubePlayerViewModel.getChannelDetails(currentChannelId)
        youtubePlayerViewModel.channelDetails.observe(this) { resource ->
            when (resource) {
                is YoutubeResource.Success -> {
                    resource.data.items?.firstOrNull()?.let { updateChannelUI(it) }
                }
                else -> { /* Handle other states */ }
            }
        }

        // Related Playlists
        youtubePlayerViewModel.getChannelsPlaylist(currentChannelId)
        youtubePlayerViewModel.channelsPlaylists.observe(this) { resource ->
            if (resource is YoutubeResource.Success) {
                setupRelatedPlaylistsRecyclerView(resource.data)
            }
        }

        // Favourites
        databaseViewModel.isFavouriteVideo(currentVideoId)
        databaseViewModel.isFavourite.observe(this) { videoId ->
            binding.btnLike.isChecked = videoId == currentVideoId
        }
    }

    private fun setupRelatedPlaylistsRecyclerView(data: com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists) {
        binding.channelsPlaylist.apply {
            layoutManager = LinearLayoutManager(this@YoutubePlayer)
            adapter = YoutubeChannelPlaylistsAdapter(this@YoutubePlayer, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVideoUI(item: Item) {
        item.snippet?.let { snippet ->
            binding.textTitle.text = snippet.title
            binding.textDescription.text = snippet.description

            val viewCount = formatCount(item.statistics?.viewCount?.toLong() ?: 0)
            val likeCount = formatCount(item.statistics?.likeCount?.toLong() ?: 0)

            binding.metaInfoText.text = "$viewCount views"
            binding.btnLike.text = likeCount

            setupLikeButton(snippet)
            handleRecentVideo(item)
            saveToPreferences(item, viewCount)
        }
    }

    private fun setupLikeButton(snippet: com.google.android.piyush.youtube.model.Snippet) {
        binding.btnLike.addOnCheckedStateChangedListener { _, state ->
            val isChecked = state == MaterialCheckBox.STATE_CHECKED
            animateLikeButton(binding.btnLike)

            if (isChecked) {
                databaseViewModel.insertFavouriteVideos(
                    EntityFavouritePlaylist(
                        videoId = currentVideoId,
                        thumbnail = snippet.thumbnails?.high?.url,
                        title = snippet.title,
                        channelId = currentChannelId,
                        channelTitle = snippet.channelTitle
                    )
                )
            } else {
                databaseViewModel.deleteFavouriteVideo(currentVideoId)
            }
        }
    }

    private fun updateChannelUI(item: ChannelItem) {
        item.snippet?.let { snippet ->
            binding.channelName.text = snippet.title
            binding.channelSubscribers.text = "${formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"

            val logoUrl = snippet.thumbnails?.default?.url
            Glide.with(this)
                .load(logoUrl.takeUnless { it.isNullOrEmpty() } ?: Utilities.DEFAULT_LOGO)
                .into(binding.channelImage)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRecentVideo(item: Item) {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

        databaseViewModel.isRecentVideo(currentVideoId)
        databaseViewModel.isRecent.observe(this) { recentVideoId ->
            if (recentVideoId == currentVideoId) {
                databaseViewModel.updateRecentVideo(currentVideoId, currentTime)
            } else {
                databaseViewModel.insertRecentVideos(
                    EntityRecentVideos(
                        id = Random.nextInt(),
                        videoId = currentVideoId,
                        thumbnail = item.snippet?.thumbnails?.high?.url,
                        title = item.snippet?.title,
                        timing = currentTime,
                        channelId = currentChannelId
                    )
                )
            }
        }
    }

    private fun saveToPreferences(item: Item, viewCount: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putString(KEY_VIDEO_ID, currentVideoId)
            putString("thumbnail", item.snippet?.thumbnails?.high?.url)
            putString("title", item.snippet?.title)
            putString(KEY_CHANNEL_ID, currentChannelId)
            putString("channelTitle", item.snippet?.channelTitle)
            putString("viewCount", viewCount)
            putString("publishedAt", item.snippet?.publishedAt)
            putString("duration", item.contentDetails?.duration)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipMode() {
        val aspectRatio = android.util.Rational(16, 9)
        val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Configuration changes handled by fullscreen listener
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            // Hide everything except the player
            binding.nestedScrollView.visibility = View.GONE
            binding.appBarLayout.visibility = View.GONE
        } else {
            // Restore normal view
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.appBarLayout.visibility = View.VISIBLE
        }
    }

    private fun formatCount(count: Long): String {
        if (count < 1000) return count.toString()

        val suffix = charArrayOf(' ', 'K', 'M', 'B', 'T', 'P', 'E')
        val value = kotlin.math.floor(kotlin.math.log10(count.toDouble())).toInt()
        val base = value / 3

        return if (value >= 3 && base < suffix.size) {
            val scaledValue = count / Math.pow(10.0, (base * 3).toDouble())
            "${decimalFormatter.format(scaledValue)}${suffix[base]}"
        } else {
            integerFormatter.format(count)
        }
    }

    private fun animateLikeButton(view: View) {
        view.animate()
            .scaleX(ANIM_SCALE_DOWN)
            .scaleY(ANIM_SCALE_DOWN)
            .setDuration(ANIM_DURATION_DOWN)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(ANIM_DURATION_UP)
                    .setInterpolator(android.view.animation.OvershootInterpolator(ANIM_OVERSHOOT))
                    .start()
            }
            .start()
    }
}