package com.google.android.piyush.dopamine.activities


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.RecentVideosAdapter
import com.google.android.piyush.dopamine.databinding.ActivityDopamineVideoWatchHistoryBinding

import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

@AndroidEntryPoint
class DopamineVideoWatchHistory : AppCompatActivity() {

    private lateinit var binding: ActivityDopamineVideoWatchHistoryBinding
    private val databaseViewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDopamineVideoWatchHistoryBinding.inflate(layoutInflater)

        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseViewModel.getRecentVideos()

        databaseViewModel.recentVideos.observe(this) { recentVideos ->
            binding.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                adapter = RecentVideosAdapter(context,recentVideos)
            }
            if(recentVideos.isNullOrEmpty()){
                binding.recyclerView.visibility = View.GONE
                binding.clearWatchHistory.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
            }
            Log.d(ContentValues.TAG, " -> Activity : DopamineVideoWatchHistory || recentVideos : $recentVideos")
        }

        binding.topAppBar.setNavigationOnClickListener{
            finish()
        }

        binding.clearWatchHistory.setOnClickListener {
            databaseViewModel.deleteRecentVideo()
            Snackbar.make(binding.root, "Watch History Cleared", Snackbar.LENGTH_SHORT).show()
            binding.recyclerView.visibility = View.GONE
            binding.clearWatchHistory.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
        }
    }
}
