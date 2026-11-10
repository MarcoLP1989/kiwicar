package com.kiwicar.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KiwiCarRepositoryTest {
    private lateinit var db: KiwiCarDatabase
    private lateinit var store: SettingsStore
    private lateinit var repo: KiwiCarRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiCarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SettingsStore(context)
        store.update { KiwiCarSettings() }
        repo = KiwiCarRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() = db.close()

    private fun fuel(km: Int, liters: Double, day: Long = 20000) =
        Entry(type = EntryType.BENZINA, dateEpochDay = day, odometerKm = km, liters = liters)

    @Test
    fun consumptionsAreComputedByKmEvenWhenSavedOutOfOrder() = runBlocking {
        repo.save(fuel(1500, 30.0))
        repo.save(fuel(1000, 40.0))
        repo.save(fuel(2000, 25.0))
        val list = repo.entriesWithStats().first()
        assertEquals(listOf(1000, 1500, 2000), list.map { it.entry.odometerKm })
        assertNull(list[0].consumption)
        assertEquals(6.0, list[1].consumption!!, 0.001)   // 30 / 500 * 100
        assertEquals(5.0, list[2].consumption!!, 0.001)   // 25 / 500 * 100
        assertNull(list[0].distanceFromPreviousKm)
        assertEquals(500, list[1].distanceFromPreviousKm)
        assertEquals(500, list[2].distanceFromPreviousKm)
    }

    @Test
    fun deletingMiddleEntryRecomputesNextAgainstNewPrevious() = runBlocking {
        repo.save(fuel(1000, 40.0))
        repo.save(fuel(1500, 30.0))
        repo.save(fuel(2000, 25.0))
        val middle = repo.entriesWithStats().first().single { it.entry.odometerKm == 1500 }.entry
        repo.delete(middle)
        val list = repo.entriesWithStats().first()
        assertEquals(2, list.size)
        assertEquals(2.5, list[1].consumption!!, 0.001)   // 25 / 1000 * 100
        assertEquals(1000, list[1].distanceFromPreviousKm)
    }

    @Test
    fun singleEntryHasNullConsumption() = runBlocking {
        repo.save(fuel(1000, 40.0))
        val list = repo.entriesWithStats().first()
        assertNull(list.single().consumption)
        assertNull(list.single().distanceFromPreviousKm)
    }

    @Test
    fun changingFuelPriceChangesEstimatedCost() = runBlocking {
        repo.save(fuel(1000, 40.0))
        store.update { it.copy(fuelPricePerLiter = 1.5) }
        assertEquals(60.0, repo.entriesWithStats().first().single().estimatedCost, 0.001)
        store.update { it.copy(fuelPricePerLiter = 2.0) }
        assertEquals(80.0, repo.entriesWithStats().first().single().estimatedCost, 0.001)
    }

    @Test
    fun oilEntryUsesOilPrice() = runBlocking {
        store.update { it.copy(fuelPricePerLiter = 2.0, oilPricePerLiter = 10.0) }
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = 20000, odometerKm = 1000, liters = 1.0))
        assertEquals(10.0, repo.entriesWithStats().first().single().estimatedCost, 0.001)
    }

    @Test
    fun homeStatsUsesLatestConsumptionPerTypeAndAverages() = runBlocking {
        val oct = LocalDate.of(2026, 10, 1).toEpochDay()
        val nov = LocalDate.of(2026, 11, 1).toEpochDay()
        store.update { it.copy(fuelPricePerLiter = 1.0) }
        repo.save(fuel(1000, 40.0, oct))
        repo.save(fuel(1500, 40.0, nov))
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = nov, odometerKm = 1000, liters = 1.0))
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = nov, odometerKm = 3000, liters = 1.0))
        val s = repo.homeStats(LocalDate.of(2026, 11, 9)).first()
        assertEquals(8.0, s.fuelLper100!!, 0.001)
        assertEquals(0.5, s.oilLper1000!!, 0.001)
        assertEquals(40.0, s.avgMonthlyCost, 0.01)
        assertEquals(480.0, s.avgYearlyCost, 0.01)
    }

    @Test
    fun homeStatsEmptyHasNullConsumptions() = runBlocking {
        val s = repo.homeStats(LocalDate.of(2026, 11, 9)).first()
        assertNull(s.fuelLper100)
        assertNull(s.oilLper1000)
        assertEquals(0.0, s.avgMonthlyCost, 0.0)
    }
}
