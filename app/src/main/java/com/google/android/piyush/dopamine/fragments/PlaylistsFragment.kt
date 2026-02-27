package com.google.android.piyush.dopamine.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.adapters.CustomPlayListVAdapter
import com.google.android.piyush.dopamine.databinding.FragmentPlaylistsBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.activityViewModels

@AndroidEntryPoint
class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val databaseViewModel: DatabaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPlaylists()
        }
        
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewLifecycleOwner.lifecycleScope.launch {
            val playlists = databaseViewModel.getPlaylist()
            if (playlists.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    adapter = CustomPlayListVAdapter(requireContext(), playlists)
                }
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
