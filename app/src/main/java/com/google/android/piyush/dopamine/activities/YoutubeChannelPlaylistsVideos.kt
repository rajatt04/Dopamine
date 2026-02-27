package com.google.android.piyush.dopamine.activities

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.YoutubePlaylistsVideosAdapter
import com.google.android.piyush.dopamine.databinding.ActivityYoutubeChannelPlaylistsVideosBinding
import com.google.android.piyush.dopamine.player.ExoYouTubePlayer
import com.google.android.piyush.dopamine.viewModels.YoutubeChannelPlaylistsVideosViewModel

import com.google.android.piyush.youtube.utilities.YoutubeResource

import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

@AndroidEntryPoint
class YoutubeChannelPlaylistsVideos : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubeChannelPlaylistsVideosBinding
    private val youtubeChannelPlaylistsVideosViewModel: YoutubeChannelPlaylistsVideosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityYoutubeChannelPlaylistsVideosBinding.inflate(layoutInflater)

        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val playlistId = intent.getStringExtra("playlistId").toString()

        binding.playlistsPlayer.setCallback(object : ExoYouTubePlayer.PlayerCallback {
            override fun onReady() {
                Log.d(TAG, "Playlist player ready")
            }
        })

        youtubeChannelPlaylistsVideosViewModel.getPlaylistsVideos(playlistId)

        youtubeChannelPlaylistsVideosViewModel.playlistsVideos.observe(this) { playlistsVideos ->
            when (playlistsVideos) {
                is YoutubeResource.Loading -> {}
                is YoutubeResource.Success -> {
                    binding.recyclerView.apply {
                        setHasFixedSize(true)
                        layoutManager = LinearLayoutManager(this@YoutubeChannelPlaylistsVideos)
                        adapter = YoutubePlaylistsVideosAdapter(context, playlistsVideos.data)
                    }

                    val firstVideoId = playlistsVideos.data.items?.firstOrNull()?.contentDetails?.videoId
                    if (firstVideoId != null) {
                        binding.playlistsPlayer.loadVideo(firstVideoId)
                    }
                }
                is YoutubeResource.Error -> {
                    Log.d(TAG, "Error: ${playlistsVideos.exception.message.toString()}")
                    binding.channelPlaylistVideosLoader.apply {
                        visibility = View.VISIBLE
                        show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.playlistsPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.playlistsPlayer.onPause()
    }

    override fun onDestroy() {
        binding.playlistsPlayer.release()
        super.onDestroy()
    }
}