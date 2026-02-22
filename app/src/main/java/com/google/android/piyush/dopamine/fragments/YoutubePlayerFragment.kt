package com.google.android.piyush.dopamine.fragments

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.CommentsAdapter
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
import com.google.android.piyush.dopamine.player.PlayerSettingsSheet
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.SharedViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModelFactory
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.channelDetails.Item as ChannelItem
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import java.text.DecimalFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.google.android.piyush.dopamine.utilities.FormatUtils
import kotlin.random.Random

@Suppress("DEPRECATION")
class YoutubePlayerFragment : Fragment() {

    private lateinit var motionLayout: MotionLayout
    private lateinit var youtubePlayer: ExoYouTubePlayer

    private lateinit var btnSubscribe: com.google.android.material.button.MaterialButton
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnPip: com.google.android.material.button.MaterialButton
    private lateinit var textTitle: TextView
    private lateinit var miniPlayerTitle: TextView
    private lateinit var btnMiniPlay: ImageButton
    private lateinit var btnMiniClose: ImageButton
    private lateinit var btnLike: MaterialCheckBox

    private lateinit var youtubePlayerViewModel: YoutubePlayerViewModel
    private lateinit var databaseViewModel: DatabaseViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var currentVideoId: String = ""
    private var currentChannelId: String = ""
    private var isPlaying = false
    private var isFullscreen = false



    companion object {
        private const val TAG = "YoutubePlayerFragment"
        private const val PREFS_NAME = "customPlaylist"
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_CHANNEL_ID = "channelId"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_youtube_player, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initViewModels()
        setupPlayer()
        setupClickListeners()
        setupObservers()
    }

