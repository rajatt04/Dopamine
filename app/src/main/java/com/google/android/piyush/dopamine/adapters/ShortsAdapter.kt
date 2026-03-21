package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.google.android.piyush.dopamine.viewHolders.ShortsViewHolder
import com.google.android.piyush.youtube.model.Shorts

class ShortsAdapter(
    private val context: Context,
    private var shorts: List<Shorts>?
) : RecyclerView.Adapter<ShortsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortsViewHolder {
        return ShortsViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_fragment_shorts, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return shorts?.size ?: 0
    }

    override fun onBindViewHolder(holder: ShortsViewHolder, position: Int) {
        val short = shorts?.getOrNull(position) ?: return

        short.videoId?.let { videoId ->
            holder.shortsPlayer.getYouTubePlayerWhenReady(object : com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }
            })
        }

        holder.title.text = short.title ?: "Short"
        holder.views.text = formatViews(short.viewCount)
        holder.duration.text = short.duration ?: "0:00"

        short.thumbnail?.let { url ->
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.default_user)
                .into(holder.thumbnail)
        }

        holder.itemView.setOnClickListener {
            short.videoId?.let { videoId ->
                context.startActivity(
                    Intent(context, YoutubePlayer::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("videoId", videoId)
                )
            }
        }
    }

    private fun formatViews(viewCount: String?): String {
        val views = viewCount?.toLongOrNull() ?: return "0 views"
        return when {
            views >= 1_000_000_000 -> "${views / 1_000_000_000}B views"
            views >= 1_000_000 -> "${views / 1_000_000}M views"
            views >= 1_000 -> "${views / 1_000}K views"
            else -> "$views views"
        }
    }

    fun clearData() {
        shorts = null
        notifyDataSetChanged()
    }
}
