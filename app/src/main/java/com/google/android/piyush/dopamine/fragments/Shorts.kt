package com.google.android.piyush.dopamine.fragments

import ShortsAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.FragmentShortsBinding
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import com.google.android.piyush.youtube.viewModels.ShortsViewModel
import com.google.android.piyush.youtube.viewModels.ShortsViewModelFactory
import com.google.android.piyush.database.viewModel.DatabaseViewModel

class Shorts : Fragment() {

    private var shortsFragmentBinding: FragmentShortsBinding? = null
    private lateinit var youtubeRepositoryImpl: YoutubeRepositoryImpl
    private lateinit var shortsViewModel: ShortsViewModel
    private lateinit var shortsViewModelFactory: ShortsViewModelFactory

    private lateinit var databaseViewModel: DatabaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_shorts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentShortsBinding.bind(view)
        shortsFragmentBinding = binding
        youtubeRepositoryImpl = YoutubeRepositoryImpl()
        shortsViewModelFactory = ShortsViewModelFactory(youtubeRepositoryImpl)
        shortsViewModel = ViewModelProvider(this, shortsViewModelFactory)[ShortsViewModel::class.java]
        
        // Initialize DatabaseViewModel
        databaseViewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        if(NetworkUtilities.isNetworkAvailable(requireContext())){
            shortsViewModel.shorts.observe(viewLifecycleOwner){ shorts ->
                when(shorts){
                    is YoutubeResource.Loading -> {}
                    is YoutubeResource.Success -> {
                        if(shorts.data.isEmpty()){
                            binding.playWithShortsEffect.apply {
                                startShimmer()
                                visibility = View.VISIBLE
                            }
                        }else{
                            binding.playWithShortsEffect.apply {
                                stopShimmer()
                                visibility = View.GONE
                            }
                            
                            val adapter = ShortsAdapter(
                                requireContext(),
                                shorts.data,
                                databaseViewModel
                            )
                            binding.playWithShorts.adapter = adapter
                            
                            // Observe Liked Videos
                            databaseViewModel.getFavouritePlayList()
                            databaseViewModel.favouritePlayList.observe(viewLifecycleOwner) { favourites ->
                                val ids = favourites.map { it.videoId }.toSet()
                                adapter.updateLikedVideos(ids)
                            }
                            
                            // Observe Subscriptions
                            databaseViewModel.getAllSubscriptions()
                            databaseViewModel.subscriptions.observe(viewLifecycleOwner) { subs ->
                                val ids = subs.map { it.channelId }.toSet()
                                adapter.updateSubscribedChannels(ids)
                            }
                        }
                    }
                    is YoutubeResource.Error -> {
                        binding.playWithShortsEffect.apply {
                            startShimmer()
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
}