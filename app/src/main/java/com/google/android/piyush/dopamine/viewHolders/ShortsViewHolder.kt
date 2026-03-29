package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.google.android.piyush.dopamine.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class ShortsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    // Player + thumbnail (thumbnail-first pattern)
    val shortsPlayer: YouTubePlayerView = itemView.findViewById(R.id.shortsPlayer)
    val thumbnail: ShapeableImageView = itemView.findViewById(R.id.thumbnail)

    // Info overlay
    val channelAvatar: ShapeableImageView = itemView.findViewById(R.id.channelAvatar)
    val channelName: MaterialTextView = itemView.findViewById(R.id.channelName)
    val videoTitle: MaterialTextView = itemView.findViewById(R.id.videoTitle)
    val views: MaterialTextView = itemView.findViewById(R.id.views)
    val followBtn: MaterialButton = itemView.findViewById(R.id.followBtn)

    // Action buttons
    val likeBtn: ShapeableImageView = itemView.findViewById(R.id.likeBtn)
    val likeCount: MaterialTextView = itemView.findViewById(R.id.likeCount)
    val dislikeBtn: ShapeableImageView = itemView.findViewById(R.id.dislikeBtn)
    val shareBtn: ShapeableImageView = itemView.findViewById(R.id.shareBtn)
    val moreBtn: ShapeableImageView = itemView.findViewById(R.id.moreBtn)

    // Mute toggle
    val muteBtn: ShapeableImageView = itemView.findViewById(R.id.muteBtn)
    val muteCard: MaterialCardView = itemView.findViewById(R.id.muteCard)

    // Play/Pause overlay
    val playPauseCard: MaterialCardView = itemView.findViewById(R.id.playPauseCard)
    val playPauseIcon: ShapeableImageView = itemView.findViewById(R.id.playPauseIcon)

    /** Guard against double-initialize when user swipes back to this page */
    var isPlayerInitialized = false
    var isPlayerInitializing = false
    var currentVideoId: String? = null

    /** Holds the live YouTubePlayer once initialized, so we can reuse it on revisit */
    var cachedPlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer? = null

    fun release() {
        shortsPlayer.release()
        isPlayerInitialized = false
        isPlayerInitializing = false
        currentVideoId = null
        cachedPlayer = null
    }
}
