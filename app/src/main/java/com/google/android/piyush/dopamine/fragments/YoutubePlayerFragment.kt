package com.google.android.piyush.dopamine.fragments

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.CommentsAdapter
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
import com.google.android.piyush.dopamine.player.NewPipeStreamExtractor
import com.google.android.piyush.dopamine.player.PlayerSettingsSheet
import com.google.android.piyush.dopamine.utilities.FormatUtils
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.SharedViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.channelDetails.Item as ChannelItem

import com.google.android.piyush.youtube.utilities.YoutubeResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels

@AndroidEntryPoint
@Suppress("DEPRECATION")
class YoutubePlayerFragment : Fragment() {

    private lateinit var motionLayout: MotionLayout
    private lateinit var youtubePlayer: ExoYouTubePlayer

    private lateinit var btnSubscribe: com.google.android.material.button.MaterialButton
    private lateinit var btnBell: ImageButton
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var btnPip: com.google.android.material.button.MaterialButton
    private lateinit var btnShare: com.google.android.material.button.MaterialButton
    private lateinit var btnDownload: com.google.android.material.button.MaterialButton
    
    private lateinit var btnLikeContainer: View
    private lateinit var btnDislikeContainer: View
    private lateinit var imgLike: ImageView
    private lateinit var imgDislike: ImageView
    private lateinit var txtLikeCount: TextView

    private lateinit var textTitle: TextView
    private lateinit var miniPlayerTitle: TextView
    private lateinit var btnMiniPlay: ImageButton
    private lateinit var btnMiniClose: ImageButton

    private val youtubePlayerViewModel: YoutubePlayerViewModel by viewModels()
    private val databaseViewModel: DatabaseViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var currentVideoId: String = ""
    private var currentChannelId: String = ""
    private var currentVideoTitle: String = ""
    private var isPlaying = false
    private var isFullscreen = false

