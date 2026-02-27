package com.google.android.piyush.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentDetails(
    val duration: String? = null,
    val videoPublishedAt: String? = null,
    val videoId: String? = null,
    val caption: String? = null,
    val definition: String? = null,
    val licensedContent: Boolean? = null,
    val regionRestriction: RegionRestriction? = null
)

@Serializable
data class RegionRestriction(
    val allowed: List<String>? = null,
    val blocked: List<String>? = null
)
