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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CustomPlaylistsAdapter(
    private val context: Context,
    private var playlists: List<CustomPlaylistView>?,
    private val databaseViewModel: DatabaseViewModel
) : RecyclerView.Adapter<CustomPlaylistsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomPlaylistsViewHolder {
        return CustomPlaylistsViewHolder(
            LayoutInflater.from(
                parent.context
            )
                .inflate(
                    R.layout.item_custom_playlists_view, parent, false
                )
        )
    }

    override fun getItemCount(): Int {
        return playlists?.size!!
    }

    override fun onBindViewHolder(holder: CustomPlaylistsViewHolder, position: Int) {
        val pref = context.getSharedPreferences("customPlaylist", Context.MODE_PRIVATE)
        val playlistName = playlists?.get(position)?.playListName ?: return
        val videoId = pref.getString("videoId", "")!!
        val title = pref.getString("title", "")!!
        val thumbnail = pref.getString("thumbnail", "")!!
        val channelId = pref.getString("channelId", "")!!
        val viewCount = pref.getString("viewCount", "")!!
        val channelTitle = pref.getString("channelTitle", "")!!
        val publishedAt = pref.getString("publishedAt", "")!!
        val duration = pref.getString("duration", "")!!

        holder.title.text = playlists?.get(position)?.playListName
        holder.description.text = playlists?.get(position)?.playListDescription

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            val isVideoAlreadyAdded = databaseViewModel.isExistsDataInPlaylist(playlistName,videoId)
            
            holder.selectedPlaylistItem.isChecked = isVideoAlreadyAdded

            holder.selectedPlaylistItem.addOnCheckedStateChangedListener { _, isChecked ->
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    if(isChecked == 1){
                        if(!isVideoAlreadyAdded){
                            databaseViewModel.addItemsInCustomPlaylist(
                                playlistName,
                                playlistsData = CustomPlaylists(
                                    videoId = videoId,
                                    title = title,
                                    thumbnail = thumbnail,
                                    channelId = channelId,
                                    viewCount = viewCount,
                                    channelTitle = channelTitle ,
                                    publishedAt = publishedAt,
                                    duration = duration
                                )
                            )
                        }
                        ToastUtilities.showToast(context, "Successfully added to playlist :)")
                    }else{
                        if(isVideoAlreadyAdded){
                            databaseViewModel.deleteVideoFromPlaylist(
                                playlistName,
                                videoId
                            )
                        }
                    }
                }
            }
        }
    }
}
