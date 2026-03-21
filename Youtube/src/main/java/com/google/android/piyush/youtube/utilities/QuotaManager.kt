package com.google.android.piyush.youtube.utilities

import java.util.concurrent.atomic.AtomicInteger

object QuotaManager {

    // YouTube API v3 quota costs per operation (in units)
    private val QUOTA_COSTS = mapOf(
        "videos.list" to 1,
        "videos.rate" to 50,
        "videos.getRating" to 1,
        "search.list" to 100,
        "channels.list" to 1,
        "playlists.list" to 1,
        "playlistItems.list" to 1,
        "playlistItems.insert" to 50,
        "playlistItems.delete" to 50,
        "commentThreads.list" to 1,
        "comments.insert" to 50,
        "comments.update" to 50,
        "comments.delete" to 50,
        "comments.markAsSpam" to 50,
        "comments.setModerationStatus" to 50,
        "subscriptions.list" to 1,
        "subscriptions.insert" to 50,
        "subscriptions.delete" to 50,
        "activities.list" to 1,
        "i18nLanguages.list" to 1,
        "i18nRegions.list" to 1,
        "captions.list" to 50,
        "captions.download" to 200
    )

    // Daily quota limit (default 10,000 units for YouTube API v3)
    private const val DAILY_QUOTA_LIMIT = 10000
    private const val QUOTA_WARNING_THRESHOLD = 0.8

    private val usedQuota = AtomicInteger(0)
    private var lastResetDay = getTodayDay()

    fun getCost(operation: String): Int {
        return QUOTA_COSTS[operation] ?: 1
    }

    fun trackUsage(operation: String) {
        checkDayReset()
        val cost = getCost(operation)
        usedQuota.addAndGet(cost)
    }

    fun canMakeRequest(operation: String): Boolean {
        checkDayReset()
        val cost = getCost(operation)
        return (usedQuota.get() + cost) <= DAILY_QUOTA_LIMIT
    }

    fun isNearQuotaLimit(): Boolean {
        checkDayReset()
        return usedQuota.get() >= (DAILY_QUOTA_LIMIT * QUOTA_WARNING_THRESHOLD).toInt()
    }

    fun getUsedQuota(): Int = usedQuota.get()

    fun getRemainingQuota(): Int {
        checkDayReset()
        return (DAILY_QUOTA_LIMIT - usedQuota.get()).coerceAtLeast(0)
    }

    fun getQuotaPercentage(): Float {
        checkDayReset()
        return (usedQuota.get().toFloat() / DAILY_QUOTA_LIMIT.toFloat()) * 100f
    }

    fun resetQuota() {
        usedQuota.set(0)
        lastResetDay = getTodayDay()
    }

    private fun checkDayReset() {
        val today = getTodayDay()
        if (today != lastResetDay) {
            usedQuota.set(0)
            lastResetDay = today
        }
    }

    private fun getTodayDay(): Int {
        return (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
    }
}
