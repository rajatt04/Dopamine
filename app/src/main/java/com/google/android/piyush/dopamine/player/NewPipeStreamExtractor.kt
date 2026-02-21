package com.google.android.piyush.dopamine.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import android.util.Log

/**
 * Utility to extract direct video stream URLs from YouTube video IDs
 * using the NewPipe Extractor library.
 */
object NewPipeStreamExtractor {

    private const val TAG = "NewPipeExtractor"
    private var isInitialized = false

    /**
     * Initialize NewPipe Extractor. Must be called once before extraction.
     */
    fun init() {
        if (!isInitialized) {
            NewPipe.init(NewPipeDownloaderImpl.getInstance())
            isInitialized = true
            Log.d(TAG, "NewPipe Extractor initialized")
        }
    }

    /**
     * Extract the best available video stream URL for a YouTube video ID.
     * Must be called from a coroutine (runs on IO dispatcher).
     *
     * @param videoId YouTube video ID (e.g. "dQw4w9WgXcQ")
     * @return Direct stream URL or null if extraction fails
     */
    suspend fun extractStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            init()
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(
                ServiceList.YouTube,
                url
            )

            // Try to get a video stream with audio (usually better quality)
            val videoStreams = streamInfo.videoStreams
                .filter { !it.isVideoOnly }
                .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }

            val bestStream = videoStreams.firstOrNull()

            if (bestStream != null) {
                Log.d(TAG, "Found stream: ${bestStream.resolution} - ${bestStream.format}")
                return@withContext bestStream.content
            }

            // Fallback: try video-only streams
            val videoOnlyStreams = streamInfo.videoStreams
                .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }

            val fallbackStream = videoOnlyStreams.firstOrNull()
            if (fallbackStream != null) {
                Log.d(TAG, "Fallback stream: ${fallbackStream.resolution}")
                return@withContext fallbackStream.content
            }

            Log.e(TAG, "No video streams found for $videoId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Stream extraction failed for $videoId", e)
            null
        }
    }

    /**
     * Extract full StreamInfo (includes title, description, etc.)
     */
    suspend fun extractStreamInfo(videoId: String): StreamInfo? = withContext(Dispatchers.IO) {
        try {
            init()
            val url = "https://www.youtube.com/watch?v=$videoId"
            StreamInfo.getInfo(ServiceList.YouTube, url)
        } catch (e: Exception) {
            Log.e(TAG, "StreamInfo extraction failed for $videoId", e)
            null
        }
    }
}
