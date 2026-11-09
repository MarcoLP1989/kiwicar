package com.kiwicar.ui

import com.kiwicar.data.EntryType
import java.text.NumberFormat
import java.util.Locale

private fun decimals(n: Int) = NumberFormat.getNumberInstance(Locale.ITALY).apply {
    minimumFractionDigits = n
    maximumFractionDigits = n
}

const val EMPTY_VALUE = "—"

/** Consumption with one decimal (e.g. `6,8`), or `—` when unknown. */
fun formatConsumption(value: Double?): String = value?.let { decimals(1).format(it) } ?: EMPTY_VALUE

/** Consumption with its unit (`L/100 km` for fuel, `L/1.000 km` for oil), or `—` when unknown. */
fun formatConsumption(value: Double?, type: EntryType): String =
    value?.let { formatConsumption(it) + " " + if (type == EntryType.BENZINA) "L/100 km" else "L/1.000 km" } ?: EMPTY_VALUE

/** Litres with 1 to 2 decimals and unit (e.g. `40,5 L`). */
fun formatLiters(liters: Double): String =
    NumberFormat.getNumberInstance(Locale.ITALY).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 2
    }.format(liters) + " L"

/** Money with two decimals (e.g. `1.234,50 €`). */
fun formatMoney(value: Double): String = decimals(2).format(value) + " €"

/** Kilometres with thousands separator (e.g. `181.560 km`). */
fun formatKm(km: Int): String = NumberFormat.getIntegerInstance(Locale.ITALY).format(km) + " km"

/** Parses user input with comma or dot as decimal separator (e.g. `40,5`); null when blank or invalid. */
fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Dd/MM/yyyy (e.g. `09/11/2026`). */
fun formatDate(epochDay: Long): String =
    java.time.LocalDate.ofEpochDay(epochDay)
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY))
