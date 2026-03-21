package com.google.android.piyush.dopamine.adapters

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.database.model.CustomPlaylistView
import com.google.android.piyush.database.model.CustomPlaylists
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.ToastUtilities
import com.google.android.piyush.dopamine.viewHolders.CustomPlaylistsViewHolder

data class VideoData(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelId: String,
    val viewCount: String,
    val channelTitle: String,
    val publishedAt: String,
    val duration: String
)

class CustomPlaylistsAdapter(
    private val context: Context,
    private var playlists: List<CustomPlaylistView>?,
    private val videoData: VideoData?
) : RecyclerView.Adapter<CustomPlaylistsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomPlaylistsViewHolder {
        return CustomPlaylistsViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_custom_playlists_view, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return playlists?.size ?: 0
    }

    override fun onBindViewHolder(holder: CustomPlaylistsViewHolder, position: Int) {
        val databaseViewModel = DatabaseViewModel(context = context)
        val playlist = playlists?.getOrNull(position) ?: return
        val playlistName = playlist.playListName

        val currentVideoData = videoData
        val videoId = currentVideoData?.videoId ?: ""
        val title = currentVideoData?.title ?: ""
        val thumbnail = currentVideoData?.thumbnail ?: ""
        val channelId = currentVideoData?.channelId ?: ""
        val viewCount = currentVideoData?.viewCount ?: ""
        val channelTitle = currentVideoData?.channelTitle ?: ""
        val publishedAt = currentVideoData?.publishedAt ?: ""
        val duration = currentVideoData?.duration ?: ""

        val isVideoAlreadyAdded = if (videoId.isNotEmpty()) {
            databaseViewModel.isExistsDataInPlaylist(playlistName, videoId)
        } else {
            false
        }

        Log.d(TAG, "videoId: $isVideoAlreadyAdded || playlistName: $playlistName")
        Log.d(TAG, "currentPlaylists: ${databaseViewModel.getPlaylistsFromDatabase()}")

        holder.selectedPlaylistItem.isChecked = isVideoAlreadyAdded == true

        holder.title.text = playlist.playListName
        holder.description.text = playlist.playListDescription

        holder.selectedPlaylistItem.addOnCheckedStateChangedListener { _, isChecked ->
            if (videoId.isEmpty()) {
                ToastUtilities.showToast(context, "No video selected")
                return@addOnCheckedStateChangedListener
            }

            if (isChecked == 1) {
                if (!isVideoAlreadyAdded) {
                    databaseViewModel.addItemsInCustomPlaylist(
                        playlistName,
                        playlistsData = CustomPlaylists(
                            videoId = videoId,
                            title = title,
                            thumbnail = thumbnail,
                            channelId = channelId,
                            viewCount = viewCount,
                            channelTitle = channelTitle,
                            publishedAt = publishedAt,
                            duration = duration
                        )
                    )
                    Log.d(TAG, "videoId: $videoId || playlistName: $playlistName")
                }
                ToastUtilities.showToast(context, "Added to $playlistName")
            } else {
                if (isVideoAlreadyAdded) {
                    Log.d(TAG, "Removing videoId: $videoId || playlistName: $playlistName")
                    databaseViewModel.deleteVideoFromPlaylist(playlistName, videoId)
                    ToastUtilities.showToast(context, "Removed from $playlistName")
                }
            }
        }
    }
}
