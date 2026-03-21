package com.google.android.piyush.youtube.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotaManagerTest {

    @Before
    fun setUp() {
        QuotaManager.resetQuota()
    }

    @Test
    fun `getCost returns correct cost for known operations`() {
        assertEquals(1, QuotaManager.getCost("videos.list"))
        assertEquals(100, QuotaManager.getCost("search.list"))
        assertEquals(50, QuotaManager.getCost("videos.rate"))
        assertEquals(1, QuotaManager.getCost("channels.list"))
        assertEquals(1, QuotaManager.getCost("commentThreads.list"))
    }

    @Test
    fun `getCost returns 1 for unknown operations`() {
        assertEquals(1, QuotaManager.getCost("unknown.operation"))
    }

    @Test
    fun `trackUsage increments quota`() {
        assertEquals(0, QuotaManager.getUsedQuota())

        QuotaManager.trackUsage("videos.list")
        assertEquals(1, QuotaManager.getUsedQuota())

        QuotaManager.trackUsage("search.list")
        assertEquals(101, QuotaManager.getUsedQuota())
    }

    @Test
    fun `canMakeRequest returns true when quota available`() {
        assertTrue(QuotaManager.canMakeRequest("videos.list"))
        assertTrue(QuotaManager.canMakeRequest("search.list"))
    }

    @Test
    fun `canMakeRequest returns false when quota exceeded`() {
        // Simulate at quota limit (9900 used, 100 remaining)
        repeat(99) { QuotaManager.trackUsage("search.list") }
        // Can still make 100 cost request (search.list = 100)
        assertTrue(QuotaManager.canMakeRequest("search.list"))
        // After this request, quota would be at 10000
        QuotaManager.trackUsage("search.list")
        // Now cannot make another request
        assertFalse(QuotaManager.canMakeRequest("search.list"))
    }

    @Test
    fun `isNearQuotaLimit returns true when above threshold`() {
        // Below threshold
        repeat(79) { QuotaManager.trackUsage("search.list") }
        assertFalse(QuotaManager.isNearQuotaLimit())

        // Above threshold
        QuotaManager.trackUsage("search.list")
        assertTrue(QuotaManager.isNearQuotaLimit())
    }

    @Test
    fun `getRemainingQuota returns correct value`() {
        assertEquals(10000, QuotaManager.getRemainingQuota())

        QuotaManager.trackUsage("search.list")
        assertEquals(9900, QuotaManager.getRemainingQuota())
    }

    @Test
    fun `getQuotaPercentage returns correct value`() {
        assertEquals(0.0f, QuotaManager.getQuotaPercentage(), 0.01f)

        repeat(100) { QuotaManager.trackUsage("search.list") }
        assertEquals(100.0f, QuotaManager.getQuotaPercentage(), 0.01f)
    }

    @Test
    fun `resetQuota resets to zero`() {
        QuotaManager.trackUsage("search.list")
        assertEquals(100, QuotaManager.getUsedQuota())

        QuotaManager.resetQuota()
        assertEquals(0, QuotaManager.getUsedQuota())
    }
}
