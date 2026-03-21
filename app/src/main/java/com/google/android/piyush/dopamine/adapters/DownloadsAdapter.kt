package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.database.entities.EntityDownload
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.activities.YoutubePlayer

class DownloadsAdapter(
    private val context: Context,
    private val onCancel: (EntityDownload) -> Unit,
    private val onDelete: (EntityDownload) -> Unit,
    private val onRetry: (EntityDownload) -> Unit
) : ListAdapter<EntityDownload, DownloadsAdapter.DownloadViewHolder>(DOWNLOAD_COMPARATOR) {

    inner class DownloadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.downloadThumbnail)
        val title: TextView = view.findViewById(R.id.downloadTitle)
        val channelName: TextView = view.findViewById(R.id.downloadChannelName)
        val statusText: TextView = view.findViewById(R.id.downloadStatusText)
        val progressBar: ProgressBar = view.findViewById(R.id.downloadProgressBar)
        val progressText: TextView = view.findViewById(R.id.downloadProgressText)
        val actionButton: ImageButton = view.findViewById(R.id.downloadActionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_download, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val download = getItem(position)

        holder.title.text = download.title ?: "Unknown Title"
        holder.channelName.text = download.channelTitle ?: "Unknown Channel"

        if (!download.thumbnail.isNullOrEmpty()) {
            Glide.with(context).load(download.thumbnail).into(holder.thumbnail)
        }

        when (download.status) {
            EntityDownload.STATUS_PENDING -> {
                holder.statusText.text = "Pending..."
                holder.progressBar.visibility = View.VISIBLE
                holder.progressBar.progress = 0
                holder.progressText.visibility = View.GONE
                holder.actionButton.setImageResource(R.drawable.ic_cancel)
                holder.actionButton.setOnClickListener { onCancel(download) }
            }
            EntityDownload.STATUS_DOWNLOADING -> {
                holder.statusText.text = "Downloading"
                holder.progressBar.visibility = View.VISIBLE
                holder.progressBar.progress = download.progress
                holder.progressText.visibility = View.VISIBLE
                holder.progressText.text = "${download.progress}%"
                holder.actionButton.setImageResource(R.drawable.ic_cancel)
                holder.actionButton.setOnClickListener { onCancel(download) }
            }
            EntityDownload.STATUS_COMPLETED -> {
                holder.statusText.text = "Completed"
                holder.progressBar.visibility = View.GONE
                holder.progressText.visibility = View.GONE
                holder.actionButton.setImageResource(R.drawable.ic_delete)
                holder.actionButton.setOnClickListener { onDelete(download) }
                holder.itemView.setOnClickListener {
                    val filePath = download.filePath
                    if (!filePath.isNullOrEmpty()) {
                        context.startActivity(
                            Intent(context, YoutubePlayer::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra("videoId", download.videoId)
                                .putExtra("channelId", download.channelId)
                                .putExtra("filePath", filePath)
                        )
                    }
                }
            }
            EntityDownload.STATUS_FAILED -> {
                holder.statusText.text = "Failed"
                holder.progressBar.visibility = View.GONE
                holder.progressText.visibility = View.GONE
                holder.actionButton.setImageResource(R.drawable.ic_retry)
                holder.actionButton.setOnClickListener { onRetry(download) }
            }
            EntityDownload.STATUS_PAUSED -> {
                holder.statusText.text = "Paused"
                holder.progressBar.visibility = View.VISIBLE
                holder.progressBar.progress = download.progress
                holder.progressText.visibility = View.VISIBLE
                holder.progressText.text = "${download.progress}%"
                holder.actionButton.setImageResource(R.drawable.ic_cancel)
                holder.actionButton.setOnClickListener { onCancel(download) }
            }
        }
    }

    companion object {
        private val DOWNLOAD_COMPARATOR = object : DiffUtil.ItemCallback<EntityDownload>() {
            override fun areItemsTheSame(oldItem: EntityDownload, newItem: EntityDownload): Boolean {
                return oldItem.videoId == newItem.videoId
            }

            override fun areContentsTheSame(oldItem: EntityDownload, newItem: EntityDownload): Boolean {
                return oldItem == newItem
            }
        }
    }
}
