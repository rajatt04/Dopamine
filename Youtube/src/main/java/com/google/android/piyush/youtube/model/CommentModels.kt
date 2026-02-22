package com.google.android.piyush.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class CommentThreads(
    val items: List<CommentThreadItem>? = null,
    val nextPageToken: String? = null
)

@Serializable
data class CommentThreadItem(
    val snippet: CommentThreadSnippet? = null
)

@Serializable
data class CommentThreadSnippet(
    val videoId: String? = null,
    val topLevelComment: TopLevelComment? = null,
    val totalReplyCount: Int? = null
)

@Serializable
data class TopLevelComment(
    val snippet: CommentSnippet? = null
)

@Serializable
data class CommentSnippet(
    val textDisplay: String? = null,
    val authorDisplayName: String? = null,
    val authorProfileImageUrl: String? = null,
    val likeCount: Int? = null,
    val publishedAt: String? = null
)
