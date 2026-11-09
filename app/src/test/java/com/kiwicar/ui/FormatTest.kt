package com.kiwicar.ui

import com.kiwicar.data.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun consumptionUsesOneDecimalAndCommaOrDashForNull() {
        assertEquals("6,8", formatConsumption(6.8))
        assertEquals("6,0", formatConsumption(6.0))
        assertEquals("—", formatConsumption(null))
    }

    @Test
    fun consumptionWithUnitDependsOnType() {
        assertEquals("6,8 L/100 km", formatConsumption(6.8, EntryType.BENZINA))
        assertEquals("0,5 L/1.000 km", formatConsumption(0.5, EntryType.OLIO))
        assertEquals("—", formatConsumption(null, EntryType.OLIO))
    }

    @Test
    fun litersUseOneToTwoDecimals() {
        assertEquals("40,0 L", formatLiters(40.0))
        assertEquals("0,5 L", formatLiters(0.5))
        assertEquals("40,25 L", formatLiters(40.25))
    }

    @Test
    fun moneyUsesTwoDecimalsAndEuroSign() {
        assertEquals("0,00 €", formatMoney(0.0))
        assertEquals("1.234,50 €", formatMoney(1234.5))
    }

    @Test
    fun kmUsesThousandsSeparator() {
        assertEquals("181.560 km", formatKm(181560))
        assertEquals("0 km", formatKm(0))
    }

    @Test
    fun parseDecimalAcceptsCommaAndDot() {
        assertEquals(40.5, parseDecimal("40,5")!!, 0.0)
        assertEquals(40.5, parseDecimal(" 40.5 ")!!, 0.0)
        assertEquals(null, parseDecimal(""))
        assertEquals(null, parseDecimal("abc"))
    }

    @Test
    fun dateIsItalianFormat() {
        assertEquals("09/11/2026", formatDate(java.time.LocalDate.of(2026, 11, 9).toEpochDay()))
    }
}
