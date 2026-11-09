package com.kiwicar.domain

import com.kiwicar.data.Entry
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object Calculations {
    fun previousOfSameType(entry: Entry, all: List<Entry>): Entry? =
        all.filter { it.type == entry.type && it.odometerKm < entry.odometerKm }
            .maxByOrNull { it.odometerKm }

    fun fuelConsumptionLper100(entry: Entry, previous: Entry?): Double? =
        consumptionPer(entry, previous, 100)

    fun oilConsumptionLper1000(entry: Entry, previous: Entry?): Double? =
        consumptionPer(entry, previous, 1000)

    // Rounded to cents: it is a currency amount.
    fun estimatedCost(liters: Double, pricePerLiter: Double): Double =
        Math.round(liters * pricePerLiter * 100) / 100.0

    fun averageMonthlyFuelCost(fuelEntries: List<Entry>, pricePerLiter: Double, today: LocalDate): Double {
        if (fuelEntries.isEmpty()) return 0.0
        val total = fuelEntries.sumOf { estimatedCost(it.liters, pricePerLiter) }
        val firstMonth = YearMonth.from(LocalDate.ofEpochDay(fuelEntries.minOf { it.dateEpochDay }))
        val months = maxOf(1L, ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(today)) + 1)
        return total / months
    }

    fun averageYearlyFuelCost(fuelEntries: List<Entry>, pricePerLiter: Double, today: LocalDate): Double =
        averageMonthlyFuelCost(fuelEntries, pricePerLiter, today) * 12

    private fun consumptionPer(entry: Entry, previous: Entry?, perKm: Int): Double? {
        if (previous == null) return null
        val distance = entry.odometerKm - previous.odometerKm
        if (distance <= 0) return null
        return entry.liters / distance * perKm
    }
}