    private fun initViews(view: View) {
        motionLayout = view.findViewById(R.id.player_motion_layout)
        youtubePlayer = view.findViewById(R.id.YtPlayer)
        textTitle = view.findViewById<TextView>(R.id.textTitle)
        miniPlayerTitle = view.findViewById<TextView>(R.id.miniPlayerTitle)
        btnMiniPlay = view.findViewById<ImageButton>(R.id.btnMiniPlay)
        btnMiniClose = view.findViewById<ImageButton>(R.id.btnMiniClose)
        btnLike = view.findViewById<MaterialCheckBox>(R.id.btnLike)
        btnSubscribe = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubscribe)
        btnSave = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddToPlaylist)
        btnPip = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPip)
    }

    private fun initViewModels() {
        val repository = YoutubeRepositoryImpl()
        val factory = YoutubePlayerViewModelFactory(repository)
        youtubePlayerViewModel = ViewModelProvider(this, factory)[YoutubePlayerViewModel::class.java]
        databaseViewModel = DatabaseViewModel(requireActivity().application)
    }

    private fun setupPlayer() {
        youtubePlayer.setShowControls(false) // Mini player: no controls
        youtubePlayer.setCallback(object : ExoYouTubePlayer.PlayerCallback {
            override fun onReady() {
                Log.d(TAG, "Player ready")
            }

            override fun onPlaying() {
                isPlaying = true
                updateMiniPlayerPlayButton()
            }

            override fun onPaused() {
                isPlaying = false
                updateMiniPlayerPlayButton()
            }

            override fun onError(error: String) {
                Log.e(TAG, "Player error: $error")
            }
        })

        // Back button collapses to mini player
        youtubePlayer.setOnBackListener {
            if (isFullscreen) {
                exitFullscreen()
            } else {
                motionLayout.transitionToState(R.id.start)
            }
        }

        // Fullscreen toggle
        youtubePlayer.setOnFullscreenListener { fullscreen ->
            if (fullscreen) enterFullscreen() else exitFullscreen()
        }

        // Settings
        youtubePlayer.setOnSettingsListener {
            PlayerSettingsSheet.newInstance(
                currentSpeed = youtubePlayer.getPlaybackSpeed(),
                videoId = currentVideoId,
                onSpeedSelected = { speed ->
                    youtubePlayer.setPlaybackSpeed(speed)
                },
                onQualitySelected = { option ->
                    youtubePlayer.switchStream(option.url)
                }
            ).show(parentFragmentManager, PlayerSettingsSheet.TAG)
        }

        // MotionLayout state listener to show/hide controls
        motionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}
            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {}

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.end) {
                    // Full player state: show controls
                    youtubePlayer.setShowControls(true)
                } else if (currentId == R.id.start) {
                    // Mini player state: hide controls
                    youtubePlayer.setShowControls(false)
                }
            }

            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
        })
    }

    private fun updateMiniPlayerPlayButton() {
        btnMiniPlay.setImageResource(
            if (isPlaying) R.drawable.pause_24dp else R.drawable.play_arrow_24dp
        )
    }

    private fun setupClickListeners() {
        btnMiniPlay.setOnClickListener {
            youtubePlayer.togglePlayPause()
        }

        btnMiniClose.setOnClickListener {
            youtubePlayer.pause()
            sharedViewModel.closePlayer()
        }

        view?.findViewById<View>(R.id.btnCollapse)?.setOnClickListener {
            motionLayout.transitionToState(R.id.start)
        }

        btnSave.setOnClickListener {
            AddToPlaylistSheet().show(parentFragmentManager, AddToPlaylistSheet.TAG)
        }

        view?.findViewById<View>(R.id.btnCustomPlaylist)?.setOnClickListener {
            AddToPlaylistSheet().show(parentFragmentManager, AddToPlaylistSheet.TAG)
        }

        btnPip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                )
            }
        }

        var isDescriptionExpanded = false
        val textDescription = view?.findViewById<TextView>(R.id.textDescription)
        val btnShowMore = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowMore)

        btnShowMore?.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                textDescription?.maxLines = Integer.MAX_VALUE
                btnShowMore.text = "Show Less"
            } else {
                textDescription?.maxLines = 3
                btnShowMore.text = "Show More"
            }
        }

        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowAllComments)?.setOnClickListener {
            youtubePlayerViewModel.getCommentThreads(currentVideoId, true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        sharedViewModel.currentVideo.observe(viewLifecycleOwner) { selectedVideo ->
            if (selectedVideo != null) {
                if (currentVideoId != selectedVideo.videoId) {
                    currentVideoId = selectedVideo.videoId
                    currentChannelId = selectedVideo.channelId

                    textTitle.text = selectedVideo.title
                    miniPlayerTitle.text = selectedVideo.title

                    youtubePlayer.loadVideo(currentVideoId)
                    fetchVideoDetails()
                }
            } else {
                youtubePlayer.pause()
            }
        }

        setupInnerObservers()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchVideoDetails() {
        youtubePlayerViewModel.getVideoDetails(currentVideoId)
        youtubePlayerViewModel.getChannelDetails(currentChannelId)
        youtubePlayerViewModel.getChannelsPlaylist(currentChannelId)
        youtubePlayerViewModel.getCommentThreads(currentVideoId)
        databaseViewModel.isFavouriteVideo(currentVideoId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupInnerObservers() {
        youtubePlayerViewModel.videoDetails.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) {
                resource.data.items?.firstOrNull()?.let { updateVideoUI(it) }
            }
        }

        youtubePlayerViewModel.channelDetails.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) {
                resource.data.items?.firstOrNull()?.let { updateChannelUI(it) }
            }
        }

        youtubePlayerViewModel.channelsPlaylists.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) {
                val recyclerView = view?.findViewById<RecyclerView>(R.id.channelsPlaylist)
                recyclerView?.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = YoutubeChannelPlaylistsAdapter(requireContext(), resource.data)
                }
            }
        }

        youtubePlayerViewModel.commentThreads.observe(viewLifecycleOwner) { resource ->
            val recyclerView = view?.findViewById<RecyclerView>(R.id.commentsRecyclerView)
            val commentsHeader = view?.findViewById<TextView>(R.id.textCommentsHeader)
            val commentsCard = view?.findViewById<View>(R.id.commentsCard)
            val showMoreBtn = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowAllComments)

            when (resource) {
                is YoutubeResource.Loading -> {
                    if (youtubePlayerViewModel.commentNextPageToken == null) {
                        commentsCard?.visibility = View.GONE
                    }
                    showMoreBtn?.text = "Loading..."
                    showMoreBtn?.isEnabled = false
                }
                is YoutubeResource.Success -> {
                    val items = resource.data.items ?: emptyList()
                    val nextPageToken = resource.data.nextPageToken

                    if (items.isEmpty()) {
                        commentsCard?.visibility = View.GONE
                    } else {
                        commentsCard?.visibility = View.VISIBLE
                        commentsHeader?.text = "Comments (${items.size})"
                        
                        recyclerView?.apply {
                            if (layoutManager == null) {
                                layoutManager = LinearLayoutManager(requireContext())
                                isNestedScrollingEnabled = false
                            }
                            // Using a new adapter instance for simplicity as it's a full list update
                            // but ideally we'd use DiffUtil. For now, this is better than re-creating layoutManager.
                            adapter = CommentsAdapter(requireContext(), items)
                        }

                        showMoreBtn?.visibility = if (nextPageToken != null) View.VISIBLE else View.GONE
                        showMoreBtn?.text = "Show More"
                        showMoreBtn?.isEnabled = true
                    }
                }
                is YoutubeResource.Error -> {
                    if (youtubePlayerViewModel.commentNextPageToken == null) {
                        commentsCard?.visibility = View.GONE
                    }
                    showMoreBtn?.text = "Show More"
                    showMoreBtn?.isEnabled = true
                }
            }
        }

        databaseViewModel.isFavourite.observe(viewLifecycleOwner) { videoId ->
            btnLike.isChecked = videoId == currentVideoId
        }

        databaseViewModel.isSubscribed.observe(viewLifecycleOwner) { isSubscribed ->
            if (isSubscribed) {
                btnSubscribe.text = "Subscribed"
                btnSubscribe.setIconResource(R.drawable.rounded_done_24)
            } else {
                btnSubscribe.text = "Subscribe"
                btnSubscribe.setIconResource(0)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVideoUI(item: Item) {
        item.snippet?.let { snippet ->
            textTitle.text = snippet.title
            miniPlayerTitle.text = snippet.title

            // Set title on the custom player
            youtubePlayer.setTitle(snippet.title ?: "")

            view?.findViewById<TextView>(R.id.textDescription)?.text = snippet.description

            val viewCount = FormatUtils.formatCount(item.statistics?.viewCount?.toLong() ?: 0)
            val likeCount = FormatUtils.formatCount(item.statistics?.likeCount?.toLong() ?: 0)

            view?.findViewById<TextView>(R.id.metaInfoText)?.text = "$viewCount views"
            btnLike.text = likeCount

            setupLikeButton(snippet)
            handleRecentVideo(item)
            saveToPreferences(item, viewCount)
        }
    }

    private fun setupLikeButton(snippet: com.google.android.piyush.youtube.model.Snippet) {
        btnLike.addOnCheckedStateChangedListener { _, state ->
            val isChecked = state == MaterialCheckBox.STATE_CHECKED
            FormatUtils.animateBounce(btnLike)

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
            view?.findViewById<TextView>(R.id.channelName)?.text = snippet.title
            view?.findViewById<TextView>(R.id.channelSubscribers)?.text =
                "${FormatUtils.formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"

            val logoUrl = snippet.thumbnails?.default?.url
            val channelImage = view?.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.channelImage)
            if (channelImage != null) {
                Glide.with(this)
                    .load(logoUrl.takeUnless { it.isNullOrEmpty() } ?: Utilities.DEFAULT_LOGO)
                    .into(channelImage)
            }
            databaseViewModel.checkIsSubscribed(currentChannelId)
            setupSubscribeButton(item)
        }
    }

    private fun setupSubscribeButton(item: ChannelItem) {
        btnSubscribe.setOnClickListener {
            val isCurrentlySubscribed = databaseViewModel.isSubscribed.value ?: false
            if (isCurrentlySubscribed) {
                databaseViewModel.deleteSubscription(currentChannelId)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRecentVideo(item: Item) {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

        databaseViewModel.isRecentVideo(currentVideoId)
        databaseViewModel.isRecent.observe(viewLifecycleOwner) { recentVideoId ->
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
        requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit {
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

    fun setMotionProgress(progress: Float) {
        if (view != null) {
            motionLayout.progress = progress
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        val activity = requireActivity()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        youtubePlayer.setFullscreen(true)

        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Hide details, only the player
        view?.findViewById<View>(R.id.nestedScrollView)?.visibility = View.GONE
        btnMiniPlay.visibility = View.GONE
        btnMiniClose.visibility = View.GONE
        miniPlayerTitle.visibility = View.GONE
    }

    private fun exitFullscreen() {
        isFullscreen = false
        val activity = requireActivity()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        youtubePlayer.setFullscreen(false)

        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        view?.findViewById<View>(R.id.nestedScrollView)?.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        youtubePlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        youtubePlayer.onPause()
    }

    override fun onDestroyView() {
        youtubePlayer.release()
        super.onDestroyView()
    }
}
