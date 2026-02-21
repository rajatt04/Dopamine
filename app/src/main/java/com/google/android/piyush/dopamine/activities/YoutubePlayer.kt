package com.google.android.piyush.dopamine.activities

import android.app.Application
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
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModelFactory
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.channelDetails.Item as ChannelItem
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import java.text.DecimalFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Suppress("DEPRECATION")
class YoutubePlayer : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var youtubePlayerViewModel: YoutubePlayerViewModel
    private lateinit var databaseViewModel: DatabaseViewModel

    private var currentVideoId: String = ""
    private var currentChannelId: String = ""

    private val decimalFormatter by lazy { DecimalFormat("#0.0") }
    private val integerFormatter by lazy { DecimalFormat("#,##0") }

    companion object {
        private const val TAG = "YoutubePlayer"
        private const val PREFS_NAME = "customPlaylist"
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

        if (currentVideoId.isNotEmpty()) {
            binding.YtPlayer.loadVideo(currentVideoId)
        }
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

        databaseViewModel.isFavouriteVideo(currentVideoId)
        databaseViewModel.isFavourite.observe(this) { videoId ->
            binding.btnLike.isChecked = videoId == currentVideoId
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
            binding.channelSubscribers.text =
                "${formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"

            Glide.with(this)
                .load(snippet.thumbnails?.default?.url.takeUnless { it.isNullOrEmpty() } ?: Utilities.DEFAULT_LOGO)
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
        super.onDestroy()
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
            .scaleX(0.7f).scaleY(0.7f).setDuration(100)
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .start()
            }.start()
    }
}