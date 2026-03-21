package com.google.android.piyush.dopamine.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.DopamineUserProfile
import com.google.android.piyush.dopamine.adapters.SearchAdapter
import com.google.android.piyush.dopamine.adapters.SearchHistoryAdapter
import com.google.android.piyush.dopamine.databinding.FragmentSearchBinding
import com.google.android.piyush.dopamine.utilities.AnalyticsHelper
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.utilities.ToastUtilities
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.SearchViewModel
import com.google.android.piyush.dopamine.viewModels.SearchViewModelFactory
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.random.Random

class Search : Fragment() {
    private var fragmentSearchBinding: FragmentSearchBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var youtubeRepositoryImpl: YoutubeRepositoryImpl
    private lateinit var searchViewModelFactory: SearchViewModelFactory

    private var currentSearchOrder = "relevance"
    private var currentSearchType: String? = null

    private val trendingSearches = listOf(
        "Trending Music 2024",
        "Latest Tech Reviews",
        "Gaming Highlights",
        "Movie Trailers",
        "Cooking Tutorials",
        "Fitness Workouts",
        "Travel Vlogs",
        "News Today"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceSearch()
        } else {
            ToastUtilities.showToast(requireContext(), "Microphone permission required for voice search")
        }
    }

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                fragmentSearchBinding?.searchVideo?.setQuery(results[0], true)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSearchBinding.bind(view)
        fragmentSearchBinding = binding
        firebaseAuth = FirebaseAuth.getInstance()
        youtubeRepositoryImpl = YoutubeRepositoryImpl()
        searchViewModelFactory = SearchViewModelFactory(youtubeRepositoryImpl)
        searchViewModel = ViewModelProvider(this, searchViewModelFactory)[SearchViewModel::class.java]
        databaseViewModel = DatabaseViewModel(context?.applicationContext!!)

        setupUserImage()
        setupSearchHistory()
        setupSearchView()
        setupFilterChips()
        setupVoiceSearch()
        setupTrendingSearches()
    }

    private fun setupUserImage() {
        val sharedPrefs = context?.getSharedPreferences("currentUser", android.content.Context.MODE_PRIVATE)
        val loginType = sharedPrefs?.getString("loginType", "")
        val targetImageView = fragmentSearchBinding?.userImage ?: return

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
            val cachedUrl = sharedPrefs?.getString("photoUrl", "")
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

        targetImageView.setOnClickListener {
            startActivity(Intent(context, DopamineUserProfile::class.java))
        }
    }

    private fun setupSearchHistory() {
        databaseViewModel.getSearchVideoList()

        databaseViewModel.searchVideoHistory.observe(viewLifecycleOwner) { history ->
            if (history.isNullOrEmpty()) {
                showTrendingSearches()
            } else {
                showSearchHistory(history)
            }
        }

        fragmentSearchBinding!!.clearAll.setOnClickListener {
            databaseViewModel.deleteSearchVideoList()
            showTrendingSearches()
            ToastUtilities.showToast(requireContext(), "Search History Cleared")
        }
    }

    private fun showSearchHistory(history: List<EntityVideoSearch>) {
        fragmentSearchBinding?.apply {
            searchEffect.visibility = View.GONE
            clearAll.visibility = View.VISIBLE
            filterScrollView.visibility = View.GONE
            trendingHeader?.visibility = View.GONE
            trendingChips?.visibility = View.GONE
            utilList.visibility = View.VISIBLE
            utilList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = SearchHistoryAdapter(
                    history,
                    onItemClick = { query ->
                        searchVideo.setQuery(query, true)
                    },
                    onDeleteClick = { item ->
                        ToastUtilities.showToast(requireContext(), "Deleted: ${item.search}")
                    }
                )
            }
        }
    }

    private fun showTrendingSearches() {
        fragmentSearchBinding?.apply {
            searchEffect.visibility = View.GONE
            clearAll.visibility = View.VISIBLE
            clearAllText.text = "Clear History"
            filterScrollView.visibility = View.GONE
            utilList.visibility = View.GONE

            trendingHeader?.visibility = View.VISIBLE
            trendingChips?.visibility = View.VISIBLE

            trendingChips?.removeAllViews()
            trendingSearches.forEach { trend ->
                val chip = Chip(requireContext()).apply {
                    text = trend
                    isClickable = true
                    isCheckable = false
                    setOnClickListener {
                        searchVideo.setQuery(trend, true)
                    }
                }
                trendingChips?.addView(chip)
            }
        }
    }

    private fun setupSearchView() {
        fragmentSearchBinding!!.searchVideo.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query.isNullOrEmpty()) return true

                    fragmentSearchBinding?.trendingHeader?.visibility = View.GONE
                    fragmentSearchBinding?.trendingChips?.visibility = View.GONE
                    fragmentSearchBinding?.filterScrollView?.visibility = View.VISIBLE

                    databaseViewModel.insertSearchVideos(
                        EntityVideoSearch(Random.nextInt(1, 100000), query)
                    )

                    performSearch(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        fragmentSearchBinding?.filterScrollView?.visibility = View.GONE
                        databaseViewModel.getSearchVideoList()
                        return true
                    }
                    return false
                }
            }
        )

        fragmentSearchBinding!!.searchVideo.setOnCloseListener {
            fragmentSearchBinding?.filterScrollView?.visibility = View.GONE
            databaseViewModel.getSearchVideoList()
            false
        }
    }

    private fun performSearch(query: String) {
        if (!NetworkUtilities.isNetworkAvailable(requireContext())) {
            Utilities.turnOnNetworkDialog(requireContext(), "search videos")
            return
        }

        fragmentSearchBinding?.utilList?.visibility = View.VISIBLE
        fragmentSearchBinding?.clearAll?.visibility = View.GONE

        searchViewModel.searchVideos(
            query = query,
            order = currentSearchOrder,
            type = currentSearchType
        )

        searchViewModel.searchVideos.removeObservers(viewLifecycleOwner)
        searchViewModel.searchVideos.observe(viewLifecycleOwner) { searchVideos ->
            when (searchVideos) {
                is NetworkResult.Loading -> {
                    fragmentSearchBinding?.utilList?.visibility = View.GONE
                }

                is NetworkResult.Success -> {
                    val itemCount = searchVideos.data.items?.size ?: 0
                    AnalyticsHelper.logSearch(query, itemCount)
                    fragmentSearchBinding?.utilList?.apply {
                        layoutManager = LinearLayoutManager(context)
                        visibility = View.VISIBLE
                        adapter = SearchAdapter(context!!, searchVideos.data)
                    }
                }

                is NetworkResult.Error -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .apply {
                            this.setTitle("Oops!")
                            this.setMessage(searchVideos.message)
                            this.setIcon(R.drawable.ic_dialog_error)
                            this.setCancelable(false)
                            this.setNegativeButton("Cancel") { dialog, _ ->
                                dialog?.dismiss()
                            }
                            this.setPositiveButton("Retry") { _, _ ->
                                performSearch(query)
                            }.create().show()
                        }
                }
            }
        }
    }

    private fun setupFilterChips() {
        fragmentSearchBinding!!.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                when (chipId) {
                    R.id.chipRelevance -> {
                        currentSearchOrder = "relevance"
                        currentSearchType = null
                    }
                    R.id.chipUploadDate -> {
                        currentSearchOrder = "date"
                        currentSearchType = null
                    }
                    R.id.chipViewCount -> {
                        currentSearchOrder = "viewCount"
                        currentSearchType = null
                    }
                    R.id.chipRating -> {
                        currentSearchOrder = "rating"
                        currentSearchType = null
                    }
                }
                val currentQuery = fragmentSearchBinding?.searchVideo?.query?.toString()
                if (!currentQuery.isNullOrEmpty()) {
                    performSearch(currentQuery)
                }
            }
        }
    }

    private fun setupVoiceSearch() {
        fragmentSearchBinding!!.voiceSearch.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                startVoiceSearch()
            }
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say something to search...")
        }
        voiceSearchLauncher.launch(intent)
    }

    private fun setupTrendingSearches() {
        fragmentSearchBinding?.trendingHeader?.visibility = View.VISIBLE
        fragmentSearchBinding?.trendingChips?.visibility = View.VISIBLE

        fragmentSearchBinding?.trendingChips?.removeAllViews()
        trendingSearches.forEach { trend ->
            val chip = Chip(requireContext()).apply {
                text = trend
                isClickable = true
                isCheckable = false
                setOnClickListener {
                    fragmentSearchBinding?.searchVideo?.setQuery(trend, true)
                }
            }
            fragmentSearchBinding?.trendingChips?.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentSearchBinding = null
        searchViewModel.searchVideos.removeObservers(viewLifecycleOwner)
    }
}
