package com.kiwicar.ui

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
    fun moneyUsesTwoDecimalsAndEuroSign() {
        assertEquals("0,00 €", formatMoney(0.0))
        assertEquals("1.234,50 €", formatMoney(1234.5))
    }

    @Test
    fun kmUsesThousandsSeparator() {
        assertEquals("181.560 km", formatKm(181560))
        assertEquals("0 km", formatKm(0))
    }
}
