package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.dopamine.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class ShortsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val shortsPlayer: YouTubePlayerView = itemView.findViewById(R.id.shortsPlayer)
    val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    val title: TextView = itemView.findViewById(R.id.title)
    val views: TextView = itemView.findViewById(R.id.views)
    val duration: TextView = itemView.findViewById(R.id.duration)
}
