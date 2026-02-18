package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.google.android.piyush.dopamine.viewHolders.YourFavouriteVideosViewHolder

class YourFavouriteVideosAdapter(
    private val context: Context,
    private val videos: List<EntityFavouritePlaylist>?,
    private val onVideoClick: (com.google.android.piyush.dopamine.viewModels.SelectedVideo) -> Unit
) : RecyclerView.Adapter<YourFavouriteVideosViewHolder>() {
    
    // ... (onCreateViewHolder and getItemCount unchanged)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): YourFavouriteVideosViewHolder {
        return YourFavouriteVideosViewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(R.layout.item_fragment_library_your_favourite_videos, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return videos?.size!!
    }

    override fun onBindViewHolder(holder: YourFavouriteVideosViewHolder, position: Int) {
        val favouriteVideo = videos?.get(position)
        holder.title.text = favouriteVideo?.title
        holder.customName.text = favouriteVideo?.channelTitle
        Glide.with(context).load(favouriteVideo?.thumbnail).into(holder.image)
        holder.videoCard.setOnClickListener {
            onVideoClick(
                com.google.android.piyush.dopamine.viewModels.SelectedVideo(
                    videoId = favouriteVideo?.videoId!!,
                    channelId = favouriteVideo.channelId!!,
                    title = favouriteVideo.title!!,
                    description = "", // Description might not be available in EntityFavouritePlaylist, defaulting to empty
                    thumbnailUrl = favouriteVideo.thumbnail,
                    channelTitle = favouriteVideo.channelTitle!!
                )
            )
        }
    }
}