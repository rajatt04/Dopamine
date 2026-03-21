package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.viewHolders.HomeViewHolder
import com.google.android.piyush.youtube.model.Youtube
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HomeAdapter(
    private val context: Context,
    private var youtube : Youtube?
) : RecyclerView.Adapter<HomeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_fragment_home, parent, false)
        )
    }

    override fun getItemCount(): Int {
       return youtube?.items?.size ?: 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = youtube?.items?.get(position) ?: return

        val publishedTime = try {
            formatDuration(
                ChronoUnit.SECONDS.between(
                    LocalDateTime.parse(item.snippet?.publishedAt, DateTimeFormatter.ISO_DATE_TIME),
                    LocalDateTime.now()
                )
            )
        } catch (e: Exception) {
            ""
        }

        val publishedViews = try {
            viewsCount(item.statistics?.viewCount?.toInt() ?: 0)
        } catch (e: Exception) {
            ""
        }

        holder.videoTitle.text = item.snippet?.title ?: ""
        holder.channelTitle.text = item.snippet?.channelTitle ?: ""
        holder.videoViews.text = publishedViews
        holder.videoPublished.text = publishedTime

        Glide.with(holder.itemView.context)
            .load(item.snippet?.thumbnails?.default?.url)
            .circleCrop()
            .into(holder.imageView)

        Glide.with(holder.itemView.context)
            .load(item.snippet?.thumbnails?.high?.url)
            .into(holder.youTubePlayerView)

        try {
            holder.videoDuration.text = formatDuration(
                Duration.parse(item.contentDetails?.duration ?: "PT0S")
            )
        } catch (e: Exception) {
            holder.videoDuration.text = ""
        }

        holder.itemView.setOnClickListener {
            if(NetworkUtilities.isNetworkAvailable(context)) {
                context.startActivity(
                    Intent(context, YoutubePlayer::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("videoId", item.id)
                        .putExtra("channelId", item.snippet?.channelId)
                )
            } else {
                NetworkUtilities.showNetworkError(context)
            }
        }
    }

    fun clearData() {
        youtube = null
        notifyDataSetChanged()
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