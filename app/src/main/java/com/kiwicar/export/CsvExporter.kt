package com.kiwicar.export

import com.kiwicar.data.Entry
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** Pure CSV backup format: `tipo;data;km;litri`, ISO dates, Italian decimal comma, `\n` line endings. */
object CsvExporter {
    const val HEADER = "tipo;data;km;litri"

    fun toCsv(entries: List<Entry>): String {
        val liters = NumberFormat.getNumberInstance(Locale.ITALY).apply {
            isGroupingUsed = false
            maximumFractionDigits = 3
        }
        return buildString {
            append(HEADER).append('\n')
            entries
                .sortedWith(compareBy<Entry> { it.dateEpochDay }.thenBy { it.odometerKm }.thenBy { it.id })
                .forEach {
                    append(it.type.name).append(';')
                        .append(LocalDate.ofEpochDay(it.dateEpochDay)).append(';')
                        .append(it.odometerKm).append(';')
                        .append(liters.format(it.liters)).append('\n')
                }
        }
    }
}
