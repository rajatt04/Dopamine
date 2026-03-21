package com.google.android.piyush.dopamine.fragments

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.DopamineUserProfile
import com.google.android.piyush.dopamine.activities.DopamineVideoWatchHistory
import com.google.android.piyush.dopamine.adapters.HomeAdapter
import com.google.android.piyush.dopamine.databinding.FragmentHomeBinding
import com.google.android.piyush.dopamine.utilities.AnalyticsHelper
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.android.piyush.youtube.viewModels.HomeViewModel
import com.google.android.piyush.youtube.viewModels.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.system.exitProcess

class Home : Fragment() {

    private var fragmentHomeBinding : FragmentHomeBinding? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var repository: YoutubeRepositoryImpl
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var homeAdapter: HomeAdapter

    private val categorySearchTerms = mapOf(
        R.id.chipAll to "",
        R.id.chipMusic to "Music",
        R.id.chipGaming to "Gaming",
        R.id.chipNews to "News",
        R.id.chipSports to "Sports",
        R.id.chipTech to "Technology"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentHomeBinding.bind(view)
        fragmentHomeBinding = binding
        repository = YoutubeRepositoryImpl()
        homeViewModelFactory = HomeViewModelFactory(repository)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()

        fragmentHomeBinding!!.greeting.text = getGreeting()
        Log.d(TAG, " -> Fragment : Home || Greeting : ${getGreeting()}")

        setupUserImage()
        setupClickListeners()
        setupCategoryChips()
        loadHomeVideos()
    }

