package com.google.android.piyush.dopamine.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.CustomPlaylistsAdapter
import com.google.android.piyush.dopamine.databinding.FragmentAddToPlaylistSheetBinding

class AddToPlaylistSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAddToPlaylistSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseViewModel: DatabaseViewModel

    companion object {
        const val TAG = "AddToPlaylistSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddToPlaylistSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        databaseViewModel = DatabaseViewModel(requireContext())
        setupPlaylistsRecyclerView()
    }

    private fun setupPlaylistsRecyclerView() {
        val playlists = databaseViewModel.getPlaylist()
        
        if (playlists.isNullOrEmpty()) {
            binding.playlistsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.playlistsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            
            binding.playlistsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = CustomPlaylistsAdapter(requireContext(), playlists)
                setHasFixedSize(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
