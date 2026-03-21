package com.google.android.piyush.youtube.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.piyush.youtube.model.SearchTubeItems
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl

class SearchPagingSource(
    private val repository: YoutubeRepositoryImpl,
    private val query: String,
    private val order: String = "relevance",
    private val type: String? = null,
    private val videoDuration: String? = null
) : PagingSource<String, SearchTubeItems>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, SearchTubeItems> {
        return try {
            val pageToken = params.key
            val response = repository.getSearchVideosPaged(
                query = query,
                pageToken = pageToken,
                maxResults = params.loadSize,
                order = order,
                type = type,
                videoDuration = videoDuration
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

    override fun getRefreshKey(state: PagingState<String, SearchTubeItems>): String? {
        return null
    }
}
