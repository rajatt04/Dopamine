package com.google.android.piyush.youtube.model.comments

import kotlinx.serialization.Serializable

@Serializable
data class CommentThreadsResponse(
    val kind: String? = null,
    val etag: String? = null,
    val nextPageToken: String? = null,
    val pageInfo: CommentPageInfo? = null,
    val items: List<CommentThreadItem>? = null
)

@Serializable
data class CommentPageInfo(
    val totalResults: Int? = null,
    val resultsPerPage: Int? = null
)

@Serializable
data class CommentThreadItem(
    val kind: String? = null,
    val etag: String? = null,
    val id: String? = null,
    val snippet: CommentThreadSnippet? = null,
    val replies: CommentReplies? = null
)

@Serializable
data class CommentThreadSnippet(
    val channelId: String? = null,
    val videoId: String? = null,
    val topLevelComment: TopLevelComment? = null,
    val canReply: Boolean? = null,
    val totalReplyCount: Int? = null,
    val isPublic: Boolean? = null
)

@Serializable
data class TopLevelComment(
    val kind: String? = null,
    val etag: String? = null,
    val id: String? = null,
    val snippet: CommentSnippet? = null
)

@Serializable
data class CommentSnippet(
    val channelId: String? = null,
    val videoId: String? = null,
    val textDisplay: String? = null,
    val textOriginal: String? = null,
    val authorDisplayName: String? = null,
    val authorProfileImageUrl: String? = null,
    val authorChannelUrl: String? = null,
    val authorChannelId: AuthorChannelId? = null,
    val canRate: Boolean? = null,
    val viewerRating: String? = null,
    val likeCount: Int? = null,
    val publishedAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class AuthorChannelId(
    val value: String? = null
)

@Serializable
data class CommentReplies(
    val comments: List<CommentReplyItem>? = null
)

@Serializable
data class CommentReplyItem(
    val kind: String? = null,
    val etag: String? = null,
    val id: String? = null,
    val snippet: CommentSnippet? = null
)

@Serializable
data class CommentsResponse(
    val kind: String? = null,
    val etag: String? = null,
    val nextPageToken: String? = null,
    val pageInfo: CommentPageInfo? = null,
    val items: List<CommentReplyItem>? = null
)

@Serializable
data class CommentBody(
    val snippet: CommentBodySnippet? = null
)

@Serializable
data class CommentBodySnippet(
    val parentId: String? = null,
    val textOriginal: String? = null,
    val videoId: String? = null
)
