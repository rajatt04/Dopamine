package com.google.android.piyush.dopamine.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.android.piyush.database.DopamineDatabase
import com.google.android.piyush.database.entities.EntityDownload
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DownloadProgressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DownloadProgressWorker"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val CHANNEL_ID = "download_progress_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return@withContext Result.failure()
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)

        if (downloadId == -1L) return@withContext Result.failure()

        val dao = DopamineDatabase.getDatabase(applicationContext).dopamineDao()

        try {
            createNotificationChannel()

            var isDownloading = true
            while (isDownloading && !isStopped) {
                val (progress, bytesDownloaded) = DownloadHelper.getDownloadProgress(applicationContext, downloadId)
                val status = DownloadHelper.getDownloadStatus(applicationContext, downloadId)

                dao.updateDownloadProgress(videoId, progress, bytesDownloaded)

                when (status) {
                    android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                        val filePath = DownloadHelper.getDownloadFilePath(applicationContext, downloadId)
                        if (filePath != null) {
                            dao.updateDownloadPath(videoId, filePath, EntityDownload.STATUS_COMPLETED)
                        }
                        showNotification(videoId, "Download complete", progress, true)
                        isDownloading = false
                    }
                    android.app.DownloadManager.STATUS_FAILED -> {
                        dao.updateDownloadStatus(videoId, EntityDownload.STATUS_FAILED)
                        showNotification(videoId, "Download failed", progress, true)
                        isDownloading = false
                    }
                    else -> {
                        showNotification(videoId, "Downloading...", progress, false)
                    }
                }

                delay(1000)
            }

            Result.success()
        } catch (e: Exception) {
            dao.updateDownloadStatus(videoId, EntityDownload.STATUS_FAILED)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: "download"
        return ForegroundInfo(
            NOTIFICATION_ID_BASE + videoId.hashCode(),
            createNotification("Downloading...", 0).build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String, progress: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Dopamine Downloads")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(progress < 100)
            .setProgress(100, progress, progress == 0)
    }

    private fun showNotification(videoId: String, message: String, progress: Int, finished: Boolean) {
        val notification = createNotification(message, progress)
        if (finished) {
            notification.setOngoing(false)
                .setAutoCancel(true)
        }
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            NOTIFICATION_ID_BASE + videoId.hashCode(),
            notification.build()
        )
    }
}
