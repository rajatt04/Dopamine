package com.google.android.piyush.youtube.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.piyush.youtube.model.comments.CommentThreadItem
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl

class CommentsPagingSource(
    private val repository: YoutubeRepositoryImpl,
    private val videoId: String,
    private val order: String = "relevance"
) : PagingSource<String, CommentThreadItem>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, CommentThreadItem> {
        return try {
            val pageToken = params.key
            val response = repository.getCommentThreads(
                videoId = videoId,
                order = order,
                pageToken = pageToken,
                maxResults = params.loadSize
            )

            when (response) {
                is com.google.android.piyush.youtube.utilities.NetworkResult.Success -> {
                    val items = response.data.items ?: emptyList()
                    LoadResult.Page(
                        data = items,
                        prevKey = null,
                        nextKey = response.data.nextPageToken
                    )
                }
                is com.google.android.piyush.youtube.utilities.NetworkResult.Error -> {
                    LoadResult.Error(Exception(response.message))
                }
                is com.google.android.piyush.youtube.utilities.NetworkResult.Loading -> {
                    LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, CommentThreadItem>): String? {
        return null
    }
}
