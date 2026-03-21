package com.google.android.piyush.youtube.model.ratings

import kotlinx.serialization.Serializable

@Serializable
data class VideoRatingResponse(
    val kind: String? = null,
    val etag: String? = null,
    val items: List<VideoRatingItem>? = null
)

@Serializable
data class VideoRatingItem(
    val videoId: String? = null,
    val rating: String? = null
)
