package com.google.android.piyush.dopamine.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterParserTest {

    @Test
    fun `parseChapters extracts timestamps from description`() {
        val description = """
            Welcome to this video!
            0:00 Introduction
            1:23 Getting Started
            5:45 Advanced Topics
            12:30 Conclusion
            Thanks for watching!
        """.trimIndent()

        val chapters = ChapterParser.parseChapters(description)

        assertEquals(4, chapters.size)
        assertEquals("Introduction", chapters[0].title)
        assertEquals(0, chapters[0].startTimeSeconds)
        assertEquals("0:00", chapters[0].formattedTime)
        assertEquals("Getting Started", chapters[1].title)
        assertEquals(83, chapters[1].startTimeSeconds)
        assertEquals("1:23", chapters[1].formattedTime)
    }

    @Test
    fun `parseChapters handles hours format`() {
        val description = """
            0:00 Start
            1:02:30 One hour mark
            2:15:45 End
        """.trimIndent()

        val chapters = ChapterParser.parseChapters(description)

        assertEquals(3, chapters.size)
        assertEquals("1:02:30", chapters[1].formattedTime)
        assertEquals(3750, chapters[1].startTimeSeconds)
    }

    @Test
    fun `parseChapters returns empty for null description`() {
        val chapters = ChapterParser.parseChapters(null)
        assertTrue(chapters.isEmpty())
    }

    @Test
    fun `parseChapters returns empty for blank description`() {
        val chapters = ChapterParser.parseChapters("")
        assertTrue(chapters.isEmpty())
    }

    @Test
    fun `parseChapters ignores invalid timestamps`() {
        val description = """
            This is not a timestamp: 99:99
            0:00 Valid chapter
            Random text
            1:23 Another valid chapter
        """.trimIndent()

        val chapters = ChapterParser.parseChapters(description)
        assertEquals(2, chapters.size)
    }

    @Test
    fun `formatDuration converts PT format correctly`() {
        assertEquals("1:30", ChapterParser.formatDuration("PT1M30S"))
        assertEquals("1:02:03", ChapterParser.formatDuration("PT1H2M3S"))
        assertEquals("0:45", ChapterParser.formatDuration("PT45S"))
        assertEquals("10:00", ChapterParser.formatDuration("PT10M"))
    }

    @Test
    fun `formatDuration handles null and blank`() {
        assertEquals("0:00", ChapterParser.formatDuration(null))
        assertEquals("0:00", ChapterParser.formatDuration(""))
    }

    @Test
    fun `formatViewCount formats correctly`() {
        assertEquals("1.2M", ChapterParser.formatViewCount(1234567))
        assertEquals("1.5K", ChapterParser.formatViewCount(1500))
        assertEquals("500", ChapterParser.formatViewCount(500))
        assertEquals("0", ChapterParser.formatViewCount(0))
        assertEquals("0", ChapterParser.formatViewCount(null))
    }

    @Test
    fun `parseChapters sorts chapters by time`() {
        val description = """
            5:00 Middle
            0:00 Start
            10:00 End
        """.trimIndent()

        val chapters = ChapterParser.parseChapters(description)

        assertEquals("Start", chapters[0].title)
        assertEquals("Middle", chapters[1].title)
        assertEquals("End", chapters[2].title)
    }

    @Test
    fun `parseChapters removes duplicates`() {
        val description = """
            0:00 Start
            0:00 Duplicate Start
            1:00 End
        """.trimIndent()

        val chapters = ChapterParser.parseChapters(description)
        assertEquals(2, chapters.size)
    }
}
