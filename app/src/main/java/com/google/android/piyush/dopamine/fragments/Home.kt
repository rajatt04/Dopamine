package com.google.android.piyush.dopamine.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.DopamineUserProfile
import com.google.android.piyush.dopamine.activities.DopamineVideoWatchHistory
import com.google.android.piyush.dopamine.adapters.HomeAdapter
import com.google.android.piyush.dopamine.adapters.SearchAdapter
import com.google.android.piyush.dopamine.adapters.SearchHistoryAdapter
import com.google.android.piyush.dopamine.databinding.FragmentHomeBinding
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.utilities.ToastUtilities
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.SearchViewModel
import com.google.android.piyush.dopamine.viewModels.SearchViewModelFactory
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import com.google.android.piyush.youtube.viewModels.HomeViewModel
import com.google.android.piyush.youtube.viewModels.HomeViewModelFactory
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class Home : Fragment() {

    private var fragmentHomeBinding : FragmentHomeBinding? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var repository: YoutubeRepositoryImpl
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var searchViewModelFactory: SearchViewModelFactory
    private lateinit var homeAdapter: HomeAdapter

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.firstOrNull()?.let { voiceResult ->
                fragmentHomeBinding?.searchVideo?.setQuery(voiceResult, true)
            }
        }
    }

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
        
        searchViewModelFactory = SearchViewModelFactory(repository)
        searchViewModel = ViewModelProvider(this, searchViewModelFactory)[SearchViewModel::class.java]
        databaseViewModel = DatabaseViewModel(requireActivity().application)

        setupSearchLogic(binding)

        // Removed greeting text, using Dopamine logo text instead

        // Default user avatar (no auth)
        fragmentHomeBinding!!.userImage.setImageResource(R.drawable.default_user)

        fragmentHomeBinding!!.watchHistory.setOnClickListener {
            startActivity(
                Intent(
                    context,
                    DopamineVideoWatchHistory::class.java
                )
            )
        }

        fragmentHomeBinding!!.userImage.setOnClickListener {
            startActivity(
                Intent(
                    context,
                    DopamineUserProfile::class.java
                )
            )
        }

        if(NetworkUtilities.isNetworkAvailable(requireContext())) {
            homeViewModel.videos.observe(viewLifecycleOwner) {videos ->
                when (videos) {
                    is YoutubeResource.Loading -> {
                        binding.shimmerRecyclerView.visibility = View.VISIBLE
                        binding.shimmerRecyclerView.startShimmer()
                    }
                    is YoutubeResource.Success -> {
                        binding.shimmerRecyclerView.visibility = View.INVISIBLE
                        binding.shimmerRecyclerView.stopShimmer()
                        binding.recyclerView.apply {
                            setHasFixedSize(true)
                            layoutManager = LinearLayoutManager(context)
                            homeAdapter = HomeAdapter(requireContext(), videos.data) { video ->
                                val sharedViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.google.android.piyush.dopamine.viewModels.SharedViewModel::class.java]
                                sharedViewModel.selectVideo(video)
                            }
                            adapter = homeAdapter
                        }
                    }
                    is YoutubeResource.Error -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .apply {
                                this.setTitle("Something went wrong")
                                this.setMessage(videos.exception.message.toString())
                                this.setIcon(R.drawable.ic_dialog_error)
                                this.setCancelable(false)
                                this.setNegativeButton("Cancel") { dialog, _ ->
                                    dialog?.dismiss()
                                }
                                this.setPositiveButton("Retry") { _, _ ->
                                    homeViewModel.reGetHomeVideos()
                                    homeViewModel.reGetVideos.observe(viewLifecycleOwner){ videos ->
                                        when (videos) {
                                            is YoutubeResource.Loading -> {
                                                binding.shimmerRecyclerView.visibility = View.VISIBLE
                                                binding.shimmerRecyclerView.startShimmer()
                                            }
                                            is YoutubeResource.Success -> {
                                                binding.shimmerRecyclerView.visibility = View.INVISIBLE
                                                binding.shimmerRecyclerView.stopShimmer()
                                            binding.recyclerView.apply {
                                                    setHasFixedSize(true)
                                                    layoutManager = LinearLayoutManager(context)
                                                    homeAdapter = HomeAdapter(requireContext(), videos.data) { video ->
                                                        val sharedViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.google.android.piyush.dopamine.viewModels.SharedViewModel::class.java]
                                                        sharedViewModel.selectVideo(video)
                                                    }
                                                    adapter = homeAdapter
                                                }
                                            }
                                            is YoutubeResource.Error -> {
                                                MaterialAlertDialogBuilder(requireContext())
                                                    .apply {
                                                        this.setTitle("Something went wrong")
                                                        this.setMessage(videos.exception.message.toString())
                                                        this.setIcon(R.drawable.ic_dialog_error)
                                                        this.setCancelable(false)
                                                        this.setPositiveButton("Try again later") { dialog, _ ->
                                                            dialog?.dismiss()
                                                        }.create().show()
                                                    }
                                            }
                                        }
                                    }
                                }.create().show()
                            }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentHomeBinding = null
        homeViewModel.videos.removeObservers(viewLifecycleOwner)
    }

    private fun setupSearchLogic(binding: FragmentHomeBinding) {
        binding.searchIcon.setOnClickListener {
            binding.searchOverlay.visibility = View.VISIBLE
            databaseViewModel.getSearchVideoList()
        }

        binding.backFromSearch.setOnClickListener {
            binding.searchOverlay.visibility = View.GONE
        }

        // Search History Observer
        databaseViewModel.searchVideoHistory.observe(viewLifecycleOwner) { history ->
            if (binding.searchVideo.query.isEmpty()) {
                binding.searchList.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = SearchHistoryAdapter(history)
                }
            }
        }

        binding.searchVideo.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    databaseViewModel.insertSearchVideos(
                        EntityVideoSearch(0, query)
                    )
                    performSearch(query, binding)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    databaseViewModel.getSearchVideoList()
                }
                return false
            }
        })

        binding.voiceSearch.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), Utilities.PERMISSION_REQUEST_CODE)
            } else {
                startVoiceSearch()
            }
        }
    }

    private fun performSearch(query: String, binding: FragmentHomeBinding) {
        if (NetworkUtilities.isNetworkAvailable(requireContext())) {
            searchViewModel.searchVideos(query)
        } else {
            NetworkUtilities.showNetworkError(requireContext())
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...")
        voiceSearchLauncher.launch(intent)
    }
}