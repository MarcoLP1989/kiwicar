package com.kiwicar.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {
    private val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())

    @Before
    fun resetSharedStore() = runBlocking { store.update { KiwiSettings() } }

    @Test
    fun defaultsThenUpdatesArePersistedAndMerged() = runBlocking {
        assertEquals(KiwiSettings(), store.settings.first())
        store.update { it.copy(fuelPricePerLiter = 1.72) }
        store.update { it.copy(carName = "Fiat Panda", initialKm = 85000, oilPricePerLiter = 9.5) }
        val s = store.settings.first()
        assertEquals("Fiat Panda", s.carName)
        assertEquals(1.72, s.fuelPricePerLiter, 0.0)
        assertEquals(85000, s.initialKm)
        assertEquals(9.5, s.oilPricePerLiter, 0.0)
    }
}
