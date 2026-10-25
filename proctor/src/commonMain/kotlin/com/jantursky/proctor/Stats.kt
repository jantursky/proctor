package com.jantursky.proctor

import kotlin.math.sqrt

internal fun List<Double>.meanOrNull(): Double? = if (isEmpty()) null else sum() / size

/** Population standard deviation, `null` for no values. */
internal fun List<Double>.spreadOrNull(): Double? {
    val mean = meanOrNull() ?: return null
    return sqrt(sumOf { (it - mean) * (it - mean) } / size)
}
