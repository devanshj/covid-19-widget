package me.devanshj.covid19widget

import android.graphics.Path
import android.graphics.PointF

fun cardinalCurve(points: List<PointF>): Path {
    val p = Path()
    val cardinal = Cardinal(p, 0f)
    cardinal.lineStart()
    points.forEach { pt -> cardinal.point(pt.x, pt.y) }
    cardinal.lineEnd()
    return p
}


// ported from
// https://github.com/d3/d3-shape/blob/master/src/curve/cardinal.js

private class Cardinal(val path: Path, tension: Float) {
    private var k = (1 - tension) / 6
    private var x0: Float = 0f
    private var y0: Float = 0f
    private var x1: Float = 0f
    private var y1: Float = 0f
    private var x2: Float = 0f
    private var y2: Float = 0f
    private var pointIndex: Int = 0

    private fun _point(x: Float, y: Float) {
        path.cubicTo(
            x1 + k * (x2 - x0),
            y1 + k * (y2 - y0),
            x2 + k * (x1 - x),
            y2 + k * (y1 - y),
            x2,
            y2
        )
    }

    fun lineStart() {
        this.pointIndex = 0
    }

    fun lineEnd() {
        when (pointIndex) {
            2 -> path.lineTo(x2, y2)
            3 -> _point(x1, y1)
        }
    }

    fun point(x: Float, y: Float) {
        when (pointIndex) {
            0 -> {
                pointIndex = 1
                path.moveTo(x, y)
            }
            1 -> {
                pointIndex = 2
                x1 = x
                y1 = y
            }
            2 -> pointIndex = 3
            else -> _point(x, y)
        }
        x0 = x1
        x1 = x2
        x2 = x
        y0 = y1
        y1 = y2
        y2 = y
    }
}