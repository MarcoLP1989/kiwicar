package com.kiwicar.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ChartKind { LINE, BAR }

private const val Y_TICKS = 3
private const val X_LABELS = 4
private val TOUCH_RADIUS = 24.dp
private val LEFT_MARGIN = 44.dp
private val BOTTOM_MARGIN = 22.dp
private val BAR_WIDTH = 14.dp

private fun formatTick(v: Double): String =
    NumberFormat.getNumberInstance(Locale.ITALY).apply {
        maximumFractionDigits = if (v >= 10) 0 else 1
        minimumFractionDigits = 0
    }.format(v)

private fun axisDate(epochDay: Long, window: ChartWindow): String {
    val pattern = if (window == ChartWindow.MESE) "dd/MM" else "MMM yy"
    return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(pattern, Locale.ITALY))
}

/**
 * Generic line/bar chart on a Compose Canvas. X axis spans [window] ending at [today]; Y axis starts at zero.
 * Points with a non-null [ChartPoint.source] are tappable via [onPointTap].
 */
@Composable
fun KiwiCarChart(
    title: String,
    series: ChartSeries,
    kind: ChartKind,
    color: Color,
    window: ChartWindow,
    today: LocalDate,
    onPointTap: (ChartPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(title.uppercase(Locale.ITALY), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(series.unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (series.points.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("Nessun dato nel periodo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            ChartCanvas(series, kind, color, window, today, onPointTap)
        }
    }
}

@Composable
private fun ChartCanvas(
    series: ChartSeries,
    kind: ChartKind,
    color: Color,
    window: ChartWindow,
    today: LocalDate,
    onPointTap: (ChartPoint) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = remember(labelColor) { TextStyle(color = labelColor, fontSize = 10.sp) }
    val xTo = today.toEpochDay()
    val xFrom = xTo - window.days
    val maxValue = series.points.maxOf { it.value }
    val yMax = if (maxValue <= 0.0) 1.0 else maxValue * 1.1

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
            .pointerInput(series, window, today) {
                detectTapGestures { tap ->
                    val area = PlotArea(LEFT_MARGIN.toPx(), size.width - LEFT_MARGIN.toPx(), size.height - BOTTOM_MARGIN.toPx())
                    hitTest(series.points, tap, area, xFrom, xTo, yMax, kind, TOUCH_RADIUS.toPx(), BAR_WIDTH.toPx() / 2)
                        ?.let(onPointTap)
                }
            },
    ) {
        val left = LEFT_MARGIN.toPx()
        val w = size.width - left
        val h = size.height - BOTTOM_MARGIN.toPx()

        for (i in 0..Y_TICKS) {
            val v = yMax * i / Y_TICKS
            val y = h - h * (i.toFloat() / Y_TICKS)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1f)
            val label = measurer.measure(formatTick(v), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
        }
        for (i in 0 until X_LABELS) {
            val day = xFrom + (xTo - xFrom) * i / (X_LABELS - 1)
            val label = measurer.measure(axisDate(day, window), labelStyle)
            val x = left + w * i.toFloat() / (X_LABELS - 1)
            val tx = (x - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
            drawText(label, topLeft = Offset(tx, h + 4.dp.toPx()))
        }

        val offsets = series.points.map { pointOffset(it, PlotArea(left, w, h), xFrom, xTo, yMax) }
        when (kind) {
            ChartKind.LINE -> {
                offsets.zipWithNext { a, b -> drawLine(color, a, b, strokeWidth = 2.dp.toPx()) }
                offsets.forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
            }
            ChartKind.BAR -> {
                val barW = BAR_WIDTH.toPx()
                offsets.forEach { drawRect(color, Offset(it.x - barW / 2, it.y), Size(barW, h - it.y)) }
            }
        }
        drawRect(gridColor, Offset(left, 0f), Size(w, h), style = Stroke(1f))
    }
}
