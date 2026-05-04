package com.google.android.piyush.dopamine.fragments

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.DopamineUserProfile
import com.google.android.piyush.dopamine.activities.DopamineVideoWatchHistory
import com.google.android.piyush.dopamine.adapters.HomeAdapter
import com.google.android.piyush.dopamine.databinding.FragmentHomeBinding
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.youtube.utilities.YoutubeResource
import com.google.android.piyush.youtube.viewModels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlin.system.exitProcess

@AndroidEntryPoint
class Home : Fragment() {

    private var fragmentHomeBinding : FragmentHomeBinding? = null
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeAdapter

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
        binding.greeting.text = getGreeting()
        Log.d(TAG, " -> Fragment : Home || Greeting : ${getGreeting()}")

        Glide.with(this).load(R.drawable.default_user).into(binding.userImage)

        binding.watchHistory.setOnClickListener {
            startActivity(
                Intent(
                    context,
                    DopamineVideoWatchHistory::class.java
                )
            )
        }

        binding.userImage.setOnClickListener {
            startActivity(
                Intent(
                    context,
                    DopamineUserProfile::class.java
                )
            )
        }

        val chips = listOf("All", "Music", "Gaming", "News", "Entertainment", "Comedy")
        binding.chipsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = com.google.android.piyush.dopamine.adapters.ChipAdapter(chips) { selectedChip ->
                val categoryId = when(selectedChip) {
                    "Music" -> "10"
                    "Gaming" -> "20"
                    "News" -> "25"
                    "Entertainment" -> "24"
                    "Comedy" -> "34"
                    else -> null // "All" or unknown
                }
                homeViewModel.fetchHomeVideos(categoryId)
            }
        }

        if(NetworkUtilities.isNetworkAvailable(requireContext())) {
            homeViewModel.videos.observe(viewLifecycleOwner) {videos ->
                when (videos) {
                    is YoutubeResource.Loading -> {
                        binding.shimmerRecyclerView.visibility = View.VISIBLE
                        binding.shimmerRecyclerView.startShimmer()
                        //Log.d(TAG, "Loading: True")
                    }
                    is YoutubeResource.Success -> {
                        binding.shimmerRecyclerView.visibility = View.INVISIBLE
                        binding.shimmerRecyclerView.stopShimmer()
                        binding.recyclerView.apply {
                            setHasFixedSize(true)
                            layoutManager = LinearLayoutManager(context)
                            homeAdapter = HomeAdapter(requireContext(), videos.data)
                            adapter = homeAdapter
                        }
                    }
                    is YoutubeResource.Error -> {
                        Log.d(TAG, "Error: ${videos.exception.message.toString()}")
                        MaterialAlertDialogBuilder(requireContext())
                            .apply {
                                this.setTitle(getString(R.string.error_title_something_went_wrong))
                                this.setMessage(videos.exception.message.toString())
                                this.setIcon(R.drawable.ic_dialog_error)
                                this.setCancelable(false)
                                this.setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog?.dismiss()
                                }
                                this.setPositiveButton(getString(R.string.action_retry)) { _, _ ->
                                    homeViewModel.reGetHomeVideos()
                                    homeViewModel.reGetVideos.observe(viewLifecycleOwner){ videos ->
                                        when (videos) {
                                            is YoutubeResource.Loading -> {
                                                binding.shimmerRecyclerView.visibility = View.VISIBLE
                                                binding.shimmerRecyclerView.startShimmer()
                                                Log.d(TAG, "Loading: True")
                                            }
                                            is YoutubeResource.Success -> {
                                                binding.shimmerRecyclerView.visibility = View.INVISIBLE
                                                binding.shimmerRecyclerView.stopShimmer()
                                                binding.recyclerView.apply {
                                                    setHasFixedSize(true)
                                                    layoutManager = LinearLayoutManager(context)
                                                    homeAdapter = HomeAdapter(requireContext(), videos.data)
                                                    adapter = homeAdapter
                                                }
                                                //Log.d(TAG, "Success: ${videos.data}")
                                            }
                                            is YoutubeResource.Error -> {
                                                Log.d(TAG, "Error: ${videos.exception.message.toString()}")
                                                MaterialAlertDialogBuilder(requireContext())
                                                    .apply {
                                                        this.setTitle(getString(R.string.error_title_something_went_wrong))
                                                        this.setMessage(videos.exception.message.toString())
                                                        this.setIcon(R.drawable.ic_dialog_error)
                                                        this.setCancelable(false)
                                                        this.setPositiveButton(getString(R.string.action_try_again_later)) { dialog, _ ->
                                                            dialog?.dismiss()
                                                            exitProcess(0)
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