package com.google.android.piyush.dopamine.utilities

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.android.piyush.database.entities.EntityDownload

object DownloadHelper {

    private const val DOWNLOAD_FOLDER = "Dopamine/Downloads"

    fun startDownload(
        context: Context,
        videoId: String,
        title: String?,
        videoUrl: String
    ): Long {
        val safeTitle = title?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: videoId
        val fileName = "${safeTitle}_${videoId}.mp4"

        val request = DownloadManager.Request(Uri.parse(videoUrl))
            .setTitle(title ?: "Downloading video")
            .setDescription("Downloading ${title ?: videoId}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "$DOWNLOAD_FOLDER/$fileName"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    fun getDownloadStatus(context: Context, downloadId: Long): Int {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        return if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            cursor.getInt(statusIndex)
        } else {
            -1
        }.also { cursor.close() }
    }

    fun getDownloadProgress(context: Context, downloadId: Long): Pair<Int, Long> {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        var result = Pair(0, 0L)
        if (cursor.moveToFirst()) {
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
            val bytesTotal = cursor.getLong(bytesTotalIndex)

            val progress = if (bytesTotal > 0) {
                ((bytesDownloaded * 100L) / bytesTotal).toInt()
            } else 0

            result = Pair(progress, bytesDownloaded)
        }
        cursor.close()
        return result
    }

    fun getDownloadFilePath(context: Context, downloadId: Long): String? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        return if (cursor.moveToFirst()) {
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            cursor.getString(uriIndex)
        } else {
            null
        }.also { cursor.close() }
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }

    fun getDownloadDirectory(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            .absolutePath + "/$DOWNLOAD_FOLDER"
    }

    fun mapDownloadManagerStatus(status: Int): Int {
        return when (status) {
            DownloadManager.STATUS_PENDING -> EntityDownload.STATUS_PENDING
            DownloadManager.STATUS_RUNNING -> EntityDownload.STATUS_DOWNLOADING
            DownloadManager.STATUS_PAUSED -> EntityDownload.STATUS_PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> EntityDownload.STATUS_COMPLETED
            DownloadManager.STATUS_FAILED -> EntityDownload.STATUS_FAILED
            else -> EntityDownload.STATUS_FAILED
        }
    }
}
