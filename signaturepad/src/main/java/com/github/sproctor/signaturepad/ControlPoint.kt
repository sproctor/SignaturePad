package com.github.sproctor.signaturepad

import kotlin.math.sqrt

internal open class ControlPoint(
    val x: Float,
    val y: Float,
) {
    fun distanceTo(point: ControlPoint): Float {
        val diffX = point.x - x
        val diffY = point.y - y
        return sqrt(diffX * diffX + diffY * diffY)
    }
}
