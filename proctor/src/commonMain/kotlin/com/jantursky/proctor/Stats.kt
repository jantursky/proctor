package com.jantursky.proctor

import kotlin.math.ceil
import kotlin.math.sqrt

internal fun List<Double>.meanOrNull(): Double? = if (isEmpty()) null else sum() / size

/**
 * Nearest-rank percentile: the smallest value at or above which [percent] percent of the values lie.
 * Always returns a value that is in the list, never an interpolated one. `null` for no values.
 *
 * @throws IllegalArgumentException if [percent] is outside `0.0..100.0`.
 */
public fun List<Long>.nearestRankPercentile(percent: Double): Long? {
    require(percent in 0.0..100.0) { "percent must be in 0.0..100.0, got $percent" }
    if (isEmpty()) return null
    val sorted = sorted()
    val rank = ceil(percent / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
    return sorted[rank - 1]
}

/** Population standard deviation, `null` for no values. */
internal fun List<Double>.spreadOrNull(): Double? {
    val mean = meanOrNull() ?: return null
    return sqrt(sumOf { (it - mean) * (it - mean) } / size)
}
