package com.google.android.piyush.dopamine.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.piyush.database.entities.SubscriptionEntity
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.adapters.SubscriptionsAdapter
import com.google.android.piyush.dopamine.databinding.FragmentSubscriptionsBinding

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.activityViewModels

@AndroidEntryPoint
class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!
    private val databaseViewModel: DatabaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        
        setupRecyclerView()
        setupRefreshLayout()
        
        databaseViewModel.subscriptions.observe(viewLifecycleOwner) { subscriptions ->
            updateUI(subscriptions)
            binding.swipeRefreshLayout.isRefreshing = false
        }

        loadSubscriptions()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            setHasFixedSize(true)
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadSubscriptions()
        }
    }

    private fun loadSubscriptions() {
        binding.swipeRefreshLayout.isRefreshing = true
        databaseViewModel.getAllSubscriptions()
    }

    private fun updateUI(subscriptions: List<SubscriptionEntity>?) {
        if (subscriptions.isNullOrEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.adapter = SubscriptionsAdapter(requireContext(), subscriptions) { subscription ->
                // Handle subscription click if needed
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
