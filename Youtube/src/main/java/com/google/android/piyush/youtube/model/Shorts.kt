package com.google.android.piyush.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class Shorts(
    val videoId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val thumbnail: String? = null,
    val channelId: String? = null
)