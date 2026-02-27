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
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.activityViewModels

@AndroidEntryPoint
class AddToPlaylistSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAddToPlaylistSheetBinding? = null
    private val binding get() = _binding!!
    private val databaseViewModel: DatabaseViewModel by activityViewModels()

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
        

        setupPlaylistsRecyclerView()
    }

    private fun setupPlaylistsRecyclerView() {
        viewLifecycleOwner.lifecycleScope.launch {
            val playlists = databaseViewModel.getPlaylist()
            
            if (playlists.isNullOrEmpty()) {
                binding.playlistsRecyclerView.visibility = View.GONE
                binding.emptyStateText.visibility = View.VISIBLE
            } else {
                binding.playlistsRecyclerView.visibility = View.VISIBLE
                binding.emptyStateText.visibility = View.GONE
                
                binding.playlistsRecyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = CustomPlaylistsAdapter(requireContext(), playlists, databaseViewModel)
                    setHasFixedSize(true)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
