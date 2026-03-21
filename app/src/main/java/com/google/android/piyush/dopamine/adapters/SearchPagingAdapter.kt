package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubeChannel
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.google.android.piyush.dopamine.viewHolders.SearchViewHolder
import com.google.android.piyush.youtube.model.SearchTubeItems

class SearchPagingAdapter(
    private val context: Context
) : PagingDataAdapter<SearchTubeItems, SearchViewHolder>(SEARCH_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_fragment_search_videos, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val video = getItem(position) ?: return
        Glide.with(context)
            .load(video.snippet?.thumbnails?.high?.url)
            .into(holder.image)
        holder.text1.text = video.snippet?.title
        holder.text2.text = video.snippet?.channelTitle

        holder.channelview.setOnClickListener {
            if (video.snippet?.channelId.isNullOrEmpty()) {
                MaterialAlertDialogBuilder(context).apply {
                    this.setTitle("Error")
                    this.setMessage("Channel Id is null or channel not found")
                    this.setIcon(R.drawable.ic_dialog_error)
                    this.setCancelable(true)
                    this.setNegativeButton("Okay") { dialog, _ ->
                        dialog?.dismiss()
                    }
                }.create().show()
            } else {
                context.startActivity(
                    Intent(context, YoutubeChannel::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("channelId", video.snippet?.channelId)
                )
            }
        }

        holder.video.setOnClickListener {
            if (video.id?.videoId.isNullOrEmpty() || video.snippet?.channelId.isNullOrEmpty()) {
                MaterialAlertDialogBuilder(context).apply {
                    this.setTitle("Error")
                    this.setMessage("Either video id or channel id is null")
                    this.setIcon(R.drawable.ic_dialog_error)
                    this.setCancelable(true)
                    this.setNegativeButton("Okay") { dialog, _ ->
                        dialog?.dismiss()
                    }
                }.create().show()
            } else {
                context.startActivity(
                    Intent(context, YoutubePlayer::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("videoId", video.id?.videoId)
                        .putExtra("channelId", video.snippet?.channelId)
                )
            }
        }
    }

    companion object {
        private val SEARCH_COMPARATOR = object : DiffUtil.ItemCallback<SearchTubeItems>() {
            override fun areItemsTheSame(oldItem: SearchTubeItems, newItem: SearchTubeItems): Boolean {
                return oldItem.id?.videoId == newItem.id?.videoId
            }

            override fun areContentsTheSame(oldItem: SearchTubeItems, newItem: SearchTubeItems): Boolean {
                return oldItem == newItem
            }
        }
    }
}
