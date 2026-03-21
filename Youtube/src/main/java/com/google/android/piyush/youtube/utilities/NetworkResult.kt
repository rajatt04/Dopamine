package com.google.android.piyush.youtube.utilities

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int? = null, val message: String, val exception: Exception? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}

object ErrorMapper {
    fun mapError(code: Int?, message: String?): String {
        return when (code) {
            400 -> "Bad request. Please try again."
            401 -> "Authentication failed. Please sign in again."
            403 -> when {
                message?.contains("quota", ignoreCase = true) == true ->
                    "API quota exceeded. Please try again later."
                message?.contains("commentsDisabled", ignoreCase = true) == true ->
                    "Comments are disabled for this video."
                else -> "Access denied. You don't have permission for this action."
            }
            404 -> "Content not found. It may have been removed."
            409 -> "Conflict. This action was already performed."
            413 -> "Request too large."
            429 -> "Too many requests. Please wait and try again."
            500 -> "YouTube server error. Please try again later."
            503 -> "YouTube service unavailable. Please try again later."
            else -> message ?: "An unexpected error occurred."
        }
    }

    fun getUserMessage(exception: Exception?): String {
        val message = exception?.message ?: return "An unexpected error occurred."
        return when {
            message.contains("Unable to resolve host") -> "No internet connection. Please check your network."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("SSL", ignoreCase = true) -> "Secure connection failed. Please try again."
            else -> message
        }
    }
}