    private fun setupUserImage() {
        val sharedPrefs = requireContext().getSharedPreferences("currentUser", android.content.Context.MODE_PRIVATE)
        val loginType = sharedPrefs.getString("loginType", "")
        val targetImageView = fragmentHomeBinding?.userImage ?: return

        if (loginType == "mobile") {
            val photoUrl = sharedPrefs.getString("photoUrl", "")
            if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.default_user)
                    .error(R.drawable.default_user)
                    .circleCrop()
                    .into(targetImageView)
            } else {
                targetImageView.setImageResource(R.drawable.default_user)
            }
        } else {
            // Google login — prefer the cached SharedPrefs URL (set by MainActivity),
            // fall back to live FirebaseAuth Uri, fall back to default image.
            val cachedUrl = sharedPrefs.getString("photoUrl", "")
            val firebasePhotoUrl = firebaseAuth.currentUser?.photoUrl?.toString()
            val bestUrl = when {
                !cachedUrl.isNullOrEmpty() && cachedUrl != "null" -> cachedUrl
                !firebasePhotoUrl.isNullOrEmpty() && firebasePhotoUrl != "null" -> firebasePhotoUrl
                else -> null
            }
            if (bestUrl != null) {
                Glide.with(this)
                    .load(bestUrl)
                    .placeholder(R.drawable.default_user)
                    .error(R.drawable.default_user)
                    .circleCrop()
                    .into(targetImageView)
            } else {
                targetImageView.setImageResource(R.drawable.default_user)
            }
        }
    }

    private fun setupClickListeners() {
        fragmentHomeBinding!!.watchHistory.setOnClickListener {
            startActivity(Intent(context, DopamineVideoWatchHistory::class.java))
        }

        fragmentHomeBinding!!.userImage.setOnClickListener {
            startActivity(Intent(context, DopamineUserProfile::class.java))
        }
    }

    private fun setupCategoryChips() {
        fragmentHomeBinding!!.categoryChips.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                val searchTerm = categorySearchTerms[chipId] ?: ""

                if (searchTerm.isEmpty()) {
                    loadHomeVideos()
                } else {
                    loadCategoryVideos(searchTerm)
                }

                AnalyticsHelper.breadcrumb("Category chip selected: $searchTerm")
            }
        }
    }

    private fun loadHomeVideos() {
        if (!NetworkUtilities.isNetworkAvailable(requireContext())) {
            NetworkUtilities.showNetworkError(requireContext())
            return
        }

        homeViewModel.videos.observe(viewLifecycleOwner) { videos ->
            when (videos) {
                is NetworkResult.Loading -> {
                    showLoading()
                }
                is NetworkResult.Success -> {
                    showVideos(videos.data)
                }
                is NetworkResult.Error -> {
                    showError(videos.message)
                }
            }
        }
    }

    private fun loadCategoryVideos(category: String) {
        if (!NetworkUtilities.isNetworkAvailable(requireContext())) {
            NetworkUtilities.showNetworkError(requireContext())
            return
        }

        showLoading()
        Log.d(TAG, "Loading category: $category")

        lifecycleScope.launch {
            // First get search results to get video IDs
            val searchResponse = repository.getSearchVideos(
                query = category,
                order = "viewCount",
                type = "video"
            )

            Log.d(TAG, "Category search response: $searchResponse")

            when (searchResponse) {
                is NetworkResult.Success -> {
                    val searchItems = searchResponse.data.items
                    Log.d(TAG, "Category search items count: ${searchItems?.size ?: 0}")
                    
                    if (!searchItems.isNullOrEmpty()) {
                        // Get video IDs
                        val videoIds = searchItems.mapNotNull { it.id?.videoId }.take(20)
                        
                        if (videoIds.isNotEmpty()) {
                            // Now fetch full video details with statistics and duration
                            val videoIdsString = videoIds.joinToString(",")
                            val videosResponse = repository.getVideosByIds(videoIdsString)
                            
                            when (videosResponse) {
                                is NetworkResult.Success -> {
                                    if (!videosResponse.data.items.isNullOrEmpty()) {
                                        showVideos(videosResponse.data)
                                    } else {
                                        // Fallback: create items from search with minimal data
                                        val fallbackResponse = createFallbackYoutubeResponse(searchItems)
                                        showVideos(fallbackResponse)
                                    }
                                }
                                is NetworkResult.Error -> {
                                    Log.e(TAG, "Videos fetch error: ${videosResponse.message}")
                                    // Fallback to search data without stats
                                    val fallbackResponse = createFallbackYoutubeResponse(searchItems)
                                    showVideos(fallbackResponse)
                                }
                                is NetworkResult.Loading -> {}
                            }
                        } else {
                            showError("No videos found for \"$category\"")
                        }
                    } else {
                        showError("No videos found for \"$category\"")
                    }
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Category error: ${searchResponse.message}", searchResponse.exception)
                    showError("Failed to load $category: ${searchResponse.message}")
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private suspend fun createFallbackYoutubeResponse(
        searchItems: List<com.google.android.piyush.youtube.model.SearchTubeItems>
    ): Youtube {
        return Youtube(
            items = searchItems.mapNotNull { searchItem ->
                searchItem.id?.videoId?.let { videoId ->
                    com.google.android.piyush.youtube.model.Item(
                        id = videoId,
                        snippet = searchItem.snippet?.let { snippet ->
                            com.google.android.piyush.youtube.model.Snippet(
                                publishedAt = snippet.publishedAt,
                                channelId = snippet.channelId,
                                title = snippet.title,
                                description = snippet.description,
                                thumbnails = snippet.thumbnails?.let { thumbs ->
                                    com.google.android.piyush.youtube.model.Thumbnails(
                                        default = thumbs.default?.let { t ->
                                            com.google.android.piyush.youtube.model.Default(url = t.url)
                                        },
                                        medium = thumbs.medium?.let { t ->
                                            com.google.android.piyush.youtube.model.Medium(url = t.url)
                                        },
                                        high = thumbs.high?.let { t ->
                                            com.google.android.piyush.youtube.model.High(url = t.url)
                                        }
                                    )
                                },
                                channelTitle = snippet.channelTitle
                            )
                        },
                        // Add default statistics so it doesn't crash
                        statistics = com.google.android.piyush.youtube.model.Statistics(viewCount = 0),
                        contentDetails = com.google.android.piyush.youtube.model.ContentDetails(duration = "PT0S")
                    )
                }
            }
        )
    }

    private fun showLoading() {
        fragmentHomeBinding?.shimmerRecyclerView?.visibility = View.VISIBLE
        fragmentHomeBinding?.shimmerRecyclerView?.startShimmer()
        fragmentHomeBinding?.recyclerView?.visibility = View.GONE
    }

    private fun showVideos(youtube: Youtube) {
        fragmentHomeBinding?.shimmerRecyclerView?.visibility = View.INVISIBLE
        fragmentHomeBinding?.shimmerRecyclerView?.stopShimmer()
        fragmentHomeBinding?.recyclerView?.apply {
            visibility = View.VISIBLE
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            homeAdapter = HomeAdapter(requireContext(), youtube)
            adapter = homeAdapter
        }
    }

    private fun showError(message: String) {
        fragmentHomeBinding?.shimmerRecyclerView?.visibility = View.INVISIBLE
        fragmentHomeBinding?.shimmerRecyclerView?.stopShimmer()

        Log.d(TAG, "Error: $message")
        context?.let { ctx ->
            MaterialAlertDialogBuilder(ctx)
                .apply {
                    this.setTitle("Something went wrong")
                    this.setMessage(message)
                    this.setIcon(R.drawable.ic_dialog_error)
                    this.setCancelable(false)
                    this.setNegativeButton("Cancel") { dialog, _ ->
                        dialog?.dismiss()
                    }
                    this.setPositiveButton("Retry") { _, _ ->
                        val checkedChipId = fragmentHomeBinding?.categoryChips?.checkedChipId
                        if (checkedChipId == R.id.chipAll || checkedChipId == null) {
                            loadHomeVideos()
                        } else {
                            val searchTerm = categorySearchTerms[checkedChipId] ?: ""
                            loadCategoryVideos(searchTerm)
                        }
                    }.create().show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentHomeBinding = null
        homeViewModel.videos.removeObservers(viewLifecycleOwner)
    }

    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hourOfDay) {
            in 6..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..23 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
