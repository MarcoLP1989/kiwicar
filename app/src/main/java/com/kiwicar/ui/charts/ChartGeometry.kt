package com.kiwicar.ui.charts

import androidx.compose.ui.geometry.Offset

/** Plot rectangle in pixels: x from [left] to [left]+[width], y from 0 (top) to [height] (baseline). */
data class PlotArea(val left: Float, val width: Float, val height: Float)

/** Maps a point to canvas coordinates; safe for zero ranges (no NaN). */
internal fun pointOffset(p: ChartPoint, area: PlotArea, xFrom: Long, xTo: Long, yMax: Double): Offset {
    val xRange = xTo - xFrom
    val fx = if (xRange <= 0) 0.5f else ((p.epochDay - xFrom).toFloat() / xRange).coerceIn(0f, 1f)
    val fy = if (yMax <= 0.0) 0f else (p.value / yMax).toFloat().coerceIn(0f, 1f)
    return Offset(area.left + area.width * fx, area.height - area.height * fy)
}

/**
 * Returns the tappable point hit by [tap]. Bars are hit anywhere inside their rectangle (half width
 * [barHalfWidth] plus [slack]); line points are hit when the nearest one is within [radius].
 */
internal fun hitTest(
    points: List<ChartPoint>,
    tap: Offset,
    area: PlotArea,
    xFrom: Long,
    xTo: Long,
    yMax: Double,
    kind: ChartKind,
    radius: Float,
    barHalfWidth: Float,
    slack: Float = 4f,
): ChartPoint? {
    val tappable = points.filter { it.source != null }
    return when (kind) {
        ChartKind.BAR -> tappable
            .filter {
                val o = pointOffset(it, area, xFrom, xTo, yMax)
                kotlin.math.abs(tap.x - o.x) <= barHalfWidth + slack && tap.y >= o.y - slack && tap.y <= area.height + slack
            }
            .minByOrNull { kotlin.math.abs(tap.x - pointOffset(it, area, xFrom, xTo, yMax).x) }
        ChartKind.LINE -> tappable
            .minByOrNull { (pointOffset(it, area, xFrom, xTo, yMax) - tap).getDistance() }
            ?.takeIf { (pointOffset(it, area, xFrom, xTo, yMax) - tap).getDistance() <= radius }
    }
}
