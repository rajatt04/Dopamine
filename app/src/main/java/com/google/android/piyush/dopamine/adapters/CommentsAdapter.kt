package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.ChapterParser
import com.google.android.piyush.youtube.model.comments.CommentThreadItem

class CommentsAdapter(
    private val context: Context,
    private val comments: MutableList<CommentThreadItem>,
    private val onReplyClick: (CommentThreadItem) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val authorImage: ImageView = view.findViewById(R.id.commentAuthorImage)
        val authorName: TextView = view.findViewById(R.id.commentAuthorName)
        val commentTime: TextView = view.findViewById(R.id.commentTime)
        val commentText: TextView = view.findViewById(R.id.commentText)
        val likeCount: TextView = view.findViewById(R.id.commentLikeCount)
        val replyCount: TextView = view.findViewById(R.id.commentReplyCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val topLevelComment = comment.snippet?.topLevelComment
        val snippet = topLevelComment?.snippet

        holder.authorName.text = snippet?.authorDisplayName ?: "Anonymous"
        holder.commentText.text = snippet?.textDisplay ?: ""
        holder.commentTime.text = ChapterParser.getRelativeTime(snippet?.publishedAt)

        val likeCount = snippet?.likeCount ?: 0
        holder.likeCount.text = if (likeCount > 0) ChapterParser.formatViewCount(likeCount) else ""

        val replyCount = comment.snippet?.totalReplyCount ?: 0
        if (replyCount > 0) {
            holder.replyCount.visibility = View.VISIBLE
            holder.replyCount.text = "$replyCount replies"
            holder.replyCount.setOnClickListener { onReplyClick(comment) }
        } else {
            holder.replyCount.visibility = View.GONE
        }

        val authorImage = snippet?.authorProfileImageUrl
        if (!authorImage.isNullOrEmpty()) {
            Glide.with(context).load(authorImage).circleCrop().into(holder.authorImage)
        } else {
            holder.authorImage.setImageResource(R.drawable.default_user)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun addComments(newComments: List<CommentThreadItem>) {
        val startPos = comments.size
        comments.addAll(newComments)
        notifyItemRangeInserted(startPos, newComments.size)
    }

    fun clearComments() {
        comments.clear()
        notifyDataSetChanged()
    }
}
