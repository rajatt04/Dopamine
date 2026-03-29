package com.google.android.piyush.dopamine.activities

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.ContentValues.TAG
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.HeroCarouselStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.ChaptersAdapter
import com.google.android.piyush.dopamine.adapters.CommentsAdapter
import com.google.android.piyush.dopamine.adapters.CustomPlaylistsAdapter
import com.google.android.piyush.dopamine.adapters.VideoData
import com.google.android.piyush.dopamine.adapters.YoutubeChannelPlaylistsAdapter
import com.google.android.piyush.dopamine.databinding.ActivityYoutubePlayerBinding
import com.google.android.piyush.dopamine.services.YouTubePlaybackService
import com.google.android.piyush.dopamine.utilities.AnalyticsHelper
import com.google.android.piyush.dopamine.utilities.ChapterParser
import com.google.android.piyush.dopamine.utilities.CustomDialog
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModel
import com.google.android.piyush.dopamine.viewModels.YoutubePlayerViewModelFactory
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private var youTubePlayer: YouTubePlayer? = null
    private var commentsAdapter: CommentsAdapter? = null

    private var playbackService: YouTubePlaybackService? = null
    private var serviceBound = false

    private var currentVideoId: String = ""
    private var currentVideoTitle: String = ""
    private var currentChannelTitle: String = ""
    private var currentThumbnail: String = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as YouTubePlaybackService.LocalBinder
            playbackService = binder.getService()
            serviceBound = true
            playbackService?.youTubePlayer?.let { player ->
                this@YoutubePlayer.youTubePlayer = player
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            serviceBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        youtubeRepositoryImpl = YoutubeRepositoryImpl()
        youtubePlayerViewModelFactory = YoutubePlayerViewModelFactory(youtubeRepositoryImpl)
        databaseViewModel = DatabaseViewModel(applicationContext)
        youtubePlayerViewModel = ViewModelProvider(
            this, youtubePlayerViewModelFactory
        )[YoutubePlayerViewModel::class.java]

        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentVideoId = intent?.getStringExtra("videoId").toString()

        startAndBindService()

        databaseViewModel.isFavouriteVideo(currentVideoId)
        databaseViewModel.isFavourite.observe(this) {
            binding.addToPlayList.isChecked = it == currentVideoId
        }

        binding.YtPlayer.enableBackgroundPlayback(true)
        binding.YtPlayer.enableAutomaticInitialization = false
        val iFramePlayerOptions = IFramePlayerOptions.Builder(this@YoutubePlayer)
            .rel(1)
            .controls(1)
            .fullscreen(1)
            .build()

        binding.YtPlayer.initialize(youTubePlayerListener = object :
            AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                this@YoutubePlayer.youTubePlayer = youTubePlayer
                playbackService?.youTubePlayer = youTubePlayer
                youTubePlayer.apply {
                    loadVideo(currentVideoId, 0F)
                    Log.d(TAG, " -> Activity : YoutubePlayer || videoId : $currentVideoId")
                }
            }
        }, true, iFramePlayerOptions)

        binding.YtPlayer.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                listOf(
                    binding.YtPlayer,
                    binding.addToPlayList,
                    binding.addToCustomPlayList
                ).forEach { it.visibility = View.GONE }

                if (fullscreenView.parent == null) {
                    binding.frameLayout.addView(fullscreenView)
                }
            }

            override fun onExitFullscreen() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                binding.frameLayout.removeAllViews()
                listOf(
                    binding.YtPlayer,
                    binding.addToPlayList,
                    binding.addToCustomPlayList
                ).forEach { it.visibility = View.VISIBLE }
            }
        })

        binding.enterInPip.setOnClickListener {
            val supportsPIP =
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            if (supportsPIP) enterPictureInPictureMode()
        }

        setupServiceObserver()

        val channelId = intent?.getStringExtra("channelId").toString()

        youtubePlayerViewModel.loadVideoData(currentVideoId, channelId)

        youtubePlayerViewModel.videoDetails.observe(this) { videoDetails ->
            when (videoDetails) {
                is NetworkResult.Loading -> {}

                is NetworkResult.Success -> {
                    val videoTitle = videoDetails.data.items?.get(0)?.snippet?.title
                    val videoDescription = videoDetails.data.items?.get(0)?.snippet?.description
                    val videoThumbnail = videoDetails.data.items?.get(0)?.snippet?.thumbnails?.high?.url
                    val videoDuration = videoDetails.data.items?.get(0)?.contentDetails?.duration
                    val videoPublishedAt = videoDetails.data.items?.get(0)?.snippet?.publishedAt
                    val channelTitle = videoDetails.data.items?.get(0)?.snippet?.channelTitle
                    val videoLikes = counter(videoDetails.data.items?.get(0)?.statistics?.likeCount!!.toInt())
                    val videoViews = counter(videoDetails.data.items?.get(0)?.statistics?.viewCount!!.toInt())

                    currentVideoTitle = videoTitle ?: ""
                    currentChannelTitle = channelTitle ?: ""
                    currentThumbnail = videoThumbnail ?: ""

                    updateServiceWithVideoInfo()

                    binding.apply {
                        textTitle.text  = videoTitle
                        textLiked.text  = videoLikes
                        textView.text   = videoViews
                        textDescription.text = videoDescription

                        // Show more / Show less toggle
                        textDescription.post {
                            val lineCount = textDescription.lineCount
                            if (lineCount > 4) {
                                btnShowMore.visibility = View.VISIBLE
                            }
                        }

                        var isExpanded = false
                        btnShowMore.setOnClickListener {
                            isExpanded = !isExpanded
                            if (isExpanded) {
                                textDescription.maxLines = Int.MAX_VALUE
                                textDescription.ellipsize = null
                                btnShowMore.text = "Show less"
                            } else {
                                textDescription.maxLines = 4
                                textDescription.ellipsize = android.text.TextUtils.TruncateAt.END
                                btnShowMore.text = "Show more"
                            }
                        }

                        AnalyticsHelper.logVideoPlay(currentVideoId, videoTitle, channelTitle)
                        AnalyticsHelper.setCustomKey("current_video_id", currentVideoId)

                        addToPlayList.addOnCheckedStateChangedListener { _, isFavourite ->
                            if (isFavourite == 1) {
                                databaseViewModel.insertFavouriteVideos(
                                    EntityFavouritePlaylist(
                                        videoId = currentVideoId,
                                        thumbnail = videoThumbnail,
                                        title = videoTitle,
                                        channelId = channelId,
                                        channelTitle = channelTitle
                                    )
                                )
                            } else {
                                databaseViewModel.deleteFavouriteVideo(
                                    videoId = currentVideoId
                                )
                            }
                        }
                    }

                    val chapters = ChapterParser.parseChapters(videoDescription)
                    if (chapters.isNotEmpty()) {
                        binding.chaptersHeader.visibility = View.VISIBLE
                        binding.chaptersRecyclerView.visibility = View.VISIBLE
                        binding.chaptersRecyclerView.apply {
                            layoutManager = LinearLayoutManager(this@YoutubePlayer)
                            adapter = ChaptersAdapter(this@YoutubePlayer, chapters) { chapter ->
                                youTubePlayer?.seekTo(chapter.startTimeSeconds.toFloat())
                            }
                        }
                    }

                    databaseViewModel.isRecentVideo(videoId = currentVideoId)

                    databaseViewModel.isRecent.observe(this) {
                        if (it == currentVideoId) {
                            databaseViewModel.updateRecentVideo(
                                videoId = currentVideoId,
                                time = LocalTime.now()
                                    .format(DateTimeFormatter.ofPattern("hh:mm a")).toString())
                        } else {
                            databaseViewModel.insertRecentVideos(
                                EntityRecentVideos(
                                    id = Random.nextInt(1, 100000),
                                    videoId = currentVideoId,
                                    thumbnail = videoThumbnail,
                                    title = videoTitle,
                                    timing = LocalTime.now()
                                        .format(DateTimeFormatter.ofPattern("hh:mm a"))
                                        .toString(),
                                    channelId = channelId
                                )
                            )
                        }
                    }

                    getSharedPreferences("customPlaylist", MODE_PRIVATE).edit {
                        putString("videoId", currentVideoId)
                        putString("thumbnail", videoThumbnail)
                        putString("title", videoTitle)
                        putString("channelId", channelId)
                        putString("channelTitle", channelTitle)
                        putString("viewCount", videoViews)
                        putString("publishedAt",  videoPublishedAt)
                        putString("duration", videoDuration)
                    }
                }

                is NetworkResult.Error -> {
                    Log.d("YoutubePlayer", "YoutubePlayer: ${videoDetails.message}")
                }
            }
        }

        youtubePlayerViewModel.channelDetails.observe(this) { channelDetails ->
            when (channelDetails) {
                is NetworkResult.Loading -> {}

                is NetworkResult.Success -> {
                    val channelLogo = channelDetails.data.items?.get(0)?.snippet?.thumbnails?.default?.url
                    val channelSubscribers = "${counter(channelDetails.data.items?.get(0)?.statistics?.subscriberCount!!.toInt())} Subscribers"
                    val channelTitle = channelDetails.data.items?.get(0)?.snippet?.title
                    val customUrl = channelDetails.data.items?.get(0)?.snippet?.customUrl
                    val channelDescription = channelDetails.data.items?.get(0)?.snippet?.description

                    if(channelLogo.isNullOrEmpty()){
                        Glide.with(this).load(Utilities.DEFAULT_LOGO).into(binding.imageView)
                    }else {
                        Glide.with(this).load(channelLogo).into(binding.imageView)
                    }
                    binding.apply {
                        this.text1.text = channelTitle
                        this.text2.text = customUrl
                        if (channelSubscribers.isNotEmpty()) {
                            this.text3.text = channelSubscribers
                            this.text3.visibility = View.VISIBLE
                        }
                    }
                }

                is NetworkResult.Error -> {
                    Log.d(TAG, "YoutubePlayer: ${channelDetails.message}")
                }
            }
        }

        youtubePlayerViewModel.channelsPlaylists.observe(this) { channelsPlaylist ->
            when (channelsPlaylist) {
                is NetworkResult.Loading -> {
                    binding.moreFromChannelHeader.visibility = View.GONE
                    binding.channelsPlaylist.visibility = View.GONE
                }

                is NetworkResult.Success -> {
                    if (!channelsPlaylist.data.items.isNullOrEmpty()) {
                        binding.moreFromChannelHeader.visibility = View.VISIBLE
                        binding.channelsPlaylist.visibility = View.VISIBLE
                        binding.channelsPlaylist.apply {
                            layoutManager = CarouselLayoutManager(HeroCarouselStrategy())
                            adapter = YoutubeChannelPlaylistsAdapter(this@YoutubePlayer, channelsPlaylist.data)
                        }
                    } else {
                        binding.moreFromChannelHeader.visibility = View.GONE
                        binding.channelsPlaylist.visibility = View.GONE
                    }
                }

                is NetworkResult.Error -> {
                    Log.d(TAG, "YoutubePlayer Playlists Error: ${channelsPlaylist.message}")
                    binding.moreFromChannelHeader.visibility = View.GONE
                    binding.channelsPlaylist.visibility = View.GONE
                }
            }
        }

        youtubePlayerViewModel.comments.observe(this) { comments ->
            when (comments) {
                is NetworkResult.Loading -> {}
                is NetworkResult.Success -> {
                    val items = comments.data.items
                    if (!items.isNullOrEmpty()) {
                        binding.commentsHeader.visibility = View.VISIBLE
                        binding.commentsRecyclerView.visibility = View.VISIBLE
                        if (commentsAdapter == null) {
                            commentsAdapter = CommentsAdapter(
                                this,
                                items.toMutableList()
                            ) { /* reply click handler */ }
                            binding.commentsRecyclerView.apply {
                                layoutManager = LinearLayoutManager(this@YoutubePlayer)
                                adapter = commentsAdapter
                            }
                        } else {
                            commentsAdapter?.addComments(items)
                        }

                        // Show the toggle button only when there are more than 3 comments
                        if (commentsAdapter?.hasMore() == true) {
                            binding.btnShowMoreComments.visibility = View.VISIBLE
                            binding.btnShowMoreComments.setOnClickListener {
                                val expanded = commentsAdapter?.toggleShowAll() ?: false
                                binding.btnShowMoreComments.text =
                                    if (expanded) "Show less comments" else "Show more comments"
                            }
                        } else {
                            binding.btnShowMoreComments.visibility = View.GONE
                        }
                    }
                }
                is NetworkResult.Error -> {
                    Log.d(TAG, "Comments error: ${comments.message}")
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.YtPlayer.wrapContent()
            binding.addToPlayList.visibility = View.GONE
            binding.addToCustomPlayList.visibility = View.GONE
        } else {
            binding.addToPlayList.visibility = View.VISIBLE
            binding.addToCustomPlayList.visibility = View.VISIBLE
        }
    }

    private fun startAndBindService() {
        val intent = Intent(this, YouTubePlaybackService::class.java).apply {
            action = YouTubePlaybackService.ACTION_PLAY
            putExtra(YouTubePlaybackService.EXTRA_VIDEO_ID, currentVideoId)
            putExtra(YouTubePlaybackService.EXTRA_VIDEO_TITLE, "")
            putExtra(YouTubePlaybackService.EXTRA_CHANNEL_TITLE, "")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(Intent(this, YouTubePlaybackService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupServiceObserver() {
        lifecycleScope.launch {
            YouTubePlaybackService.isPlaying.collectLatest { isPlaying ->
                // Update UI based on playback state if needed
            }
        }

        lifecycleScope.launch {
            YouTubePlaybackService.currentVideoTitle.collectLatest { title ->
                if (title.isNullOrEmpty() && !currentVideoTitle.isEmpty()) {
                    updateServiceWithVideoInfo()
                }
            }
        }
    }

    private fun updateServiceWithVideoInfo() {
        if (currentVideoId.isNotEmpty()) {
            val intent = Intent(this, YouTubePlaybackService::class.java).apply {
                action = YouTubePlaybackService.ACTION_PLAY
                putExtra(YouTubePlaybackService.EXTRA_VIDEO_ID, currentVideoId)
                putExtra(YouTubePlaybackService.EXTRA_VIDEO_TITLE, currentVideoTitle)
                putExtra(YouTubePlaybackService.EXTRA_CHANNEL_TITLE, currentChannelTitle)
                putExtra(YouTubePlaybackService.EXTRA_THUMBNAIL_URL, currentThumbnail)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun counter(count : Int) : String{
        var num : Double = count.toDouble()
        val data: String
        if(num > 1000000.00){
            num /= 1000000.00
            num = DecimalFormat("#.##").format(num).toDouble()
            data = "${num}M"
        }else {
            num /= 1000
            num = DecimalFormat("#.##").format(num).toDouble()
            data = "${num}K"
        }
        return data
    }

    override fun onStop() {
        super.onStop()
        youTubePlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        playbackService?.youTubePlayer = youTubePlayer
    }
}

class MyBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var databaseViewModel: DatabaseViewModel

    private var videoData: VideoData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            videoData = VideoData(
                videoId = args.getString(ARG_VIDEO_ID, ""),
                title = args.getString(ARG_VIDEO_TITLE, ""),
                thumbnail = args.getString(ARG_THUMBNAIL, ""),
                channelId = args.getString(ARG_CHANNEL_ID, ""),
                viewCount = args.getString(ARG_VIEW_COUNT, ""),
                channelTitle = args.getString(ARG_CHANNEL_TITLE, ""),
                publishedAt = args.getString(ARG_PUBLISHED_AT, ""),
                duration = args.getString(ARG_DURATION, "")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_add_to_a_playlist, container, false)
        val createNewPlaylist: MaterialButton = view.findViewById(R.id.createNewPlayList)
        val customPlaylists: RecyclerView = view.findViewById(R.id.recyclerViewLocalPlaylist)
        databaseViewModel = DatabaseViewModel(requireContext())
        databaseViewModel.defaultMasterDev

        createNewPlaylist.setOnClickListener {
            val customDialog = CustomDialog(requireContext())
            customDialog.show()
        }

        if (FirebaseAuth.getInstance().currentUser?.email.isNullOrEmpty()) {
            if (!databaseViewModel.isPlaylistExist(databaseViewModel.isUserFromPhoneAuth)) {
                databaseViewModel.userFromPhoneAuth()
            } else {
                Log.d(TAG, "${databaseViewModel.isUserFromPhoneAuth} : Exists")
            }
        } else {
            if (!databaseViewModel.isPlaylistExist(databaseViewModel.newPlaylistName)) {
                databaseViewModel.defaultUserPlaylist()
            } else {
                Log.d(TAG, "${databaseViewModel.newPlaylistName} : Exists")
            }
        }

        customPlaylists.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CustomPlaylistsAdapter(
                requireContext(),
                databaseViewModel.getPlaylist(),
                videoData
            )
        }
        return view
    }

    companion object {
        private const val TAG = "MyBottomSheetFragment"
        private const val ARG_VIDEO_ID = "video_id"
        private const val ARG_VIDEO_TITLE = "video_title"
        private const val ARG_THUMBNAIL = "thumbnail"
        private const val ARG_CHANNEL_ID = "channel_id"
        private const val ARG_VIEW_COUNT = "view_count"
        private const val ARG_CHANNEL_TITLE = "channel_title"
        private const val ARG_PUBLISHED_AT = "published_at"
        private const val ARG_DURATION = "duration"

        fun newInstance(
            videoId: String,
            title: String,
            thumbnail: String,
            channelId: String,
            viewCount: String,
            channelTitle: String,
            publishedAt: String,
            duration: String
        ): MyBottomSheetFragment {
            return MyBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_ID, videoId)
                    putString(ARG_VIDEO_TITLE, title)
                    putString(ARG_THUMBNAIL, thumbnail)
                    putString(ARG_CHANNEL_ID, channelId)
                    putString(ARG_VIEW_COUNT, viewCount)
                    putString(ARG_CHANNEL_TITLE, channelTitle)
                    putString(ARG_PUBLISHED_AT, publishedAt)
                    putString(ARG_DURATION, duration)
                }
            }
        }
    }
}