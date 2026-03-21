package com.google.android.piyush.dopamine.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    @Suppress("DEPRECATION")
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

        if (loginType == "mobile") {
            val photoUrl = sharedPrefs?.getString("photoUrl", "")
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(this).load(photoUrl).circleCrop().into(fragmentSearchBinding!!.userImage)
            } else {
                Glide.with(this).load(R.drawable.default_user).into(fragmentSearchBinding!!.userImage)
            }
        } else if (firebaseAuth.currentUser?.email.toString().isEmpty()) {
            Glide.with(this).load(R.drawable.default_user).into(fragmentSearchBinding!!.userImage)
        } else {
            Glide.with(this).load(firebaseAuth.currentUser?.photoUrl).circleCrop().into(fragmentSearchBinding!!.userImage)
        }

        fragmentSearchBinding!!.userImage.setOnClickListener {
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
                        // Delete individual item (would need DAO method)
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

                    // Hide trending and show results
                    fragmentSearchBinding?.trendingHeader?.visibility = View.GONE
                    fragmentSearchBinding?.trendingChips?.visibility = View.GONE
                    fragmentSearchBinding?.filterScrollView?.visibility = View.VISIBLE

                    // Save to history
                    databaseViewModel.insertSearchVideos(
                        EntityVideoSearch(Random.nextInt(1, 100000), query)
                    )

                    performSearch(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // Show search history/suggestions as user types
                    if (newText.isNullOrEmpty()) {
                        fragmentSearchBinding?.filterScrollView?.visibility = View.GONE
                        // Show history again
                        databaseViewModel.getSearchVideoList()
                        return true
                    }
                    return false
                }
            }
        )

        // Clear button
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
                // Re-search with new filter
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
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    Utilities.PERMISSION_REQUEST_CODE
                )
            } else {
                startVoiceSearch()
            }
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something to search...")
        }
        startActivityForResult(intent, Utilities.PERMISSION_REQUEST_CODE)
    }

    private fun setupTrendingSearches() {
        // Show trending searches by default when no history
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

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Utilities.PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceSearch()
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityResult(requestCode, resultCode, data)",
        "androidx.fragment.app.Fragment"
    ))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Utilities.PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                fragmentSearchBinding?.searchVideo?.setQuery(result[0], true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentSearchBinding = null
        searchViewModel.searchVideos.removeObservers(viewLifecycleOwner)
    }
}
