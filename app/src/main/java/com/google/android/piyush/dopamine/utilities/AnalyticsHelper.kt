package com.google.android.piyush.dopamine.utilities

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AnalyticsHelper {

    private var analytics: FirebaseAnalytics? = null
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
        crashlytics.setCrashlyticsCollectionEnabled(true)
    }

    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        breadcrumb("Screen: $screenName")
    }

    fun logVideoPlay(videoId: String, videoTitle: String?, channelName: String?) {
        val bundle = Bundle().apply {
            putString("video_id", videoId)
            putString("video_title", videoTitle ?: "Unknown")
            putString("channel_name", channelName ?: "Unknown")
        }
        analytics?.logEvent("video_play", bundle)
        breadcrumb("Video play: $videoId - $videoTitle")
    }

    fun logSearch(searchQuery: String, resultCount: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, searchQuery)
            putInt("result_count", resultCount)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        breadcrumb("Search: '$searchQuery' - $resultCount results")
    }

    fun logLikeAction(videoId: String, isLiked: Boolean) {
        val bundle = Bundle().apply {
            putString("video_id", videoId)
            putBoolean("is_liked", isLiked)
        }
        analytics?.logEvent("like_action", bundle)
        breadcrumb("Like action: video=$videoId liked=$isLiked")
    }

    fun logSubscribeAction(channelId: String, channelName: String?, isSubscribed: Boolean) {
        val bundle = Bundle().apply {
            putString("channel_id", channelId)
            putString("channel_name", channelName ?: "Unknown")
            putBoolean("is_subscribed", isSubscribed)
        }
        analytics?.logEvent("subscribe_action", bundle)
        breadcrumb("Subscribe: $channelId subscribed=$isSubscribed")
    }

    fun logShareAction(videoId: String, videoTitle: String?) {
        val bundle = Bundle().apply {
            putString("video_id", videoId)
            putString("video_title", videoTitle ?: "Unknown")
        }
        analytics?.logEvent("share_action", bundle)
        breadcrumb("Share: $videoId")
    }

    fun logCommentAction(videoId: String, isReply: Boolean) {
        val bundle = Bundle().apply {
            putString("video_id", videoId)
            putBoolean("is_reply", isReply)
        }
        analytics?.logEvent("comment_action", bundle)
    }

    fun logApiError(endpoint: String, errorCode: Int?, errorMessage: String?) {
        val bundle = Bundle().apply {
            putString("endpoint", endpoint)
            putInt("error_code", errorCode ?: -1)
            putString("error_message", errorMessage ?: "Unknown")
        }
        analytics?.logEvent("api_error", bundle)
        breadcrumb("API Error: $endpoint code=$errorCode msg=$errorMessage")
    }

    fun setUserId(userId: String?) {
        analytics?.setUserId(userId)
        crashlytics.setUserId(userId ?: "")
    }

    fun setUserProperty(key: String, value: String?) {
        analytics?.setUserProperty(key, value)
    }

    fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
}
