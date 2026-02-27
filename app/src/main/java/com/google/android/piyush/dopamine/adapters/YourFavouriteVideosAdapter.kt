package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.viewHolders.YourFavouriteVideosViewHolder

class YourFavouriteVideosAdapter(
    private val context: Context,
    videos: List<EntityFavouritePlaylist>?,
    private val onVideoClick: (com.google.android.piyush.dopamine.viewModels.SelectedVideo) -> Unit
) : ListAdapter<EntityFavouritePlaylist, YourFavouriteVideosViewHolder>(FavouriteDiffCallback()) {

    init {
        submitList(videos ?: emptyList())
    }

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

    override fun onBindViewHolder(holder: YourFavouriteVideosViewHolder, position: Int) {
        val favouriteVideo = getItem(position)
        holder.title.text = favouriteVideo.title
        holder.customName.text = favouriteVideo.channelTitle
        Glide.with(context).load(favouriteVideo.thumbnail).into(holder.image)
        holder.videoCard.setOnClickListener {
            onVideoClick(
                com.google.android.piyush.dopamine.viewModels.SelectedVideo(
                    videoId = favouriteVideo.videoId ?: "",
                    channelId = favouriteVideo.channelId ?: "",
                    title = favouriteVideo.title,
                    description = "",
                    thumbnailUrl = favouriteVideo.thumbnail,
                    channelTitle = favouriteVideo.channelTitle ?: ""
                )
            )
        }
    }
}

private class FavouriteDiffCallback : DiffUtil.ItemCallback<EntityFavouritePlaylist>() {
    override fun areItemsTheSame(oldItem: EntityFavouritePlaylist, newItem: EntityFavouritePlaylist): Boolean {
        return oldItem.videoId == newItem.videoId
    }

    override fun areContentsTheSame(oldItem: EntityFavouritePlaylist, newItem: EntityFavouritePlaylist): Boolean {
        return oldItem == newItem
    }
}