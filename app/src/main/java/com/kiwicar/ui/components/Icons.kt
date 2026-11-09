package com.kiwicar.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Minimal line icons: stroke-only 24x24 vectors, tinted by the consumer via Icon(tint). */
object KiwiIcons {
    private fun line(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Home = line("Home", "M3 11 L12 3 L21 11", "M5 9.5 V20 H19 V9.5")
    val Chart = line("Chart", "M4 4 V20 H20", "M8 16 V11", "M12 16 V7", "M16 16 V13")
    val History = line("History", "M12 3 A9 9 0 1 1 11.99 3 Z", "M12 7 V12 L15 14")
    val Settings = line("Settings", "M4 7 H20", "M4 17 H20", "M9 4 V10", "M15 14 V20")
    val Pump = line(
        "Pump",
        "M5 20 V5 A1 1 0 0 1 6 4 H12 A1 1 0 0 1 13 5 V20",
        "M4 20 H14",
        "M5 10 H13",
        "M13 8 H15.5 A1.5 1.5 0 0 1 17 9.5 V15 A1.5 1.5 0 0 0 20 15 V8 L18 6",
    )
    val Drop = line("Drop", "M12 3 C12 3 5 11 5 15 A7 7 0 0 0 19 15 C19 11 12 3 12 3 Z")
    val Euro = line("Euro", "M17 6.5 A7 7 0 1 0 17 17.5", "M4 10 H13", "M4 14 H13")
    val Download = line("Download", "M12 4 V15", "M7 10.5 L12 15.5 L17 10.5", "M5 20 H19")
}
