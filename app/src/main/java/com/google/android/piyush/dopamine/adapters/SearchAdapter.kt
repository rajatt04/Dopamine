package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubeChannel
import com.google.android.piyush.dopamine.viewHolders.SearchViewHolder
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.model.SearchTubeItems

class SearchAdapter(
    private val context: Context,
    youtube: SearchTube?,
    private val onVideoClick: (com.google.android.piyush.dopamine.viewModels.SelectedVideo) -> Unit
) : ListAdapter<SearchTubeItems, SearchViewHolder>(SearchDiffCallback()) {

    init {
        submitList(youtube?.items ?: emptyList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_fragment_search_videos, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val video = getItem(position)
        Glide.with(context)
            .load(video.snippet?.thumbnails?.high?.url)
            .into(holder.image)
        holder.text1.text = video.snippet?.title
        holder.text2.text = video.snippet?.channelTitle

        // Search API doesn't return duration/quality by default.
        holder.durationCard.visibility = View.GONE
        holder.qualityCard.visibility = View.GONE

        holder.channelview.setOnClickListener {
            if(video.snippet?.channelId.isNullOrEmpty()){
                MaterialAlertDialogBuilder(context).apply {
                    this.setTitle("Error")
                    this.setMessage("Channel Id is null or channel not found")
                    this.setIcon(R.drawable.ic_dialog_error)
                    this.setCancelable(true)
                    this.setNegativeButton("Okay") { dialog, _ ->
                        dialog?.dismiss()
                    }
                }.create().show()
            }else {
                context.startActivity(
                    Intent(
                        context,
                        YoutubeChannel::class.java
                    )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("channelId", video.snippet?.channelId)
                )
                Log.d("checkId", video.snippet?.channelId!!)
            }
        }

        holder.video.setOnClickListener {
            if (video.id?.videoId.isNullOrEmpty() || video.snippet?.channelId.isNullOrEmpty()) {
                MaterialAlertDialogBuilder(context).apply {
                    this.setTitle("Error")
                    this.setMessage("Click Eye 👁 Button To View Channel")
                    this.setIcon(R.drawable.ic_dialog_error)
                    this.setCancelable(true)
                    this.setNegativeButton("Okay") { dialog, _ ->
                        dialog?.dismiss()
                    }
                }.create().show()
            } else {
                onVideoClick(
                    com.google.android.piyush.dopamine.viewModels.SelectedVideo(
                        videoId = video.id!!.videoId!!,
                        channelId = video.snippet!!.channelId!!,
                        title = video.snippet!!.title,
                        description = video.snippet!!.description,
                        thumbnailUrl = video.snippet!!.thumbnails?.high?.url,
                        channelTitle = video.snippet!!.channelTitle
                    )
                )
            }
        }
    }
}

private class SearchDiffCallback : DiffUtil.ItemCallback<SearchTubeItems>() {
    override fun areItemsTheSame(oldItem: SearchTubeItems, newItem: SearchTubeItems): Boolean {
        return oldItem.id?.videoId == newItem.id?.videoId
    }

    override fun areContentsTheSame(oldItem: SearchTubeItems, newItem: SearchTubeItems): Boolean {
        return oldItem == newItem
    }
}