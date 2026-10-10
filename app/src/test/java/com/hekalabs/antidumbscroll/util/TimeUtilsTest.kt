package com.hekalabs.antidumbscroll.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `formatTime with zero ms returns 00_00`() {
        val result = TimeUtils.formatTime(0L)
        assertEquals("00:00", result)
    }

    @Test
    fun `formatTime rounds up remaining ms to nearest second`() {
        val result1 = TimeUtils.formatTime(1L)
        assertEquals("00:01", result1)
        
        val result2 = TimeUtils.formatTime(999L)
        assertEquals("00:01", result2)
        
        val result3 = TimeUtils.formatTime(1000L)
        assertEquals("00:01", result3)

        val result4 = TimeUtils.formatTime(1001L)
        assertEquals("00:02", result4)
    }

    @Test
    fun `formatTime with exactly 1 minute returns 01_00`() {
        // 60 seconds = 60,000 ms
        val result = TimeUtils.formatTime(60_000L)
        assertEquals("01:00", result)
    }

    @Test
    fun `formatTime with 25 minutes returns 25_00`() {
        // 25 * 60 * 1000 = 1,500,000 ms
        val result = TimeUtils.formatTime(1_500_000L)
        assertEquals("25:00", result)
    }

    @Test
    fun `formatTime formats single digit minutes and seconds with leading zero`() {
        // 5 minutes, 9 seconds = 309,000 ms
        val result = TimeUtils.formatTime(309_000L)
        assertEquals("05:09", result)
    }
}
