package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer
import com.google.android.piyush.dopamine.viewHolders.ShortsViewHolder
import com.google.android.piyush.youtube.model.Shorts
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions

class ShortsAdapter(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private var shorts: List<Shorts>?
) : RecyclerView.Adapter<ShortsViewHolder>() {

    // Track which holder is currently active (playing)
    private var activeHolder: ShortsViewHolder? = null
    private var activePlayer: YouTubePlayer? = null
    private var isMuted = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fragment_shorts, parent, false)
        return ShortsViewHolder(view)
    }

    override fun getItemCount(): Int = shorts?.size ?: 0

    override fun onBindViewHolder(holder: ShortsViewHolder, position: Int) {
        val short = shorts?.getOrNull(position) ?: return

        // --- Thumbnail (shown immediately) ---
        Glide.with(context)
            .load(short.thumbnail)
            .centerCrop()
            .placeholder(R.drawable.default_user)
            .into(holder.thumbnail)

        holder.thumbnail.visibility = View.VISIBLE
        holder.shortsPlayer.visibility = View.GONE

        // --- Info overlay ---
        holder.channelName.text = short.channelTitle ?: ""
        holder.videoTitle.text = short.title ?: ""
        holder.views.text = formatViews(short.viewCount)
        holder.likeCount.text = "Like"

        // Channel avatar — use thumbnail as fallback if no avatar available
        Glide.with(context)
            .load(short.thumbnail)
            .circleCrop()
            .into(holder.channelAvatar)

        // --- Tap on thumbnail / item to open in YoutubePlayer ---
        holder.itemView.setOnClickListener {
            short.videoId?.let { videoId ->
                context.startActivity(
                    Intent(context, YoutubePlayer::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("videoId", videoId)
                )
            }
        }

        // --- Share button ---
        holder.shareBtn.setOnClickListener {
            short.videoId?.let { videoId ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://youtube.com/watch?v=$videoId")
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share via")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        // --- Mute toggle ---
        holder.muteCard.setOnClickListener {
            isMuted = !isMuted
            activePlayer?.let { player ->
                if (isMuted) player.mute() else player.unMute()
            }
            holder.muteBtn.setImageResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
            )
        }

        // --- Tap center to pause/play ---
        var isPlaying = true
        holder.itemView.setOnClickListener {
            if (activeHolder == holder) {
                if (isPlaying) {
                    activePlayer?.pause()
                    holder.playPauseIcon.setImageResource(R.drawable.ic_play)
                    holder.playPauseCard.visibility = View.VISIBLE
                } else {
                    activePlayer?.play()
                    holder.playPauseCard.visibility = View.GONE
                }
                isPlaying = !isPlaying
            }
        }
    }

    /**
     * Called by the Fragment when a new page becomes the current page.
     * Stops any active player and starts the new one with thumbnail-first pattern.
     */
    fun onPageSelected(position: Int, holder: ShortsViewHolder?) {
        // Pause and reset previous
        activePlayer?.pause()
        activeHolder?.let { prev ->
            prev.shortsPlayer.visibility = View.GONE
            prev.thumbnail.visibility = View.VISIBLE
            prev.playPauseCard.visibility = View.GONE
        }

        activeHolder = holder ?: return
        val currentHolder = holder
        val short = shorts?.getOrNull(position) ?: return

        short.videoId?.let { videoId ->
            currentHolder.currentVideoId = videoId

            if (currentHolder.isPlayerInitialized) {
                // Player already has a WebView — just seek & play, NO re-initialize
                currentHolder.cachedPlayer?.let { player ->
                    player.loadVideo(videoId, 0f)
                    if (isMuted) player.mute() else player.unMute()
                }
                // Make sure player is visible (thumbnail may have been re-shown)
                currentHolder.thumbnail.visibility = View.GONE
                currentHolder.shortsPlayer.visibility = View.VISIBLE
            } else if (!currentHolder.isPlayerInitializing) {
                // First visit — register lifecycle and initialize the WebView
                currentHolder.isPlayerInitializing = true
                lifecycle.addObserver(currentHolder.shortsPlayer)
                currentHolder.shortsPlayer.enableAutomaticInitialization = false

                val playerOptions = IFramePlayerOptions.Builder(context)
                    .controls(0)   // hide YouTube native controls
                    .rel(0)        // no related videos at end
                    .build()

                try {
                    currentHolder.shortsPlayer.initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            // Cache the player and mark as initialized
                            currentHolder.isPlayerInitialized = true
                            currentHolder.isPlayerInitializing = false
                            currentHolder.cachedPlayer = youTubePlayer
                            
                            if (activeHolder == currentHolder) {
                                activePlayer = youTubePlayer
                            }

                            currentHolder.currentVideoId?.let { latestVideoId ->
                                youTubePlayer.loadVideo(latestVideoId, 0f)
                            }
                            if (isMuted) youTubePlayer.mute() else youTubePlayer.unMute()

                            // Swap thumbnail → player only if it's still active
                            if (activeHolder == currentHolder) {
                                currentHolder.thumbnail.visibility = View.GONE
                                currentHolder.shortsPlayer.visibility = View.VISIBLE
                            } else {
                                youTubePlayer.pause()
                            }
                        }
                    }, playerOptions)
                } catch (e: IllegalStateException) {
                    currentHolder.isPlayerInitializing = false
                }
            }
        }
    }

    private fun formatViews(viewCount: String?): String {
        val views = viewCount?.toLongOrNull() ?: return "0 views"
        return when {
            views >= 1_000_000_000 -> "${views / 1_000_000_000}B views"
            views >= 1_000_000    -> "${views / 1_000_000}M views"
            views >= 1_000        -> "${views / 1_000}K views"
            else                  -> "$views views"
        }
    }

    fun clearData() {
        shorts = null
        activePlayer = null
        activeHolder = null
        notifyDataSetChanged()
    }
}
