package com.jantursky.proctor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StatsTest {
    @Test
    fun percentileOfNoValuesIsNull() {
        assertNull(emptyList<Long>().nearestRankPercentile(50.0))
    }

    @Test
    fun percentileOfOneValueIsThatValue() {
        assertEquals(7L, listOf(7L).nearestRankPercentile(0.0))
        assertEquals(7L, listOf(7L).nearestRankPercentile(50.0))
        assertEquals(7L, listOf(7L).nearestRankPercentile(100.0))
    }

    @Test
    fun nearestRankPicksAValueFromTheListNeverAnInterpolatedOne() {
        val latencies = listOf(100L, 200L, 300L, 400L)
        assertEquals(200L, latencies.nearestRankPercentile(50.0))
        assertEquals(400L, latencies.nearestRankPercentile(95.0))
        assertEquals(400L, latencies.nearestRankPercentile(100.0))
        assertEquals(100L, latencies.nearestRankPercentile(0.0))
    }

    @Test
    fun percentileHandlesUnsortedInputAndTies() {
        val latencies = listOf(500L, 100L, 100L, 100L, 900L)
        assertEquals(100L, latencies.nearestRankPercentile(50.0))
        assertEquals(900L, latencies.nearestRankPercentile(95.0))
        assertEquals(100L, listOf(300L, 300L, 100L).nearestRankPercentile(20.0))
    }

    @Test
    fun percentileRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { listOf(1L).nearestRankPercentile(101.0) }
        assertFailsWith<IllegalArgumentException> { listOf(1L).nearestRankPercentile(-1.0) }
    }
}
