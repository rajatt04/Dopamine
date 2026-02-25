package com.google.android.piyush.dopamine.activities

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.CommentsAdapter
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.databinding.ActivityYoutubePlayerBinding
import com.google.android.piyush.dopamine.fragments.AddToPlaylistSheet
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
import com.google.android.piyush.dopamine.player.NewPipeStreamExtractor
import com.google.android.piyush.dopamine.player.PlayerSettingsSheet
import com.google.android.piyush.dopamine.utilities.FormatUtils
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModelFactory
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.channelDetails.Item as ChannelItem
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class YoutubePlayer : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var youtubePlayerViewModel: YoutubePlayerViewModel
    private lateinit var databaseViewModel: DatabaseViewModel

    private var currentVideoId: String = ""
    private var currentChannelId: String = ""
    private var currentVideoTitle: String = ""

    // Like/Dislike state
    private var isLiked = false
    private var isDisliked = false

    // Bell notification state: 0=all, 1=personalized, 2=none
    private var bellState = 0

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val TAG = "YoutubePlayer"
        private const val PREFS_NAME = "customPlaylist"
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_CHANNEL_ID = "channelId"
    }

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
        databaseViewModel = DatabaseViewModel(applicationContext as Application)
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
        binding.YtPlayer.setCallback(object : ExoYouTubePlayer.PlayerCallback {
            override fun onReady() {
                Log.d(TAG, "Player ready")
            }

            override fun onError(error: String) {
                Log.e(TAG, "Player error: $error")
            }
        })

        // Back button → finish activity
        binding.YtPlayer.setOnBackListener {
            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                exitFullscreen()
            } else {
                finish()
            }
        }

        // Fullscreen toggle
        binding.YtPlayer.setOnFullscreenListener { isFullscreen ->
            if (isFullscreen) enterFullscreen() else exitFullscreen()
        }

        // Settings sheet
        binding.YtPlayer.setOnSettingsListener {
            PlayerSettingsSheet.newInstance(
                currentSpeed = binding.YtPlayer.getPlaybackSpeed(),
                videoId = currentVideoId,
                onSpeedSelected = { speed ->
                    binding.YtPlayer.setPlaybackSpeed(speed)
                },
                onQualitySelected = { option ->
                    binding.YtPlayer.switchStream(option.url)
                }
            ).show(supportFragmentManager, PlayerSettingsSheet.TAG)
        }

        if (currentVideoId.isNotEmpty()) {
            binding.YtPlayer.loadVideo(currentVideoId)
        }
    }

    private fun enterFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        binding.YtPlayer.setFullscreen(true)

        // Hide system bars for immersive mode
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Hide scrollable content
        binding.nestedScrollView.visibility = View.GONE
        binding.appBarLayout.visibility = View.GONE

        // Make player fill screen
        binding.YtPlayer.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        binding.YtPlayer.requestLayout()
    }

    private fun exitFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.YtPlayer.setFullscreen(false)

        // Show system bars
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        // Show scrollable content
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.appBarLayout.visibility = View.VISIBLE

        // Reset player height
        binding.YtPlayer.layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        binding.YtPlayer.requestLayout()
    }

    private fun setupClickListeners() {
        // PIP
        binding.btnPip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                enterPipMode()
            }
        }

        // Custom Playlist
        binding.btnCustomPlaylist.setOnClickListener {
            AddToPlaylistSheet().show(supportFragmentManager, AddToPlaylistSheet.TAG)
        }

        // Description expand/collapse
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

        // Share
        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, currentVideoTitle)
                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$currentVideoId")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        // Download
        binding.btnDownload.setOnClickListener {
            downloadVideo()
        }

        // Like button
        binding.btnLikeContainer.setOnClickListener {
            toggleLike()
        }

        // Dislike button
        binding.btnDislikeContainer.setOnClickListener {
            toggleDislike()
        }

        // Bell toggle
        binding.btnBell.setOnClickListener {
            bellState = (bellState + 1) % 3
            updateBellIcon()
            FormatUtils.animateBounce(binding.btnBell)
            FormatUtils.triggerHaptic(binding.btnBell)
        }

        // Load more comments
        binding.btnShowAllComments.setOnClickListener {
            if (youtubePlayerViewModel.commentNextPageToken != null) {
                youtubePlayerViewModel.getCommentThreads(currentVideoId, isLoadMore = true)
            }
        }
    }

    private fun toggleLike() {
        isLiked = !isLiked
        if (isLiked) isDisliked = false // Mutual exclusion

        updateLikeDislikeUI()
        FormatUtils.animateLikeBurst(binding.imgLike)
        FormatUtils.triggerHaptic(binding.btnLikeContainer)

        // Persist to database
        if (isLiked) {
            databaseViewModel.insertFavouriteVideos(
                EntityFavouritePlaylist(
                    videoId = currentVideoId,
                    thumbnail = cachedThumbnail,
                    title = currentVideoTitle,
                    channelId = currentChannelId,
                    channelTitle = cachedChannelTitle
                )
            )
        } else {
            databaseViewModel.deleteFavouriteVideo(currentVideoId)
        }
    }

    private fun toggleDislike() {
        isDisliked = !isDisliked
        if (isDisliked) {
            // Unlike if currently liked
            if (isLiked) {
                isLiked = false
                databaseViewModel.deleteFavouriteVideo(currentVideoId)
            }
        }

        updateLikeDislikeUI()
        FormatUtils.animateLikeBurst(binding.imgDislike)
        FormatUtils.triggerHaptic(binding.btnDislikeContainer)
    }

    private fun updateLikeDislikeUI() {
        val activeColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        val defaultColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSecondaryContainer)

        // Like icon
        binding.imgLike.setImageResource(
            if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_video
        )
        binding.imgLike.imageTintList = ColorStateList.valueOf(if (isLiked) activeColor else defaultColor)

        // Dislike icon
        binding.imgDislike.setImageResource(
            if (isDisliked) R.drawable.ic_dislike_filled else R.drawable.ic_dislike
        )
        binding.imgDislike.imageTintList = ColorStateList.valueOf(if (isDisliked) activeColor else defaultColor)
    }

    private fun updateBellIcon() {
        when (bellState) {
            0 -> binding.btnBell.setImageResource(R.drawable.ic_notifications_active)
            1 -> binding.btnBell.setImageResource(R.drawable.ic_notifications)
            2 -> binding.btnBell.setImageResource(R.drawable.ic_notifications)
        }
    }

    // Cached data for like button persistence
    private var cachedThumbnail: String? = null
    private var cachedChannelTitle: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        youtubePlayerViewModel.getVideoDetails(currentVideoId)
        youtubePlayerViewModel.videoDetails.observe(this) { resource ->
            when (resource) {
                is YoutubeResource.Success -> {
                    resource.data.items?.firstOrNull()?.let { updateVideoUI(it) }
                }
                is YoutubeResource.Error -> {
                    Log.e(TAG, "Video error: ${resource.exception.message}")
                }
                else -> {}
            }
        }

        youtubePlayerViewModel.getChannelDetails(currentChannelId)
        youtubePlayerViewModel.channelDetails.observe(this) { resource ->
            if (resource is YoutubeResource.Success) {
                resource.data.items?.firstOrNull()?.let { updateChannelUI(it) }
            }
        }

        youtubePlayerViewModel.getChannelsPlaylist(currentChannelId)
        youtubePlayerViewModel.channelsPlaylists.observe(this) { resource ->
            if (resource is YoutubeResource.Success) {
                binding.channelsPlaylist.apply {
                    layoutManager = LinearLayoutManager(this@YoutubePlayer)
                    adapter = YoutubeChannelPlaylistsAdapter(this@YoutubePlayer, resource.data)
                }
            }
        }

        // Check if this video is already liked
        databaseViewModel.isFavouriteVideo(currentVideoId)
        databaseViewModel.isFavourite.observe(this) { videoId ->
            isLiked = videoId == currentVideoId
            updateLikeDislikeUI()
        }

        // Subscribe state
        databaseViewModel.isSubscribed.observe(this) { isSubscribed ->
            updateSubscribeUI(isSubscribed)
        }

        // Comments
        setupCommentsObserver()
    }

    private fun setupCommentsObserver() {
        youtubePlayerViewModel.getCommentThreads(currentVideoId)
        youtubePlayerViewModel.commentThreads.observe(this) { resource ->
            when (resource) {
                is YoutubeResource.Success -> {
                    val comments = resource.data.items ?: emptyList()
                    binding.commentsRecyclerView.layoutManager =
                        LinearLayoutManager(this@YoutubePlayer)
                    binding.commentsRecyclerView.adapter =
                        CommentsAdapter(this@YoutubePlayer, comments)

                    binding.textCommentsHeader.text = "Comments (${comments.size})"

                    // Show "View all comments" if there's more to load
                    binding.btnShowAllComments.visibility =
                        if (youtubePlayerViewModel.commentNextPageToken != null) View.VISIBLE else View.GONE
                }
                is YoutubeResource.Error -> {
                    binding.textCommentsHeader.text = "Comments"
                    binding.btnShowAllComments.visibility = View.GONE
                }
                else -> {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVideoUI(item: Item) {
        item.snippet?.let { snippet ->
            currentVideoTitle = snippet.title ?: ""
            cachedThumbnail = snippet.thumbnails?.high?.url
            cachedChannelTitle = snippet.channelTitle

            binding.textTitle.text = snippet.title
            binding.textDescription.text = snippet.description

            // Set title on the player
            binding.YtPlayer.setTitle(snippet.title ?: "")

            val viewCount = FormatUtils.formatCount(item.statistics?.viewCount?.toLong() ?: 0)
            val likeCount = FormatUtils.formatCount(item.statistics?.likeCount?.toLong() ?: 0)

            binding.metaInfoText.text = "$viewCount views"
            binding.txtLikeCount.text = likeCount

            handleRecentVideo(item)
            saveToPreferences(item, viewCount)
        }
    }

    private fun updateChannelUI(item: ChannelItem) {
        item.snippet?.let { snippet ->
            binding.channelName.text = snippet.title
            binding.channelSubscribers.text =
                "${FormatUtils.formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"

            Glide.with(this)
                .load(snippet.thumbnails?.default?.url.takeUnless { it.isNullOrEmpty() } ?: Utilities.DEFAULT_LOGO)
                .into(binding.channelImage)

            databaseViewModel.checkIsSubscribed(currentChannelId)
            setupSubscribeButton(item)
        }
    }

    private fun updateSubscribeUI(isSubscribed: Boolean) {
        if (isSubscribed) {
            binding.btnSubscribe.text = "Subscribed"
            binding.btnSubscribe.setIconResource(R.drawable.rounded_done_24)
            // Switch to gray tonal style
            binding.btnSubscribe.setBackgroundColor(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerHigh)
            )
            binding.btnSubscribe.setTextColor(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            )
            binding.btnSubscribe.iconTint = ColorStateList.valueOf(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            )
            // Show bell
            binding.btnBell.visibility = View.VISIBLE
        } else {
            binding.btnSubscribe.text = "Subscribe"
            binding.btnSubscribe.setIconResource(0)
            // Switch to filled dark style
            binding.btnSubscribe.setBackgroundColor(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            )
            binding.btnSubscribe.setTextColor(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
            )
            // Hide bell
            binding.btnBell.visibility = View.GONE
        }
    }

    private fun setupSubscribeButton(item: ChannelItem) {
        binding.btnSubscribe.setOnClickListener {
            val isCurrentlySubscribed = databaseViewModel.isSubscribed.value ?: false

            FormatUtils.animateSubscribe(binding.btnSubscribe)
            FormatUtils.triggerHaptic(binding.btnSubscribe)

            if (isCurrentlySubscribed) {
                // Confirm unsubscribe
                MaterialAlertDialogBuilder(this)
                    .setTitle("Unsubscribe")
                    .setMessage("Are you sure you want to unsubscribe from ${item.snippet?.title}?")
                    .setPositiveButton("Unsubscribe") { _, _ ->
                        databaseViewModel.deleteSubscription(currentChannelId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                item.snippet?.let { snippet ->
                    databaseViewModel.insertSubscription(
                        com.google.android.piyush.database.entities.SubscriptionEntity(
                            channelId = currentChannelId,
                            title = snippet.title ?: "",
                            description = snippet.description,
                            thumbnail = snippet.thumbnails?.default?.url,
                            channelTitle = snippet.title
                        )
                    )
                }
            }
        }
    }

    private fun handleRecentVideo(item: Item) {
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        databaseViewModel.isRecentVideo(currentVideoId)
        databaseViewModel.isRecent.observe(this) { recentVideoId ->
            if (recentVideoId == currentVideoId) {
                databaseViewModel.updateRecentVideo(currentVideoId, currentTime)
            } else {
                databaseViewModel.insertRecentVideos(
                    EntityRecentVideos(
                        id = 0,
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

    private fun downloadVideo() {
        Snackbar.make(binding.root, "Preparing download...", Snackbar.LENGTH_SHORT).show()

        scope.launch {
            try {
                val streamUrl = NewPipeStreamExtractor.extractStreamUrl(currentVideoId)
                if (streamUrl != null) {
                    val request = DownloadManager.Request(Uri.parse(streamUrl))
                        .setTitle(currentVideoTitle.ifEmpty { "Dopamine Video" })
                        .setDescription("Downloading via Dopamine")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "Dopamine/${currentVideoTitle.take(50).replace("[^a-zA-Z0-9 ]".toRegex(), "")}.mp4"
                        )
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)

                    Snackbar.make(binding.root, "Download started", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Could not extract video stream", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                Snackbar.make(binding.root, "Download failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.nestedScrollView.visibility = View.GONE
            binding.appBarLayout.visibility = View.GONE
        } else {
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.appBarLayout.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.YtPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.YtPlayer.onPause()
    }

    override fun onDestroy() {
        binding.YtPlayer.release()
        scope.cancel()
        super.onDestroy()
    }
}