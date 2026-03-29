package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.ChapterParser
import com.google.android.piyush.youtube.model.comments.CommentThreadItem

class CommentsAdapter(
    private val context: Context,
    private val allComments: MutableList<CommentThreadItem>,
    private val onReplyClick: (CommentThreadItem) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    companion object {
        private const val COLLAPSED_COUNT = 3
    }

    private var showAll = false

    /** Sorted by likes so top-liked comments appear first */
    private val sortedComments: List<CommentThreadItem>
        get() = allComments.sortedByDescending {
            it.snippet?.topLevelComment?.snippet?.likeCount ?: 0
        }

    private val visibleComments: List<CommentThreadItem>
        get() = if (showAll) sortedComments else sortedComments.take(COLLAPSED_COUNT)

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val authorImage: ImageView = view.findViewById(R.id.commentAuthorImage)
        val authorName: TextView   = view.findViewById(R.id.commentAuthorName)
        val commentTime: TextView  = view.findViewById(R.id.commentTime)
        val commentText: TextView  = view.findViewById(R.id.commentText)
        val likeCount: TextView    = view.findViewById(R.id.commentLikeCount)
        val replyCount: TextView   = view.findViewById(R.id.commentReplyCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = visibleComments[position]
        val snippet = comment.snippet?.topLevelComment?.snippet

        holder.authorName.text  = snippet?.authorDisplayName ?: "Anonymous"
        holder.commentText.text = HtmlCompat.fromHtml(
            snippet?.textDisplay ?: "",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
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

    override fun getItemCount(): Int = visibleComments.size

    /** Toggle expanded/collapsed. Returns true if now expanded. */
    fun toggleShowAll(): Boolean {
        showAll = !showAll
        notifyDataSetChanged()
        return showAll
    }

    /** Whether there are more comments than the collapsed threshold */
    fun hasMore(): Boolean = allComments.size > COLLAPSED_COUNT

    fun addComments(newComments: List<CommentThreadItem>) {
        allComments.addAll(newComments)
        notifyDataSetChanged()
    }

    fun clearComments() {
        allComments.clear()
        notifyDataSetChanged()
    }
}
