package com.google.android.piyush.dopamine.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.SubscriptionEntity
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.viewHolders.ShortsViewHolder
import com.google.android.piyush.youtube.model.Shorts
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import kotlin.random.Random

class ShortsAdapter(
    private val context: Context,
    private val shorts: List<Shorts>?,
    private val databaseViewModel: DatabaseViewModel
) : RecyclerView.Adapter<ShortsViewHolder>() {

    private var likedVideoIds: Set<String> = emptySet()
    private var subscribedChannelIds: Set<String> = emptySet()

    // Track YouTube player instances per position for play/pause control
    private val playerMap = mutableMapOf<Int, YouTubePlayer>()
    private val videoIdMap = mutableMapOf<Int, String>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateLikedVideos(ids: Set<String>) {
        likedVideoIds = ids
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSubscribedChannels(ids: Set<String>) {
        subscribedChannelIds = ids
        notifyDataSetChanged()
    }

    /** Called by the fragment when the user swipes to a new page */
    fun playAt(position: Int) {
        playerMap[position]?.let { player ->
            videoIdMap[position]?.let { vid ->
                player.loadVideo(vid, 0f)
            }
        }
    }

    /** Called by the fragment when leaving a page */
    fun pauseAt(position: Int) {
        playerMap[position]?.pause()
    }

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
        val currentShort = shorts?.get(position) ?: return
        val videoId = currentShort.videoId
        val channelId = currentShort.channelId ?: "UC_x5XG1OV2P6uZZ5FSM9Ttw"

        // Bind Data
        holder.videoTitle.text = currentShort.title ?: "Shorts Video"
        holder.channelName.text = currentShort.channelTitle ?: "@Channel"
        holder.textLikeCount.text = "Like"

        if (!currentShort.thumbnail.isNullOrEmpty()) {
            Glide.with(context).load(currentShort.thumbnail).circleCrop().into(holder.channelImage)
        } else {
            holder.channelImage.setImageResource(R.mipmap.ic_launcher_round)
        }

        // Initialize Player (only once per ViewHolder)
        if (videoId != null) {
            videoIdMap[position] = videoId

            if (!holder.isInitialized) {
                // First time — initialize the player
                holder.isInitialized = true

                val iFrameOptions = IFramePlayerOptions.Builder(context)
                    .controls(0)
                    .rel(0)
                    .build()

                holder.shortsPlayer.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        holder.youTubePlayer = youTubePlayer
                        playerMap[position] = youTubePlayer
                        if (position == 0) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        } else {
                            youTubePlayer.cueVideo(videoId, 0f)
                        }
                    }
                }, true, iFrameOptions)
            } else {
                // Already initialized — just swap the video
                holder.youTubePlayer?.let { player ->
                    playerMap[position] = player
                    if (position == 0) {
                        player.loadVideo(videoId, 0f)
                    } else {
                        player.cueVideo(videoId, 0f)
                    }
                }
            }
        }

        // Like Button State & Click
        val isLiked = likedVideoIds.contains(videoId)
        updateLikeButton(holder, isLiked)

        holder.btnLike.setOnClickListener {
            if (videoId != null) {
                if (isLiked) {
                    databaseViewModel.deleteFavouriteVideo(videoId)
                } else {
                    val entity = EntityFavouritePlaylist(
                        videoId = videoId,
                        title = currentShort.title ?: "Shorts",
                        thumbnail = currentShort.thumbnail,
                        channelTitle = currentShort.channelTitle,
                        channelId = channelId
                    )
                    databaseViewModel.insertFavouriteVideos(entity)
                }
            }
        }

        // Subscribe Button State & Click
        val isSubscribed = subscribedChannelIds.contains(channelId)
        updateSubscribeButton(holder, isSubscribed)

        holder.btnSubscribe.setOnClickListener {
            if (isSubscribed) {
                databaseViewModel.deleteSubscription(channelId)
            } else {
                val entity = SubscriptionEntity(
                    channelId = channelId,
                    title = currentShort.channelTitle ?: "Channel",
                    description = "",
                    thumbnail = currentShort.thumbnail,
                    channelTitle = currentShort.channelTitle
                )
                databaseViewModel.insertSubscription(entity)
            }
        }
        
        // Other Actions
        holder.btnDislike.setOnClickListener {
            Toast.makeText(context, "Dislike not supported yet", Toast.LENGTH_SHORT).show()
        }

        holder.btnComment.setOnClickListener {
             Toast.makeText(context, "Comments not supported yet", Toast.LENGTH_SHORT).show()
        }

        holder.btnShare.setOnClickListener {
             val shareIntent = Intent(Intent.ACTION_SEND)
             shareIntent.type = "text/plain"
             shareIntent.putExtra(Intent.EXTRA_TEXT, "Watch this cool short: https://youtu.be/$videoId")
             context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
        }
    }

    override fun onViewRecycled(holder: ShortsViewHolder) {
        super.onViewRecycled(holder)
        // Clean up player reference when view is recycled
        @Suppress("DEPRECATION")
        val pos = holder.adapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            playerMap.remove(pos)
            videoIdMap.remove(pos)
        }
    }

    private fun updateLikeButton(holder: ShortsViewHolder, isLiked: Boolean) {
        if (isLiked) {
             holder.btnLike.setImageResource(R.drawable.ic_heart_filled)
             holder.btnLike.setColorFilter(context.resources.getColor(android.R.color.holo_red_dark, null))
             holder.textLikeCount.text = "Liked"
        } else {
             holder.btnLike.setImageResource(R.drawable.ic_like_video) 
             holder.btnLike.setColorFilter(context.resources.getColor(android.R.color.white, null))
             holder.textLikeCount.text = "Like"
        }
    }

    private fun updateSubscribeButton(holder: ShortsViewHolder, isSubscribed: Boolean) {
        if (isSubscribed) {
            holder.btnSubscribe.text = "Subscribed"
            holder.btnSubscribe.setBackgroundColor(context.resources.getColor(android.R.color.darker_gray, null))
             holder.btnSubscribe.setTextColor(context.resources.getColor(android.R.color.white, null))
        } else {
            holder.btnSubscribe.text = "Subscribe"
             holder.btnSubscribe.setBackgroundColor(context.resources.getColor(android.R.color.white, null))
             holder.btnSubscribe.setTextColor(context.resources.getColor(android.R.color.black, null))
        }
    }
}