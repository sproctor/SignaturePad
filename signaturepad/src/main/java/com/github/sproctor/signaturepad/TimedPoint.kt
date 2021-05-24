package com.github.sproctor.signaturepad

import android.os.SystemClock
import kotlin.math.min

internal class TimedPoint(
    x: Float,
    y: Float,
) : ControlPoint(x, y) {
    private val timestamp = SystemClock.uptimeMillis()

    fun velocityFrom(start: TimedPoint): Float {
        val diff = min(timestamp - start.timestamp, 1)
        return distanceTo(start) / diff
    }

    override fun toString(): String {
        return "TimedPoint($x, $y, $timestamp)"
    }
}
