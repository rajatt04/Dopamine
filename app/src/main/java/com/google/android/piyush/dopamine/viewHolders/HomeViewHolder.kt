package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.piyush.dopamine.R

class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val youTubePlayerView: ShapeableImageView = itemView.findViewById(R.id.youtube_player_view)
    val imageView: ShapeableImageView = itemView.findViewById(R.id.channel_image)
    val videoTitle: TextView = itemView.findViewById(R.id.video_title)
    val channelTitle: TextView = itemView.findViewById(R.id.channel_title)
    val videoDuration: TextView = itemView.findViewById(R.id.video_duration)
    val videoViews: TextView = itemView.findViewById(R.id.video_views)
    val videoPublished: TextView = itemView.findViewById(R.id.video_published)
}
