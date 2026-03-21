package com.google.android.piyush.dopamine.utilities

data class Chapter(
    val title: String,
    val startTimeSeconds: Int,
    val formattedTime: String
)

object ChapterParser {

    private val TIMESTAMP_PATTERN = Regex("""^(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*[-–—]?\s*(.+)""")

    fun parseChapters(description: String?): List<Chapter> {
        if (description.isNullOrBlank()) return emptyList()

        val chapters = mutableListOf<Chapter>()
        val lines = description.lines()

        for (line in lines) {
            val trimmed = line.trim()
            val match = TIMESTAMP_PATTERN.find(trimmed) ?: continue

            val groups = match.groupValues
            val hours = groups[1].let { if (it.isEmpty()) 0 else it.toIntOrNull() ?: 0 }
            val minutes = groups[2].toIntOrNull() ?: continue
            val seconds = groups[3].toIntOrNull() ?: continue
            val title = groups[4].trim()

            if (title.isBlank()) continue

            val totalSeconds = hours * 3600 + minutes * 60 + seconds
            val formattedTime = if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }

            chapters.add(
                Chapter(
                    title = title,
                    startTimeSeconds = totalSeconds,
                    formattedTime = formattedTime
                )
            )
        }

        return chapters.distinctBy { it.startTimeSeconds }.sortedBy { it.startTimeSeconds }
    }

    fun formatDuration(iso8601Duration: String?): String {
        if (iso8601Duration.isNullOrBlank()) return "0:00"

        val duration = iso8601Duration.removePrefix("PT")
        var hours = 0
        var minutes = 0
        var seconds = 0

        val hourMatch = Regex("""(\d+)H""").find(duration)
        val minuteMatch = Regex("""(\d+)M""").find(duration)
        val secondMatch = Regex("""(\d+)S""").find(duration)

        hourMatch?.let { hours = it.groupValues[1].toIntOrNull() ?: 0 }
        minuteMatch?.let { minutes = it.groupValues[1].toIntOrNull() ?: 0 }
        secondMatch?.let { seconds = it.groupValues[1].toIntOrNull() ?: 0 }

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun formatViewCount(count: Int?): String {
        if (count == null) return "0"
        return when {
            count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
            count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
            else -> count.toString()
        }
    }

    fun getRelativeTime(iso8601Date: String?): String {
        if (iso8601Date.isNullOrBlank()) return ""
        return try {
            val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
            val publishDate = java.time.ZonedDateTime.parse(iso8601Date, formatter)
            val now = java.time.ZonedDateTime.now(publishDate.zone)
            val duration = java.time.Duration.between(publishDate, now)

            when {
                duration.toDays() > 365 -> "${duration.toDays() / 365} year${if (duration.toDays() / 365 > 1) "s" else ""} ago"
                duration.toDays() > 30 -> "${duration.toDays() / 30} month${if (duration.toDays() / 30 > 1) "s" else ""} ago"
                duration.toDays() > 0 -> "${duration.toDays()} day${if (duration.toDays() > 1) "s" else ""} ago"
                duration.toHours() > 0 -> "${duration.toHours()} hour${if (duration.toHours() > 1) "s" else ""} ago"
                duration.toMinutes() > 0 -> "${duration.toMinutes()} minute${if (duration.toMinutes() > 1) "s" else ""} ago"
                else -> "Just now"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
