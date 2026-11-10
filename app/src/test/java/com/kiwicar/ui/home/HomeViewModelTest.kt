package com.kiwicar.ui.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.KiwiCarDatabase
import com.kiwicar.data.KiwiCarRepository
import com.kiwicar.data.KiwiCarSettings
import com.kiwicar.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeViewModelTest {
    private lateinit var db: KiwiCarDatabase
    private lateinit var store: SettingsStore
    private lateinit var repo: KiwiCarRepository
    private val today = LocalDate.of(2026, 11, 9)

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiCarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SettingsStore(context)
        store.update { KiwiCarSettings() }
        repo = KiwiCarRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun vm() = HomeViewModel(repo, store) { today }

    private suspend fun HomeViewModel.awaitLoaded(): HomeUiState = state.first { it.loaded }

    @Test
    fun emptyHistoryHasNullConsumptionsZeroCostsAndInitialKm() = runBlocking {
        store.update { it.copy(carName = "Panda", initialKm = 1200) }
        val s = vm().awaitLoaded()
        assertEquals("Panda", s.carName)
        assertEquals(1200, s.currentKm)
        assertNull(s.stats.fuelLper100)
        assertNull(s.stats.oilLper1000)
        assertEquals(0.0, s.stats.avgMonthlyCost, 0.0)
        assertEquals(0.0, s.stats.avgYearlyCost, 0.0)
        assertEquals(emptyList<Double>(), s.oilTopUps)
    }

    @Test
    fun mapsRepositoryStatsAndCurrentKmFromEntries() = runBlocking {
        store.update { it.copy(carName = "Panda", initialKm = 1000, fuelPricePerLiter = 2.0) }
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = today.toEpochDay() - 10, odometerKm = 1000, liters = 40.0))
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = today.toEpochDay() - 5, odometerKm = 1500, liters = 30.0))
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = today.toEpochDay() - 2, odometerKm = 1600, liters = 0.5))
        val s = vm().state.first { it.loaded && it.oilTopUps.isNotEmpty() }
        assertEquals(1600, s.currentKm)
        assertEquals(6.0, s.stats.fuelLper100!!, 0.001)
        assertEquals(2.0, s.fuelPricePerLiter, 0.0)
        assertNull(s.stats.oilLper1000)
        assertEquals(listOf(0.5), s.oilTopUps)
    }

    @Test
    fun currentKmIsInitialKmWhenHigherThanAnyEntry() = runBlocking {
        store.update { it.copy(initialKm = 5000) }
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = today.toEpochDay(), odometerKm = 3000, liters = 10.0))
        val s = vm().awaitLoaded()
        assertEquals(5000, s.currentKm)
    }
}
