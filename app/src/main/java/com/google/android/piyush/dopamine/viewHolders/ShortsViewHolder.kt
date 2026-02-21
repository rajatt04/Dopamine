package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.dopamine.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class ShortsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val shortsPlayer : YouTubePlayerView = itemView.findViewById(R.id.shortsPlayer)
    val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
    val btnDislike: ImageView = itemView.findViewById(R.id.btnDislike)
    val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
    val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
    val btnSubscribe: MaterialButton = itemView.findViewById(R.id.btnSubscribe)
    val channelImage: ImageView = itemView.findViewById(R.id.channelImage)
    val channelName: TextView = itemView.findViewById(R.id.channelName)
    val videoTitle: TextView = itemView.findViewById(R.id.videoTitle)
    val textLikeCount: TextView = itemView.findViewById(R.id.textLikeCount)

    var youTubePlayer: YouTubePlayer? = null
    var isInitialized = false
}