package com.kiwicar.data

import com.kiwicar.domain.Calculations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** [consumption] is L/100km for BENZINA and L/1000km for OLIO; [distanceFromPreviousKm] is vs. the previous entry of the same type. */
data class EntryWithStats(
    val entry: Entry,
    val consumption: Double?,
    val estimatedCost: Double,
    val distanceFromPreviousKm: Int?,
)

data class HomeStats(
    val fuelLper100: Double?,
    val oilLper1000: Double?,
    val avgMonthlyCost: Double,
    val avgYearlyCost: Double,
)

class KiwiRepository(private val dao: KiwiDao, private val settings: SettingsStore) {

    fun entriesWithStats(): Flow<List<EntryWithStats>> =
        combine(dao.observeAll(), settings.settings) { entries, s -> computeStats(entries, s) }

    fun homeStats(today: LocalDate): Flow<HomeStats> =
        combine(dao.observeAll(), settings.settings) { entries, s ->
            val stats = computeStats(entries, s)
            fun latestConsumption(type: EntryType) = stats
                .filter { it.entry.type == type && it.consumption != null }
                .maxByOrNull { it.entry.odometerKm }
                ?.consumption
            val fuelEntries = entries.filter { it.type == EntryType.BENZINA }
            HomeStats(
                fuelLper100 = latestConsumption(EntryType.BENZINA),
                oilLper1000 = latestConsumption(EntryType.OLIO),
                avgMonthlyCost = Calculations.averageMonthlyFuelCost(fuelEntries, s.fuelPricePerLiter, today),
                avgYearlyCost = Calculations.averageYearlyFuelCost(fuelEntries, s.fuelPricePerLiter, today),
            )
        }

    suspend fun save(entry: Entry) {
        dao.upsert(entry)
    }

    suspend fun delete(entry: Entry) = dao.delete(entry)

    private fun computeStats(entries: List<Entry>, s: KiwiSettings): List<EntryWithStats> =
        entries
            .sortedWith(compareBy<Entry> { it.odometerKm }.thenBy { it.dateEpochDay }.thenBy { it.id })
            .map { e ->
                val prev = Calculations.previousOfSameType(e, entries)
                val (consumption, price) = when (e.type) {
                    EntryType.BENZINA -> Calculations.fuelConsumptionLper100(e, prev) to s.fuelPricePerLiter
                    EntryType.OLIO -> Calculations.oilConsumptionLper1000(e, prev) to s.oilPricePerLiter
                }
                EntryWithStats(
                    entry = e,
                    consumption = consumption,
                    estimatedCost = Calculations.estimatedCost(e.liters, price),
                    distanceFromPreviousKm = prev?.let { e.odometerKm - it.odometerKm },
                )
            }
}
