package com.seanproctor.signaturepad

import kotlin.math.max

internal class TimedPoint(
    x: Float,
    y: Float,
    private val uptimeMillis: Long,
) : ControlPoint(x, y) {

    fun velocityFrom(start: TimedPoint): Float {
        val diff = max(uptimeMillis - start.uptimeMillis, 1)
        return distanceTo(start) / diff.toFloat()
    }

    override fun toString(): String {
        return "TimedPoint($x, $y, $uptimeMillis)"
    }
}
