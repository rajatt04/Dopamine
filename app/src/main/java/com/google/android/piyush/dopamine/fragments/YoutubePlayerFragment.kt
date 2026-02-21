package com.google.android.piyush.dopamine.fragments

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
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
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
import kotlin.random.Random

@Suppress("DEPRECATION")
class YoutubePlayerFragment : Fragment() {

    private lateinit var motionLayout: MotionLayout
    private lateinit var youtubePlayer: ExoYouTubePlayer

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

    private val decimalFormatter by lazy { DecimalFormat("#0.0") }
    private val integerFormatter by lazy { DecimalFormat("#,##0") }

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
        textTitle = view.findViewById(R.id.textTitle)
        miniPlayerTitle = view.findViewById(R.id.miniPlayerTitle)
        btnMiniPlay = view.findViewById(R.id.btnMiniPlay)
        btnMiniClose = view.findViewById(R.id.btnMiniClose)
        btnLike = view.findViewById(R.id.btnLike)
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

        view?.findViewById<View>(R.id.btnCustomPlaylist)?.setOnClickListener {
            AddToPlaylistSheet().show(parentFragmentManager, AddToPlaylistSheet.TAG)
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

        databaseViewModel.isFavourite.observe(viewLifecycleOwner) { videoId ->
            btnLike.isChecked = videoId == currentVideoId
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVideoUI(item: Item) {
        item.snippet?.let { snippet ->
            textTitle.text = snippet.title
            miniPlayerTitle.text = snippet.title

            view?.findViewById<TextView>(R.id.textDescription)?.text = snippet.description

            val viewCount = formatCount(item.statistics?.viewCount?.toLong() ?: 0)
            val likeCount = formatCount(item.statistics?.likeCount?.toLong() ?: 0)

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
            animateLikeButton(btnLike)

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
                "${formatCount(item.statistics?.subscriberCount?.toLong() ?: 0)} Subscribers"

            val logoUrl = snippet.thumbnails?.default?.url
            val channelImage = view?.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.channelImage)
            if (channelImage != null) {
                Glide.with(this)
                    .load(logoUrl.takeUnless { it.isNullOrEmpty() } ?: Utilities.DEFAULT_LOGO)
                    .into(channelImage)
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

    fun setMotionProgress(progress: Float) {
        if (view != null) {
            motionLayout.progress = progress
        }
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