    private var isLiked = false
    private var isDisliked = false
    private var bellState = 0 // 0=all, 1=personalized, 2=none

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupPlayer()
        setupClickListeners()
        setupObservers()
    }

    private fun initViews(view: View) {
        motionLayout = view.findViewById(R.id.player_motion_layout)
        youtubePlayer = view.findViewById(R.id.YtPlayer)
        textTitle = view.findViewById(R.id.textTitle)
        miniPlayerTitle = view.findViewById(R.id.miniPlayerTitle)
        btnMiniPlay = view.findViewById(R.id.btnMiniPlay)
        btnMiniClose = view.findViewById(R.id.btnMiniClose)
        
        btnSubscribe = view.findViewById(R.id.btnSubscribe)
        btnBell = view.findViewById(R.id.btnBell)
        btnSave = view.findViewById(R.id.btnAddToPlaylist)
        btnPip = view.findViewById(R.id.btnPip)
        btnShare = view.findViewById(R.id.btnShare)
        btnDownload = view.findViewById(R.id.btnDownload)
        
        btnLikeContainer = view.findViewById(R.id.btnLikeContainer)
        btnDislikeContainer = view.findViewById(R.id.btnDislikeContainer)
        imgLike = view.findViewById(R.id.imgLike)
        imgDislike = view.findViewById(R.id.imgDislike)
        txtLikeCount = view.findViewById(R.id.txtLikeCount)
    }


    private fun setupPlayer() {
        youtubePlayer.setShowControls(false)
        youtubePlayer.setCallback(object : ExoYouTubePlayer.PlayerCallback {
            override fun onReady() { Log.d(TAG, "Player ready") }
            override fun onPlaying() { isPlaying = true; updateMiniPlayerPlayButton() }
            override fun onPaused() { isPlaying = false; updateMiniPlayerPlayButton() }
            override fun onError(error: String) { Log.e(TAG, "Player error: $error") }
        })

        youtubePlayer.setOnBackListener {
            if (isFullscreen) exitFullscreen() else motionLayout.transitionToState(R.id.start)
        }

        youtubePlayer.setOnFullscreenListener { fullscreen ->
            if (fullscreen) enterFullscreen() else exitFullscreen()
        }

        youtubePlayer.setOnSettingsListener {
            PlayerSettingsSheet.newInstance(
                currentSpeed = youtubePlayer.getPlaybackSpeed(),
                videoId = currentVideoId,
                onSpeedSelected = { youtubePlayer.setPlaybackSpeed(it) },
                onQualitySelected = { youtubePlayer.switchStream(it.url) }
            ).show(parentFragmentManager, PlayerSettingsSheet.TAG)
        }

        motionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}
            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}
            override fun onTransitionCompleted(ml: MotionLayout?, currentId: Int) {
                youtubePlayer.setShowControls(currentId == R.id.end)
            }
            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
        })
    }

    private fun updateMiniPlayerPlayButton() {
        btnMiniPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }

    private fun setupClickListeners() {
        btnMiniPlay.setOnClickListener { youtubePlayer.togglePlayPause() }
        btnMiniClose.setOnClickListener { youtubePlayer.pause(); sharedViewModel.closePlayer() }
        view?.findViewById<View>(R.id.btnCollapse)?.setOnClickListener { motionLayout.transitionToState(R.id.start) }
        
        btnSave.setOnClickListener { AddToPlaylistSheet().show(parentFragmentManager, AddToPlaylistSheet.TAG) }
        btnPip.setOnClickListener { enterPipMode() }
        
        btnShare.setOnClickListener { shareVideo() }
        btnDownload.setOnClickListener { downloadVideo() }
        
        btnLikeContainer.setOnClickListener { toggleLike() }
        btnDislikeContainer.setOnClickListener { toggleDislike() }
        
        btnBell.setOnClickListener {
            bellState = (bellState + 1) % 3
            updateBellIcon()
            FormatUtils.animateBounce(btnBell)
            FormatUtils.triggerHaptic(btnBell)
        }

        view?.findViewById<View>(R.id.btnShowMore)?.setOnClickListener { toggleDescription() }
        view?.findViewById<View>(R.id.btnShowAllComments)?.setOnClickListener {
            if (youtubePlayerViewModel.commentNextPageToken != null) {
                youtubePlayerViewModel.getCommentThreads(currentVideoId, true)
            }
        }
    }

    private fun toggleLike() {
        isLiked = !isLiked
        if (isLiked) isDisliked = false
        updateLikeDislikeUI()
        FormatUtils.animateLikeBurst(imgLike)
        FormatUtils.triggerHaptic(btnLikeContainer)
        
        if (isLiked) {
            databaseViewModel.insertFavouriteVideos(EntityFavouritePlaylist(
                videoId = currentVideoId, thumbnail = cachedThumbnail, title = currentVideoTitle,
                channelId = currentChannelId, channelTitle = cachedChannelTitle
            ))
        } else {
            databaseViewModel.deleteFavouriteVideo(currentVideoId)
        }
    }

    private fun toggleDislike() {
        isDisliked = !isDisliked
        if (isDisliked && isLiked) {
            isLiked = false
            databaseViewModel.deleteFavouriteVideo(currentVideoId)
        }
        updateLikeDislikeUI()
        FormatUtils.animateLikeBurst(imgDislike)
        FormatUtils.triggerHaptic(btnDislikeContainer)
    }

    private fun updateLikeDislikeUI() {
        val context = context ?: return
        val activeColor = MaterialColors.getColor(view ?: return, com.google.android.material.R.attr.colorPrimary)
        val defaultColor = MaterialColors.getColor(view ?: return, com.google.android.material.R.attr.colorOnSecondaryContainer)

        imgLike.setImageResource(if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_video)
        imgLike.imageTintList = ColorStateList.valueOf(if (isLiked) activeColor else defaultColor)
        
        imgDislike.setImageResource(if (isDisliked) R.drawable.ic_dislike_filled else R.drawable.ic_dislike)
        imgDislike.imageTintList = ColorStateList.valueOf(if (isDisliked) activeColor else defaultColor)
    }

    private fun updateBellIcon() {
        btnBell.setImageResource(if (bellState == 0) R.drawable.ic_notifications_active else R.drawable.ic_notifications)
    }

    private var isDescriptionExpanded = false
    private fun toggleDescription() {
        val textDesc = view?.findViewById<TextView>(R.id.textDescription) ?: return
        val btnMore = view?.findViewById<TextView>(R.id.btnShowMore) ?: return
        isDescriptionExpanded = !isDescriptionExpanded
        textDesc.maxLines = if (isDescriptionExpanded) Integer.MAX_VALUE else 3
        btnMore.text = if (isDescriptionExpanded) "Show Less" else "Show More"
    }

    private fun shareVideo() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, currentVideoTitle)
            putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$currentVideoId")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun downloadVideo() {
        val view = view ?: return
        Snackbar.make(view, "Preparing download...", Snackbar.LENGTH_SHORT).show()
        scope.launch {
            try {
                val streamUrl = NewPipeStreamExtractor.extractStreamUrl(currentVideoId)
                if (streamUrl != null) {
                    val request = android.app.DownloadManager.Request(Uri.parse(streamUrl))
                        .setTitle(currentVideoTitle.ifEmpty { "Dopamine Video" })
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "Dopamine/${currentVideoTitle.take(50).replace("[^a-zA-Z0-9 ]".toRegex(), "")}.mp4")
                    (requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
                    Snackbar.make(view, "Download started", Snackbar.LENGTH_SHORT).show()
                } else Snackbar.make(view, "Could not extract stream", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) { Snackbar.make(view, "Download failed", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun setupObservers() {
        sharedViewModel.currentVideo.observe(viewLifecycleOwner) { video ->
            if (video != null && currentVideoId != video.videoId) {
                currentVideoId = video.videoId ?: ""
                currentChannelId = video.channelId ?: ""
                currentVideoTitle = video.title ?: ""
                textTitle.text = video.title
                miniPlayerTitle.text = video.title
                youtubePlayer.loadVideo(currentVideoId)
                fetchVideoDetails()
            }
        }
        setupInnerObservers()
    }

    private fun fetchVideoDetails() {
        youtubePlayerViewModel.getVideoDetails(currentVideoId)
        youtubePlayerViewModel.getChannelDetails(currentChannelId)
        youtubePlayerViewModel.getChannelsPlaylist(currentChannelId)
        youtubePlayerViewModel.getCommentThreads(currentVideoId)
        databaseViewModel.isFavouriteVideo(currentVideoId)
    }

    private var cachedThumbnail: String? = null
    private var cachedChannelTitle: String? = null

    private fun setupInnerObservers() {
        youtubePlayerViewModel.videoDetails.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) resource.data.items?.firstOrNull()?.let { updateVideoUI(it) }
        }
        youtubePlayerViewModel.channelDetails.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) resource.data.items?.firstOrNull()?.let { updateChannelUI(it) }
        }
        youtubePlayerViewModel.channelsPlaylists.observe(viewLifecycleOwner) { resource ->
            if (resource is YoutubeResource.Success) {
                val rv = view?.findViewById<RecyclerView>(R.id.channelsPlaylist)
                rv?.layoutManager = LinearLayoutManager(requireContext())
                rv?.adapter = YoutubeChannelPlaylistsAdapter(requireContext(), resource.data)
            }
        }
        youtubePlayerViewModel.commentThreads.observe(viewLifecycleOwner) { resource ->
            updateCommentsUI(resource)
        }
        databaseViewModel.isFavourite.observe(viewLifecycleOwner) { isLiked = it == currentVideoId; updateLikeDislikeUI() }
        databaseViewModel.isSubscribed.observe(viewLifecycleOwner) { updateSubscribeUI(it) }
    }

    private fun updateCommentsUI(resource: YoutubeResource<com.google.android.piyush.youtube.model.CommentThreads>) {
        val commentsCard = view?.findViewById<View>(R.id.commentsCard) ?: return
        val showMoreBtn = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowAllComments)
        when (resource) {
            is YoutubeResource.Success -> {
                val items = resource.data.items ?: emptyList()
                commentsCard.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                view?.findViewById<TextView>(R.id.textCommentsHeader)?.text = "Comments (${items.size})"
                val rv = view?.findViewById<RecyclerView>(R.id.commentsRecyclerView)
                rv?.layoutManager = LinearLayoutManager(requireContext()).apply { isItemPrefetchEnabled = true }
                rv?.adapter = CommentsAdapter(requireContext(), items)
                showMoreBtn?.visibility = if (resource.data.nextPageToken != null) View.VISIBLE else View.GONE
            }
            else -> {}
        }
    }

    private fun updateVideoUI(item: Item) {
        item.snippet?.let {
            currentVideoTitle = it.title ?: ""
            cachedThumbnail = it.thumbnails?.high?.url
            cachedChannelTitle = it.channelTitle
            textTitle.text = it.title
            youtubePlayer.setTitle(it.title ?: "")
            view?.findViewById<TextView>(R.id.textDescription)?.text = it.description
            val views = FormatUtils.formatCount(item.statistics?.viewCount?.toLong() ?: 0)
            view?.findViewById<TextView>(R.id.metaInfoText)?.text = "$views views"
            txtLikeCount.text = FormatUtils.formatCount(item.statistics?.likeCount?.toLong() ?: 0)
            handleRecentVideo(item)
            saveToPreferences(item, views)
        }
    }

    private fun updateChannelUI(item: ChannelItem) {
        item.snippet?.let {
            view?.findViewById<TextView>(R.id.channelName)?.text = it.title
            view?.findViewById<TextView>(R.id.channelSubscribers)?.text = 
                "${FormatUtils.formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"
            val iv = view?.findViewById<ImageView>(R.id.channelImage)
            if (iv != null) Glide.with(this).load(it.thumbnails?.default?.url ?: Utilities.DEFAULT_LOGO).into(iv)
            databaseViewModel.checkIsSubscribed(currentChannelId)
            setupSubscribeButton(item)
        }
    }

    private fun updateSubscribeUI(isSubscribed: Boolean) {
        if (isSubscribed) {
            btnSubscribe.text = "Subscribed"
            btnSubscribe.setIconResource(R.drawable.rounded_done_24)
            btnSubscribe.setBackgroundColor(MaterialColors.getColor(btnSubscribe, com.google.android.material.R.attr.colorSurfaceContainerHigh))
            btnSubscribe.setTextColor(MaterialColors.getColor(btnSubscribe, com.google.android.material.R.attr.colorOnSurface))
            btnSubscribe.iconTint = ColorStateList.valueOf(MaterialColors.getColor(btnSubscribe, com.google.android.material.R.attr.colorOnSurface))
            btnBell.visibility = View.VISIBLE
        } else {
            btnSubscribe.text = "Subscribe"
            btnSubscribe.setIconResource(0)
            btnSubscribe.setBackgroundColor(MaterialColors.getColor(btnSubscribe, com.google.android.material.R.attr.colorOnSurface))
            btnSubscribe.setTextColor(MaterialColors.getColor(btnSubscribe, com.google.android.material.R.attr.colorSurface))
            btnBell.visibility = View.GONE
        }
    }

    private fun setupSubscribeButton(item: ChannelItem) {
        btnSubscribe.setOnClickListener {
            val isSub = databaseViewModel.isSubscribed.value ?: false
            FormatUtils.animateSubscribe(btnSubscribe); FormatUtils.triggerHaptic(btnSubscribe)
            if (isSub) {
                MaterialAlertDialogBuilder(requireContext()).setTitle("Unsubscribe")
                    .setMessage("Unsubscribe from ${item.snippet?.title}?").setPositiveButton("Unsubscribe") { _,_ -> databaseViewModel.deleteSubscription(currentChannelId) }
                    .setNegativeButton("Cancel", null).show()
            } else {
                item.snippet?.let { databaseViewModel.insertSubscription(com.google.android.piyush.database.entities.SubscriptionEntity(
                    currentChannelId, it.title ?: "", it.description, it.thumbnails?.default?.url, it.title)) }
            }
        }
    }

    private fun handleRecentVideo(item: Item) {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        databaseViewModel.isRecentVideo(currentVideoId)
        databaseViewModel.isRecent.observe(viewLifecycleOwner) { if (it == currentVideoId) databaseViewModel.updateRecentVideo(currentVideoId, time) else databaseViewModel.insertRecentVideos(EntityRecentVideos(0, currentVideoId, item.snippet?.thumbnails?.high?.url, item.snippet?.title, time, currentChannelId)) }
    }

    private fun saveToPreferences(item: Item, views: String) {
        requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit {
            putString(KEY_VIDEO_ID, currentVideoId); putString("thumbnail", item.snippet?.thumbnails?.high?.url)
            putString("title", item.snippet?.title); putString(KEY_CHANNEL_ID, currentChannelId)
            putString("channelTitle", item.snippet?.channelTitle); putString("viewCount", views)
            putString("publishedAt", item.snippet?.publishedAt); putString("duration", item.contentDetails?.duration)
        }
    }

    fun setMotionProgress(progress: Float) {
        if (view != null) {
            motionLayout.progress = progress
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            requireActivity().enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16, 9)).build())
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        youtubePlayer.setFullscreen(true)
        val wc = WindowCompat.getInsetsController(requireActivity().window, requireActivity().window.decorView)
        wc.hide(WindowInsetsCompat.Type.systemBars()); wc.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        view?.findViewById<View>(R.id.nestedScrollView)?.visibility = View.GONE
        btnMiniPlay.visibility = View.GONE; btnMiniClose.visibility = View.GONE; miniPlayerTitle.visibility = View.GONE
    }

    private fun exitFullscreen() {
        isFullscreen = false
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        youtubePlayer.setFullscreen(false)
        WindowCompat.getInsetsController(requireActivity().window, requireActivity().window.decorView).show(WindowInsetsCompat.Type.systemBars())
        view?.findViewById<View>(R.id.nestedScrollView)?.visibility = View.VISIBLE
    }

    override fun onResume() { super.onResume(); youtubePlayer.onResume() }
    override fun onPause() { super.onPause(); youtubePlayer.onPause() }
    override fun onDestroyView() { youtubePlayer.release(); scope.cancel(); super.onDestroyView() }
}

