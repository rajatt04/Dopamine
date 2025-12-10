package com.google.android.piyush.dopamine.activities

import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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

    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var youtubeRepositoryImpl: YoutubeRepositoryImpl
    private lateinit var youtubePlayerViewModel: YoutubePlayerViewModel
    private lateinit var youtubePlayerViewModelFactory: YoutubePlayerViewModelFactory
    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var context: Context

    private var currentVideoId: String = ""
    private var currentChannelId: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this

        initViewModels()
        setupWindowInsets()
        getIntentData()
        
        setupPlayer()
        setupClickListeners()
        setupObservers()
    }

    private fun initViewModels() {
        youtubeRepositoryImpl = YoutubeRepositoryImpl()
        youtubePlayerViewModelFactory = YoutubePlayerViewModelFactory(youtubeRepositoryImpl)
        databaseViewModel = DatabaseViewModel(applicationContext)
        youtubePlayerViewModel = ViewModelProvider(
            this, youtubePlayerViewModelFactory
        )[YoutubePlayerViewModel::class.java]
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getIntentData() {
        currentVideoId = intent?.getStringExtra("videoId") ?: ""
        currentChannelId = intent?.getStringExtra("channelId") ?: ""
    }

    private fun setupPlayer() {
        binding.YtPlayer.enableBackgroundPlayback(true)
        val iFramePlayerOptions = IFramePlayerOptions.Builder(context)
            .rel(1)
            .controls(1)
            .fullscreen(1)
            .build()

        binding.YtPlayer.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                if (currentVideoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(currentVideoId, 0F)
                    Log.d(TAG, "YoutubePlayer Loading: $currentVideoId")
                }
            }
        }, true, iFramePlayerOptions)

        binding.YtPlayer.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                binding.nestedScrollView.visibility = View.GONE
                binding.appBarLayout.visibility = View.GONE // Hide standard container

                if (fullscreenView.parent == null) {
                    (window.decorView as androidx.coordinatorlayout.widget.CoordinatorLayout).addView(fullscreenView)
                }
            }

            override fun onExitFullscreen() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                
                // Remove fullscreen view is handled by library usually, but ensuring visibility restore
                binding.nestedScrollView.visibility = View.VISIBLE
                binding.appBarLayout.visibility = View.VISIBLE
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnPip.setOnClickListener {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                enterPictureInPictureMode()
            }
        }

        binding.btnAddToPlaylist.setOnClickListener {
           // Handled by CheckedState usually for Likes, but here it's Save
           // Implementing Save Logic if needed or mapping to Favourite
           // For now, let's map "Save" to Favourites for consistency with UI
        }
        
         binding.btnCustomPlaylist.setOnClickListener {
            val bottomSheetFragment = AddToPlaylistSheet()
            bottomSheetFragment.show(supportFragmentManager, AddToPlaylistSheet.TAG)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        // Video Details
        youtubePlayerViewModel.getVideoDetails(currentVideoId)
        youtubePlayerViewModel.videoDetails.observe(this) { videoDetails ->
            if (videoDetails is YoutubeResource.Success) {
                val item = videoDetails.data.items?.firstOrNull() ?: return@observe
                updateVideoUI(item)
            } else if (videoDetails is YoutubeResource.Error) {
                Log.e(TAG, "Video Error: ${videoDetails.exception.message}")
            }
        }

        // Channel Details
        youtubePlayerViewModel.getChannelDetails(currentChannelId)
        youtubePlayerViewModel.channelDetails.observe(this) { channelDetails ->
            if (channelDetails is YoutubeResource.Success) {
                val item = channelDetails.data.items?.firstOrNull() ?: return@observe
                updateChannelUI(item)
            }
        }

        // Related Playlists
        youtubePlayerViewModel.getChannelsPlaylist(currentChannelId)
        youtubePlayerViewModel.channelsPlaylists.observe(this) { channelsPlaylist ->
            if (channelsPlaylist is YoutubeResource.Success) {
                binding.channelsPlaylist.layoutManager = LinearLayoutManager(this)
                binding.channelsPlaylist.adapter = YoutubeChannelPlaylistsAdapter(context, channelsPlaylist.data)
            }
        }

        // Favourites
        databaseViewModel.isFavouriteVideo(currentVideoId)
        databaseViewModel.isFavourite.observe(this) {
             binding.btnLike.isChecked = it == currentVideoId
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVideoUI(item: Item) {
        val snippet = item.snippet
        val stats = item.statistics
        
        binding.textTitle.text = snippet?.title
        binding.textDescription.text = snippet?.description
        
        val viewCount = formatCount(stats?.viewCount?.toLong() ?: 0)
        val likeCount = formatCount(stats?.likeCount?.toLong() ?: 0)
        
        binding.metaInfoText.text = "$viewCount views"
        binding.btnLike.text = likeCount

        // Logic for Likes/Favourites
        binding.btnLike.addOnCheckedStateChangedListener { _, state ->
             val isChecked = state == com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
             if (isChecked) {
                databaseViewModel.insertFavouriteVideos(
                    EntityFavouritePlaylist(
                        videoId = currentVideoId,
                        thumbnail = snippet?.thumbnails?.high?.url,
                        title = snippet?.title,
                        channelId = currentChannelId,
                        channelTitle = snippet?.channelTitle
                    )
                )
            } else {
                databaseViewModel.deleteFavouriteVideo(currentVideoId)
            }
        }

        handleRecentVideo(item)
        saveToPreferences(item, viewCount)
    }

    private fun updateChannelUI(item: ChannelItem) {
        val snippet = item.snippet
        val stats = item.statistics
        
        binding.channelName.text = snippet?.title
        binding.channelSubscribers.text = "${formatCount(stats?.subscriberCount?.toLong() ?: 0)} Subscribers"
        
        val logoUrl = snippet?.thumbnails?.default?.url
        Glide.with(this)
             .load(if (logoUrl.isNullOrEmpty()) Utilities.DEFAULT_LOGO else logoUrl)
             .into(binding.channelImage)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRecentVideo(item: Item) {
        databaseViewModel.isRecentVideo(currentVideoId)
        databaseViewModel.isRecent.observe(this) {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            if (it == currentVideoId) {
                databaseViewModel.updateRecentVideo(currentVideoId, currentTime)
            } else {
                databaseViewModel.insertRecentVideos(
                    EntityRecentVideos(
                        id = Random.nextInt(), // Better random usage could be implemented
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
        getSharedPreferences("customPlaylist", MODE_PRIVATE).edit {
            putString("videoId", currentVideoId)
            putString("thumbnail", item.snippet?.thumbnails?.high?.url)
            putString("title", item.snippet?.title)
            putString("channelId", currentChannelId)
            putString("channelTitle", item.snippet?.channelTitle)
            putString("viewCount", viewCount)
            putString("publishedAt", item.snippet?.publishedAt)
            putString("duration", item.contentDetails?.duration)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.nestedScrollView.visibility = View.GONE
            binding.appBarLayout.visibility = View.GONE
            binding.YtPlayer.wrapContent()
        } else {
            binding.nestedScrollView.visibility = View.VISIBLE
            binding.appBarLayout.visibility = View.VISIBLE
        }
    }

    private fun formatCount(count: Long): String {
        if (count < 1000) return count.toString()
        val suffix = charArrayOf(' ', 'K', 'M', 'B', 'T', 'P', 'E')
        val value = Math.floor(Math.log10(count.toDouble())).toInt()
        val base = value / 3
        return if (value >= 3 && base < suffix.size) {
            DecimalFormat("#0.0").format(count / Math.pow(10.0, (base * 3).toDouble())) + suffix[base]
        } else {
            DecimalFormat("#,##0").format(count)
        }
    }
}
