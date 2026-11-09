package com.kiwicar.export

import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {
    private fun day(s: String) = LocalDate.parse(s).toEpochDay()

    @Test
    fun emptyListProducesHeaderOnly() {
        assertEquals("tipo;data;km;litri\n", CsvExporter.toCsv(emptyList()))
    }

    @Test
    fun rowsInFieldOrderWithItalianDecimalsAndIsoDates() {
        val csv = CsvExporter.toCsv(
            listOf(
                Entry(1, EntryType.BENZINA, day("2026-03-05"), 12345, 40.1),
                Entry(2, EntryType.OLIO, day("2026-03-06"), 12400, 0.5),
            ),
        )
        assertEquals(
            "tipo;data;km;litri\nBENZINA;2026-03-05;12345;40,1\nOLIO;2026-03-06;12400;0,5\n",
            csv,
        )
    }

    @Test
    fun rowsOrderedByDateThenKm() {
        val csv = CsvExporter.toCsv(
            listOf(
                Entry(1, EntryType.BENZINA, day("2026-03-06"), 500, 30.0),
                Entry(2, EntryType.BENZINA, day("2026-03-05"), 900, 31.0),
                Entry(3, EntryType.OLIO, day("2026-03-05"), 800, 1.0),
            ),
        )
        assertEquals(
            listOf("tipo;data;km;litri", "OLIO;2026-03-05;800;1", "BENZINA;2026-03-05;900;31", "BENZINA;2026-03-06;500;30"),
            csv.trim().lines(),
        )
    }
}
