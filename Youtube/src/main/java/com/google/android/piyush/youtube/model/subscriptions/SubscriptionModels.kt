package com.google.android.piyush.youtube.model.subscriptions

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionsResponse(
    val kind: String? = null,
    val etag: String? = null,
    val nextPageToken: String? = null,
    val pageInfo: SubscriptionsPageInfo? = null,
    val items: List<SubscriptionItem>? = null
)

@Serializable
data class SubscriptionsPageInfo(
    val totalResults: Int? = null,
    val resultsPerPage: Int? = null
)

@Serializable
data class SubscriptionItem(
    val kind: String? = null,
    val etag: String? = null,
    val id: String? = null,
    val snippet: SubscriptionSnippet? = null
)

@Serializable
data class SubscriptionSnippet(
    val publishedAt: String? = null,
    val title: String? = null,
    val description: String? = null,
    val resourceId: ResourceId? = null,
    val channelId: String? = null,
    val thumbnails: SubscriptionThumbnails? = null
)

@Serializable
data class ResourceId(
    val kind: String? = null,
    val channelId: String? = null
)

@Serializable
data class SubscriptionThumbnails(
    val default: SubscriptionThumbnail? = null,
    val medium: SubscriptionThumbnail? = null,
    val high: SubscriptionThumbnail? = null
)

@Serializable
data class SubscriptionThumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class SubscriptionInsertBody(
    val snippet: SubscriptionInsertSnippet? = null
)

@Serializable
data class SubscriptionInsertSnippet(
    val resourceId: ResourceId? = null
)
