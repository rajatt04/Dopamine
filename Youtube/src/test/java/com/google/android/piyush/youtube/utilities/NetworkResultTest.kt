package com.google.android.piyush.youtube.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `Success wraps data correctly`() {
        val data = "test data"
        val result = NetworkResult.Success(data)

        assertTrue(result is NetworkResult.Success)
        assertEquals(data, result.data)
    }

    @Test
    fun `Error contains message and optional exception`() {
        val exception = RuntimeException("test error")
        val result = NetworkResult.Error(code = 403, message = "Forbidden", exception = exception)

        assertTrue(result is NetworkResult.Error)
        assertEquals(403, result.code)
        assertEquals("Forbidden", result.message)
        assertNotNull(result.exception)
    }

    @Test
    fun `Error works without exception`() {
        val result = NetworkResult.Error(message = "Something went wrong")

        assertTrue(result is NetworkResult.Error)
        assertEquals("Something went wrong", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `Loading is singleton`() {
        val loading1 = NetworkResult.Loading
        val loading2 = NetworkResult.Loading
        assertTrue(loading1 === loading2)
    }
}

class ErrorMapperTest {

    @Test
    fun `mapError returns correct message for 400`() {
        val message = ErrorMapper.mapError(400, null)
        assertEquals("Bad request. Please try again.", message)
    }

    @Test
    fun `mapError returns correct message for 401`() {
        val message = ErrorMapper.mapError(401, null)
        assertEquals("Authentication failed. Please sign in again.", message)
    }

    @Test
    fun `mapError returns quota message for 403 with quota`() {
        val message = ErrorMapper.mapError(403, "quotaExceeded")
        assertEquals("API quota exceeded. Please try again later.", message)
    }

    @Test
    fun `mapError returns comments disabled message for 403`() {
        val message = ErrorMapper.mapError(403, "commentsDisabled")
        assertEquals("Comments are disabled for this video.", message)
    }

    @Test
    fun `mapError returns not found for 404`() {
        val message = ErrorMapper.mapError(404, null)
        assertEquals("Content not found. It may have been removed.", message)
    }

    @Test
    fun `mapError returns rate limit for 429`() {
        val message = ErrorMapper.mapError(429, null)
        assertEquals("Too many requests. Please wait and try again.", message)
    }

    @Test
    fun `mapError returns generic message for unknown codes`() {
        val message = ErrorMapper.mapError(999, "Custom error")
        assertEquals("Custom error", message)
    }

    @Test
    fun `getUserMessage returns network error for host resolution`() {
        val exception = RuntimeException("Unable to resolve host googleapis.com")
        val message = ErrorMapper.getUserMessage(exception)
        assertEquals("No internet connection. Please check your network.", message)
    }

    @Test
    fun `getUserMessage returns timeout message`() {
        val exception = RuntimeException("Connection timeout")
        val message = ErrorMapper.getUserMessage(exception)
        assertEquals("Request timed out. Please try again.", message)
    }

    @Test
    fun `getUserMessage returns generic for null exception`() {
        val message = ErrorMapper.getUserMessage(null)
        assertEquals("An unexpected error occurred.", message)
    }
}
