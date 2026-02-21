package com.google.android.piyush.dopamine.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.adapters.YourFavouriteVideosAdapter
import com.google.android.piyush.dopamine.databinding.FragmentLikedVideosBinding

class LikedVideosFragment : Fragment() {

    private var _binding: FragmentLikedVideosBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseViewModel: DatabaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikedVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseViewModel = DatabaseViewModel(requireActivity().application)
        databaseViewModel.getFavouritePlayList()

        databaseViewModel.favouritePlayList.observe(viewLifecycleOwner) { likedList ->
            if (likedList.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = GridLayoutManager(context, 2)
                    adapter = YourFavouriteVideosAdapter(requireContext(), likedList) { video ->
                        val sharedViewModel = ViewModelProvider(requireActivity())[com.google.android.piyush.dopamine.viewModels.SharedViewModel::class.java]
                        sharedViewModel.selectVideo(video)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
