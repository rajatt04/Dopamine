package com.google.android.piyush.dopamine.fragments

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.DopamineUserProfile
import com.google.android.piyush.dopamine.adapters.CustomPlayListVAdapter
import com.google.android.piyush.dopamine.adapters.LibraryAdapter
import com.google.android.piyush.dopamine.adapters.YourFavouriteVideosAdapter
import com.google.android.piyush.dopamine.databinding.FragmentLibraryBinding
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.android.piyush.youtube.viewModels.LibraryViewModel
import com.google.android.piyush.youtube.viewModels.LibraryViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions

class Library : Fragment() {

    private var fragmentLibraryBinding: FragmentLibraryBinding? = null
    private lateinit var repository: YoutubeRepositoryImpl
    private lateinit var viewModel : LibraryViewModel
    private lateinit var viewModelProviderFactory: LibraryViewModelFactory
    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var libraryAdapter: LibraryAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLibraryBinding.bind(view)
        fragmentLibraryBinding = binding
        repository = YoutubeRepositoryImpl()
        viewModelProviderFactory = LibraryViewModelFactory(repository)
        databaseViewModel = DatabaseViewModel(requireContext())
        viewModel = ViewModelProvider(
            this,
            viewModelProviderFactory
        )[LibraryViewModel::class.java]

        firebaseAuth = FirebaseAuth.getInstance()

        val sharedPrefs = context?.getSharedPreferences("currentUser", android.content.Context.MODE_PRIVATE)
        val loginType = sharedPrefs?.getString("loginType", "")
        val targetImageView = fragmentLibraryBinding?.userImage

