package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.viewHolders.HomeViewHolder
import com.google.android.piyush.youtube.model.Item
import com.google.android.piyush.youtube.model.Youtube
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HomeAdapter(
    private val context: Context,
    youtube: Youtube?,
    private val onVideoClick: (com.google.android.piyush.dopamine.viewModels.SelectedVideo) -> Unit
) : ListAdapter<Item, HomeViewHolder>(HomeDiffCallback()) {

    init {
        submitList(youtube?.items ?: emptyList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_fragment_home, parent, false)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = getItem(position)
        val snippet = item.snippet ?: return

        val publishedTime = formatDuration(
            ChronoUnit.SECONDS.between(
                LocalDateTime.parse(
                    snippet.publishedAt, DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.now()
            )
        )
        val publishedViews = viewsCount(
            item.statistics?.viewCount?.toIntOrNull() ?: 0
        )

        val channelTitleStr = "${snippet.channelTitle} • $publishedViews • $publishedTime"

        holder.videoTitle.text = snippet.title
        holder.channelTitle.text = channelTitleStr

        Glide.with(context)
            .load(snippet.thumbnails?.default?.url)
            .into(holder.imageView)

        Glide.with(context)
            .load(snippet.thumbnails?.high?.url)
            .into(holder.youTubePlayerView)

        holder.videoDuration.text = formatDuration(
            Duration.parse(item.contentDetails?.duration ?: "PT0S")
        )

        item.contentDetails?.definition?.let { definition ->
            if (definition.equals("hd", ignoreCase = true)) {
                holder.qualityCard.visibility = View.VISIBLE
                holder.videoQuality.text = "HD"
            } else {
                holder.qualityCard.visibility = View.GONE
            }
        } ?: run {
            holder.qualityCard.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if(NetworkUtilities.isNetworkAvailable(context)) {
                onVideoClick(
                    com.google.android.piyush.dopamine.viewModels.SelectedVideo(
                        videoId = item.id ?: "",
                        channelId = snippet.channelId ?: "",
                        title = snippet.title,
                        description = snippet.description,
                        thumbnailUrl = snippet.thumbnails?.high?.url,
                        channelTitle = snippet.channelTitle
                    )
                )
                Log.d("FragmentHome", "videoId: ${item.id}")
                Log.d("FragmentHome", "channelId: ${snippet.channelId}")
            }else{
                NetworkUtilities.showNetworkError(context)
            }
            Log.d("FragmentHome", "videoData: $item")
        }
    }

    private fun viewsCount(views: Int): String {
        return when {
            views >= 1000000000 -> {
                val formattedViews = views / 1000000000
                "${formattedViews}B views"
            }
            views >= 1000000 -> {
                val formattedViews = views / 1000000
                "${formattedViews}M views"
            }
            views >= 1000 -> {
                val formattedViews = views / 1000
                "${formattedViews}K views"
            }
            else -> {
                "$views views"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        val secondsRemaining = seconds % 60

        return when {
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "$secondsRemaining seconds"
        }
    }
}

private class HomeDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}