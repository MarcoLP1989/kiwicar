package com.kiwicar.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.KiwiDatabase
import com.kiwicar.data.KiwiRepository
import com.kiwicar.data.KiwiSettings
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsViewModelTest {
    private lateinit var db: KiwiDatabase
    private lateinit var store: SettingsStore
    private lateinit var repo: KiwiRepository

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java).allowMainThreadQueries().build()
        store = SettingsStore(context)
        store.update { KiwiSettings(carName = "Panda", initialKm = 100, fuelPricePerLiter = 1.72, oilPricePerLiter = 9.5) }
        repo = KiwiRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun vm(): SettingsViewModel {
        val vm = SettingsViewModel(store, repo)
        vm.loaded.first { it }
        return vm
    }

    @Test
    fun prefillsFieldsInItalianFormat() = runBlocking {
        val vm = vm()
        assertEquals("Panda", vm.carName)
        assertEquals("100", vm.initialKm)
        assertEquals("1,72", vm.fuelPrice)
        assertEquals("9,50", vm.oilPrice)
    }

    @Test
    fun savingValidPricesPersistsThem() = runBlocking {
        val vm = vm()
        vm.fuelPrice = "1,85"
        vm.oilPrice = "12.3"
        assertTrue(vm.savePrices())
        val s = store.settings.first()
        assertEquals(1.85, s.fuelPricePerLiter, 1e-9)
        assertEquals(12.3, s.oilPricePerLiter, 1e-9)
        assertNull(vm.pricesError)
    }

    @Test
    fun invalidPriceIsRejectedAndNotPersisted() = runBlocking {
        val vm = vm()
        for (bad in listOf("abc", "-1", "")) {
            vm.fuelPrice = bad
            assertFalse(vm.savePrices())
            assertNotNull(vm.pricesError)
        }
        val s = store.settings.first()
        assertEquals(1.72, s.fuelPricePerLiter, 1e-9)
        assertEquals(9.5, s.oilPricePerLiter, 1e-9)
    }

    @Test
    fun savingCarPersistsNameAndInitialKm() = runBlocking {
        val vm = vm()
        vm.carName = "  Fiat Panda  "
        vm.initialKm = "1500"
        assertTrue(vm.saveCar())
        val s = store.settings.first()
        assertEquals("Fiat Panda", s.carName)
        assertEquals(1500, s.initialKm)
        assertEquals(1.72, s.fuelPricePerLiter, 1e-9)
    }

    @Test
    fun invalidInitialKmIsRejected() = runBlocking {
        val vm = vm()
        for (bad in listOf("-5", "x", "1,5")) {
            vm.initialKm = bad
            assertFalse(vm.saveCar())
            assertNotNull(vm.carError)
        }
        assertEquals(100, store.settings.first().initialKm)
    }

    @Test
    fun laterEmissionsDoNotOverwriteUserInput() = runBlocking {
        val vm = vm()
        vm.carName = "Typing"
        store.update { it.copy(carName = "Other") }
        assertEquals("Typing", vm.carName)
    }

    @Test
    fun buildCsvExportsAllEntries() = runBlocking {
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = 20517, odometerKm = 1000, liters = 40.1))
        val csv = vm().buildCsv()
        assertEquals(listOf("tipo;data;km;litri", "BENZINA;2026-03-05;1000;40,1"), csv.trim().lines())
    }
}
