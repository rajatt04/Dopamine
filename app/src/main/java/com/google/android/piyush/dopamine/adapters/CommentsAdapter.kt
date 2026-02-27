package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.youtube.model.CommentThreadItem
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class CommentsAdapter(
    private val context: Context,
    comments: List<CommentThreadItem>
) : ListAdapter<CommentThreadItem, CommentsAdapter.ViewHolder>(CommentDiffCallback()) {

    init {
        submitList(comments)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorImage: ShapeableImageView = itemView.findViewById(R.id.authorImage)
        val authorName: TextView = itemView.findViewById(R.id.authorName)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val likeCount: TextView = itemView.findViewById(R.id.likeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position).snippet?.topLevelComment?.snippet

        holder.commentText.text = item?.textDisplay
        holder.likeCount.text = item?.likeCount?.toString() ?: "0"

        val authorName = item?.authorDisplayName ?: "Anonymous"
        val publishedAt = item?.publishedAt ?: ""

        val timeAgo = if (publishedAt.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val odt = OffsetDateTime.parse(publishedAt)
                formatTimeAgo(odt.toLocalDateTime())
            } catch (e: Exception) {
                ""
            }
        } else ""

        holder.authorName.text = if (timeAgo.isNotEmpty()) "$authorName • $timeAgo" else authorName

        Glide.with(context)
            .load(item?.authorProfileImageUrl)
            .circleCrop()
            .placeholder(R.mipmap.ic_launcher_round)
            .into(holder.authorImage)
    }

    private fun formatTimeAgo(dateTime: LocalDateTime): String {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return ""

        val now = LocalDateTime.now()
        val seconds = ChronoUnit.SECONDS.between(dateTime, now)
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)
        val months = ChronoUnit.MONTHS.between(dateTime, now)
        val years = ChronoUnit.YEARS.between(dateTime, now)

        return when {
            years > 0 -> "$years y"
            months > 0 -> "$months mo"
            days > 0 -> "$days d"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes m"
            else -> "$seconds s"
        }
    }
}

private class CommentDiffCallback : DiffUtil.ItemCallback<CommentThreadItem>() {
    override fun areItemsTheSame(oldItem: CommentThreadItem, newItem: CommentThreadItem): Boolean {
        // CommentThreadItem has no direct id field; use the comment text + author as identity
        val oldSnippet = oldItem.snippet?.topLevelComment?.snippet
        val newSnippet = newItem.snippet?.topLevelComment?.snippet
        return oldSnippet?.authorDisplayName == newSnippet?.authorDisplayName
                && oldSnippet?.publishedAt == newSnippet?.publishedAt
    }

    override fun areContentsTheSame(oldItem: CommentThreadItem, newItem: CommentThreadItem): Boolean {
        return oldItem == newItem
    }
}
