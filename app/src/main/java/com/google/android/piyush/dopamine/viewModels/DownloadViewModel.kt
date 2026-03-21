package com.google.android.piyush.dopamine.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.piyush.database.DopamineDatabase
import com.google.android.piyush.database.entities.EntityDownload
import com.google.android.piyush.dopamine.utilities.DownloadHelper
import com.google.android.piyush.dopamine.workers.DownloadProgressWorker
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DopamineDatabase.getDatabase(application).dopamineDao()
    private val workManager = WorkManager.getInstance(application)

    val allDownloads: LiveData<List<EntityDownload>> = dao.getAllDownloads()

    fun getDownload(videoId: String): LiveData<EntityDownload?> = dao.getDownloadLive(videoId)

    fun startDownload(
        videoId: String,
        title: String?,
        thumbnail: String?,
        channelId: String?,
        channelTitle: String?,
        videoUrl: String
    ): Result<Unit> {
        if (DownloadHelper.isYouTubeVideo(videoUrl)) {
            return Result.failure(
                UnsupportedOperationException(
                    "YouTube videos cannot be downloaded directly. " +
                    "Use YouTube Premium for offline viewing."
                )
            )
        }

        viewModelScope.launch {
            val existingDownload = dao.getDownload(videoId)
            if (existingDownload != null && existingDownload.status == EntityDownload.STATUS_COMPLETED) {
                return@launch
            }

            val result = DownloadHelper.startDownload(
                context = getApplication(),
                videoId = videoId,
                title = title,
                videoUrl = videoUrl
            )

            val downloadId = result.getOrElse { -1L }
            val isSuccess = result.isSuccess

            val download = EntityDownload(
                videoId = videoId,
                title = title,
                thumbnail = thumbnail,
                channelId = channelId,
                channelTitle = channelTitle,
                filePath = null,
                downloadId = downloadId,
                status = if (isSuccess) EntityDownload.STATUS_DOWNLOADING else EntityDownload.STATUS_FAILED,
                createdAt = System.currentTimeMillis()
            )

            dao.insertDownload(download)

            if (isSuccess) {
                val workRequest = OneTimeWorkRequestBuilder<DownloadProgressWorker>()
                    .setInputData(
                        workDataOf(
                            DownloadProgressWorker.KEY_VIDEO_ID to videoId,
                            DownloadProgressWorker.KEY_DOWNLOAD_ID to downloadId
                        )
                    )
                    .addTag(DownloadProgressWorker.TAG)
                    .build()

                workManager.enqueueUniqueWork(
                    "download_$videoId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
        return Result.success(Unit)
    }

    fun cancelDownload(videoId: String) {
        viewModelScope.launch {
            val download = dao.getDownload(videoId)
            if (download != null && download.downloadId != -1L) {
                DownloadHelper.cancelDownload(getApplication(), download.downloadId)
            }
            dao.updateDownloadStatus(videoId, EntityDownload.STATUS_FAILED)
            workManager.cancelUniqueWork("download_$videoId")
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            val download = dao.getDownload(videoId)
            if (download != null && download.downloadId != -1L) {
                DownloadHelper.cancelDownload(getApplication(), download.downloadId)
            }
            dao.deleteDownload(videoId)
            workManager.cancelUniqueWork("download_$videoId")
        }
    }

    fun retryDownload(videoId: String, videoUrl: String) {
        viewModelScope.launch {
            val download = dao.getDownload(videoId) ?: return@launch
            deleteDownload(videoId)
            startDownload(
                videoId = videoId,
                title = download.title,
                thumbnail = download.thumbnail,
                channelId = download.channelId,
                channelTitle = download.channelTitle,
                videoUrl = videoUrl
            )
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            dao.deleteDownloadsByStatus(EntityDownload.STATUS_COMPLETED)
        }
    }

    suspend fun getCompletedDownloads(): List<EntityDownload> {
        return dao.getDownloadsByStatus(EntityDownload.STATUS_COMPLETED)
    }
}
