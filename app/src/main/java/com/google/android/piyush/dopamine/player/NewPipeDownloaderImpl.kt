package com.google.android.piyush.dopamine.player

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OkHttp-based Downloader implementation required by NewPipe Extractor.
 */
class NewPipeDownloaderImpl private constructor() : Downloader() {

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        @Volatile
        private var instance: NewPipeDownloaderImpl? = null

        fun getInstance(): NewPipeDownloaderImpl {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloaderImpl().also { instance = it }
            }
        }
    }

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(
                httpMethod,
                if (dataToSend != null) dataToSend.toRequestBody() else null
            )
            .url(url)

        // Add headers
        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        val responseBodyToReturn = response.body?.string()

        val latestUrl = response.request.url.toString()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            latestUrl
        )
    }
}
