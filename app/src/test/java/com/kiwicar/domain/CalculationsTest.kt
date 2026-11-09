package com.kiwicar.domain

import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.EntryType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculationsTest {
    private fun e(id: Long, t: EntryType, km: Int, l: Double, day: Long = 0) = Entry(id, t, day, km, l)

    @Test fun fuel_consumption_is_liters_over_distance_times_100() {
        val r = Calculations.fuelConsumptionLper100(e(2, BENZINA, 181560, 40.1), e(1, BENZINA, 181000, 40.0))
        assertEquals(7.16, r!!, 0.01)
    }

    @Test fun first_fuel_entry_has_no_consumption() {
        assertNull(Calculations.fuelConsumptionLper100(e(1, BENZINA, 181000, 40.0), null))
    }

    @Test fun equal_odometer_gives_null_not_infinity() {
        assertNull(Calculations.fuelConsumptionLper100(e(2, BENZINA, 181000, 40.0), e(1, BENZINA, 181000, 40.0)))
    }

    @Test fun negative_distance_gives_null() {
        assertNull(Calculations.fuelConsumptionLper100(e(2, BENZINA, 180000, 40.0), e(1, BENZINA, 181000, 40.0)))
    }

    @Test fun previous_is_greatest_km_below_same_type() {
        val all = listOf(e(1, BENZINA, 1000, 40.0), e(2, OLIO, 1200, 0.5), e(3, BENZINA, 1500, 38.0))
        assertEquals(1L, Calculations.previousOfSameType(all[2], all)!!.id)
    }

    @Test fun previous_uses_km_order_not_insertion_order() {
        val all = listOf(e(1, BENZINA, 1500, 38.0), e(2, BENZINA, 1000, 40.0)) // inseriti fuori ordine
        assertEquals(2L, Calculations.previousOfSameType(all[0], all)!!.id)
    }

    @Test fun oil_consumption_is_liters_over_distance_times_1000() {
        assertEquals(0.5, Calculations.oilConsumptionLper1000(e(2, OLIO, 181000, 0.5), e(1, OLIO, 180000, 1.0))!!, 0.001)
    }

    @Test fun estimated_cost_is_liters_times_price() {
        assertEquals(68.97, Calculations.estimatedCost(40.1, 1.72), 0.001)
    }

    @Test fun average_monthly_empty_history_is_zero() {
        assertEquals(0.0, Calculations.averageMonthlyFuelCost(emptyList(), 1.72, LocalDate.of(2026, 11, 9)), 0.0)
    }

    @Test fun average_monthly_divides_total_by_months_elapsed() {
        // due pieni da 40 L a 1,00 €/L in due mesi distinti -> totale 80 €, mesi = 2 -> 40 €/mese
        val a = e(1, BENZINA, 1000, 40.0, LocalDate.of(2026, 10, 1).toEpochDay())
        val b = e(2, BENZINA, 1500, 40.0, LocalDate.of(2026, 11, 1).toEpochDay())
        assertEquals(40.0, Calculations.averageMonthlyFuelCost(listOf(a, b), 1.0, LocalDate.of(2026, 11, 9)), 0.01)
    }

    @Test fun average_yearly_is_monthly_times_twelve() {
        val a = e(1, BENZINA, 1000, 40.0, LocalDate.of(2026, 10, 1).toEpochDay())
        val b = e(2, BENZINA, 1500, 40.0, LocalDate.of(2026, 11, 1).toEpochDay())
        assertEquals(480.0, Calculations.averageYearlyFuelCost(listOf(a, b), 1.0, LocalDate.of(2026, 11, 9)), 0.01)
    }
}
