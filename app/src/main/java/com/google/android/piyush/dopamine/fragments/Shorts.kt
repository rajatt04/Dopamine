package com.google.android.piyush.dopamine.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.ShortsAdapter
import com.google.android.piyush.dopamine.databinding.FragmentShortsBinding
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.android.piyush.youtube.viewModels.ShortsViewModel
import com.google.android.piyush.youtube.viewModels.ShortsViewModelFactory

class Shorts : Fragment() {

    private var shortsFragmentBinding: FragmentShortsBinding? = null
    private lateinit var youtubeRepositoryImpl: YoutubeRepositoryImpl
    private lateinit var shortsViewModel: ShortsViewModel
    private lateinit var shortsViewModelFactory: ShortsViewModelFactory
    private var shortsAdapter: ShortsAdapter? = null

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

        if (NetworkUtilities.isNetworkAvailable(requireContext())) {
            shortsViewModel.shorts.observe(viewLifecycleOwner) { shorts ->
                when (shorts) {
                    is NetworkResult.Loading -> {
                        binding.playWithShortsEffect.apply {
                            startShimmer()
                            visibility = View.VISIBLE
                        }
                    }

                    is NetworkResult.Success -> {
                        if (shorts.data.isEmpty()) {
                            binding.playWithShortsEffect.apply {
                                startShimmer()
                                visibility = View.VISIBLE
                            }
                        } else {
                            binding.playWithShortsEffect.apply {
                                stopShimmer()
                                visibility = View.GONE
                            }

                            shortsAdapter = ShortsAdapter(
                                requireContext(),
                                viewLifecycleOwner.lifecycle,
                                shorts.data
                            )

                            binding.playWithShorts.apply {
                                adapter = shortsAdapter
                                offscreenPageLimit = 1 // Keep 1 neighbour ready
                            }

                            // Wire page-change → adapter so only current page plays
                            binding.playWithShorts.registerOnPageChangeCallback(
                                object : ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        super.onPageSelected(position)
                                        val rv = binding.playWithShorts.getChildAt(0)
                                            as? androidx.recyclerview.widget.RecyclerView
                                        val holder = rv
                                            ?.findViewHolderForAdapterPosition(position)
                                            as? com.google.android.piyush.dopamine.viewHolders.ShortsViewHolder
                                        shortsAdapter?.onPageSelected(position, holder)
                                    }
                                }
                            )

                            // Auto-play first item once adapter is attached
                            binding.playWithShorts.post {
                                val rv = binding.playWithShorts.getChildAt(0)
                                    as? androidx.recyclerview.widget.RecyclerView
                                val holder = rv
                                    ?.findViewHolderForAdapterPosition(0)
                                    as? com.google.android.piyush.dopamine.viewHolders.ShortsViewHolder
                                shortsAdapter?.onPageSelected(0, holder)
                            }
                        }
                    }

                    is NetworkResult.Error -> {
                        binding.playWithShortsEffect.apply {
                            startShimmer()
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shortsAdapter?.clearData()
        shortsFragmentBinding = null
    }
}