        if (targetImageView != null) {
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
                val userName = firebaseAuth.currentUser?.displayName ?: "Mobile User"
                Toast.makeText(context, userName, Toast.LENGTH_SHORT).show()
                startActivity(Intent(context, DopamineUserProfile::class.java))
            }
        }

        if(NetworkUtilities.isNetworkAvailable(requireContext())) {
            viewModel.codingVideos.observe(viewLifecycleOwner) { playListVideos ->
                when (playListVideos) {
                    is NetworkResult.Success -> {
                        libraryAdapter = LibraryAdapter(requireContext(), playListVideos.data)
                        fragmentLibraryBinding?.apply {
                            codingVideosList.layoutManager =
                                LinearLayoutManager(
                                    context,
                                    codingVideosList.horizontalFadingEdgeLength,
                                    false
                                )
                            codingVideosList.adapter = libraryAdapter
                            binding.codingVideosEffect.stopShimmer()
                            binding.codingVideosEffect.visibility = View.INVISIBLE
                        }
                    }

                    is NetworkResult.Error -> {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            this.setTitle("Oops!")
                            this.setMessage("Oh no! Something went wrong. Please try again.")
                            this.setIcon(R.drawable.get_data_problem)
                            this.setCancelable(true)
                            this.setNegativeButton("Cancel") { dialog, _ ->
                                dialog?.dismiss()
                            }
                            this.setPositiveButton("Retry") { _, _ ->
                                viewModel.reGetCodingVideos.observe(viewLifecycleOwner) {
                                    if (it is NetworkResult.Success) {
                                        binding.codingVideosEffect.visibility = View.INVISIBLE
                                        binding.codingVideosEffect.stopShimmer()
                                        binding.codingVideosList.apply {
                                            setHasFixedSize(true)
                                            layoutManager = LinearLayoutManager(
                                                context,
                                                binding.codingVideosList.horizontalFadingEdgeLength,
                                                false
                                            )
                                            libraryAdapter =
                                                LibraryAdapter(requireContext(), it.data)
                                            adapter = libraryAdapter
                                        }
                                    }
                                }

                                viewModel.reGetSportsVideos.observe(viewLifecycleOwner) {
                                    if (it is NetworkResult.Success) {
                                        binding.sportsVideosEffect.visibility = View.INVISIBLE
                                        binding.sportsVideosEffect.stopShimmer()
                                        binding.sportsVideosList.apply {
                                            setHasFixedSize(true)
                                            layoutManager = LinearLayoutManager(
                                                context,
                                                binding.sportsVideosList.horizontalFadingEdgeLength,
                                                false
                                            )
                                            libraryAdapter =
                                                LibraryAdapter(requireContext(), it.data)
                                            adapter = libraryAdapter
                                        }
                                    }
                                }

                                viewModel.reGetTechnologyVideos.observe(viewLifecycleOwner) {
                                    Log.d(TAG, it.toString())
                                    if (it is NetworkResult.Success) {
                                        binding.techVideosEffect.visibility = View.INVISIBLE
                                        binding.techVideosEffect.stopShimmer()
                                        binding.techVideosList.apply {
                                            setHasFixedSize(true)
                                            layoutManager = LinearLayoutManager(
                                                context,
                                                binding.techVideosList.horizontalFadingEdgeLength,
                                                false
                                            )
                                            libraryAdapter =
                                                LibraryAdapter(requireContext(), it.data)
                                            adapter = libraryAdapter
                                        }
                                    }
                                }

                            }

                        }.create().show()
                    }

                    is NetworkResult.Loading -> {
                        binding.codingVideosEffect.startShimmer()
                        binding.codingVideosEffect.visibility = View.VISIBLE
                    }
                }
            }

            viewModel.sportsVideos.observe(viewLifecycleOwner) { playListVideos ->
                when (playListVideos) {
                    is NetworkResult.Success -> {
                        libraryAdapter = LibraryAdapter(requireContext(), playListVideos.data)
                        fragmentLibraryBinding?.apply {
                            sportsVideosList.layoutManager =
                                LinearLayoutManager(
                                    context,
                                    sportsVideosList.horizontalFadingEdgeLength,
                                    false
                                )
                            sportsVideosList.adapter = libraryAdapter
                            binding.sportsVideosEffect.stopShimmer()
                            binding.sportsVideosEffect.visibility = View.INVISIBLE
                        }
                    }

                    is NetworkResult.Error -> {
                        binding.sportsVideosEffect.startShimmer()
                        binding.sportsVideosEffect.visibility = View.VISIBLE
                    }

                    is NetworkResult.Loading -> {
                        binding.sportsVideosEffect.startShimmer()
                        binding.sportsVideosEffect.visibility = View.VISIBLE
                    }
                }
            }

            viewModel.technologyVideos.observe(viewLifecycleOwner) { playListVideos ->
                when (playListVideos) {
                    is NetworkResult.Success -> {
                        libraryAdapter = LibraryAdapter(requireContext(), playListVideos.data)
                        fragmentLibraryBinding?.apply {
                            techVideosList.layoutManager =
                                LinearLayoutManager(
                                    context,
                                    techVideosList.horizontalFadingEdgeLength,
                                    false
                                )
                            techVideosList.adapter = libraryAdapter
                            binding.techVideosEffect.stopShimmer()
                            binding.techVideosEffect.visibility = View.INVISIBLE
                        }
                    }

                    is NetworkResult.Error -> {
                        binding.techVideosEffect.startShimmer()
                        binding.techVideosEffect.visibility = View.VISIBLE
                    }

                    is NetworkResult.Loading -> {
                        binding.techVideosEffect.startShimmer()
                        binding.techVideosEffect.visibility = View.VISIBLE
                    }
                }
            }
        }

        databaseViewModel.getFavouritePlayList()

        databaseViewModel.favouritePlayList.observe(viewLifecycleOwner){ yourFavouriteList ->
            if(yourFavouriteList.isEmpty()){
                fragmentLibraryBinding!!.yourFavouriteList.visibility = View.GONE
                fragmentLibraryBinding!!.yourFavouriteEffect.visibility = View.VISIBLE
                fragmentLibraryBinding!!.yourFavouriteEffect.startShimmer()
            }else{
                fragmentLibraryBinding!!.yourFavouriteList.visibility = View.VISIBLE
                fragmentLibraryBinding!!.yourFavouriteEffect.stopShimmer()
                fragmentLibraryBinding!!.yourFavouriteEffect.visibility = View.INVISIBLE
                fragmentLibraryBinding!!.yourFavouriteList.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = YourFavouriteVideosAdapter(requireContext(), yourFavouriteList)
                }
            }
        }

        val iFramePlayerOptions = IFramePlayerOptions.Builder(requireContext())
            .controls(0)
            .build()

        fragmentLibraryBinding!!.youtubePlayerView.enableAutomaticInitialization = false
        if(NetworkUtilities.isNetworkAvailable(requireContext())) {
            fragmentLibraryBinding!!.youtubePlayerView.initialize(
                object :
                    AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        super.onReady(youTubePlayer)
                        youTubePlayer.loadVideo(
                            "l2UDgpLz20M", 0.0F
                        )
                        youTubePlayer.mute()
                    }
                },
                true,
                iFramePlayerOptions
            )
        }

        if(databaseViewModel.countTheNumberOfCustomPlaylist() < 1){
            fragmentLibraryBinding!!.customPlaylistsList.visibility = View.GONE
        }else{
            fragmentLibraryBinding!!.customPlaylistsList.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = CustomPlayListVAdapter(requireContext(),databaseViewModel.getPlaylist())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentLibraryBinding = null
        viewModel.codingVideos.removeObservers(viewLifecycleOwner)
        viewModel.sportsVideos.removeObservers(viewLifecycleOwner)
        viewModel.technologyVideos.removeObservers(viewLifecycleOwner)
    }
}