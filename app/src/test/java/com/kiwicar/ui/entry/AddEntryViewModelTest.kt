package com.kiwicar.ui.entry

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AddEntryViewModelTest {
    private lateinit var db: KiwiDatabase
    private lateinit var store: SettingsStore
    private lateinit var repo: KiwiRepository
    private val today = LocalDate.of(2026, 11, 9)

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SettingsStore(context)
        store.update { KiwiSettings(fuelPricePerLiter = 2.0, oilPricePerLiter = 10.0) }
        repo = KiwiRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun vm() = AddEntryViewModel(repo, store) { today }

    private suspend fun seed(type: EntryType, km: Int) =
        repo.save(Entry(type = type, dateEpochDay = today.toEpochDay() - 1, odometerKm = km, liters = 10.0))

    @Test
    fun defaultsToFuelAndTodayAndCannotSaveEmpty() = runBlocking {
        val vm = vm()
        assertEquals(EntryType.BENZINA, vm.type.value)
        assertEquals(today.toEpochDay(), vm.dateEpochDay.value)
        assertFalse(vm.canSave.value)
    }

    @Test
    fun canSaveWithValidLitersAndKm() = runBlocking {
        val vm = vm()
        vm.setKm("1000"); vm.setLiters("40,5")
        assertEquals(true, vm.canSave.first { it })
    }

    @Test
    fun cannotSaveWhenLitersOrKmNotPositiveOrUnparseable() = runBlocking {
        val vm = vm()
        vm.setKm("1000"); vm.setLiters("40,5")
        vm.canSave.first { it }
        vm.setLiters("0"); assertFalse(vm.canSave.value)
        vm.setLiters("abc"); assertFalse(vm.canSave.value)
        vm.setLiters("40"); vm.setKm("0"); assertFalse(vm.canSave.value)
        vm.setKm(""); assertFalse(vm.canSave.value)
    }

    @Test
    fun kmBelowMaxOfSameTypeBlocksSaveWithHint() = runBlocking {
        seed(EntryType.BENZINA, 181560)
        val vm = vm()
        vm.setKm("181000"); vm.setLiters("30")
        val hint = vm.kmError.first { it != null }
        assertEquals("Km inferiori all'ultimo inserimento (181.560 km)", hint)
        assertFalse(vm.canSave.value)
    }

    @Test
    fun kmEqualToMaxIsAllowed() = runBlocking {
        seed(EntryType.BENZINA, 5000)
        val vm = vm()
        vm.setKm("5000"); vm.setLiters("30")
        assertEquals(true, vm.canSave.first { it })
        assertNull(vm.kmError.value)
    }

    @Test
    fun otherTypesKmIsIgnored() = runBlocking {
        seed(EntryType.OLIO, 9000)
        val vm = vm()
        vm.setKm("5000"); vm.setLiters("30")
        assertEquals(true, vm.canSave.first { it })
    }

    @Test
    fun estimatedCostUsesPriceOfSelectedType() = runBlocking {
        val vm = vm()
        vm.setLiters("40,5")
        assertEquals(81.0, vm.estimatedCost.first { it > 0.0 }, 0.001)
        vm.setType(EntryType.OLIO)
        vm.setLiters("1")
        assertEquals(10.0, vm.estimatedCost.first { it == 10.0 }, 0.001)
    }

    @Test
    fun oilQuickChoicesSetLiters() = runBlocking {
        val vm = vm()
        vm.setType(EntryType.OLIO)
        vm.setOilChoice(OilChoice.ONE)
        assertEquals("1", vm.litersText.value)
        vm.setOilChoice(OilChoice.HALF)
        assertEquals("0,5", vm.litersText.value)
        vm.setOilChoice(OilChoice.OTHER)
        assertEquals("", vm.litersText.value)
    }

    @Test
    fun savePersistsEntryWithSelectedType() = runBlocking {
        val vm = vm()
        vm.setType(EntryType.OLIO)
        vm.setOilChoice(OilChoice.HALF)
        vm.setKm("1234")
        vm.setDate(today.toEpochDay() - 3)
        vm.canSave.first { it }
        vm.save()
        val saved = repo.entriesWithStats().first { it.isNotEmpty() }.single().entry
        assertEquals(EntryType.OLIO, saved.type)
        assertEquals(today.toEpochDay() - 3, saved.dateEpochDay)
        assertEquals(1234, saved.odometerKm)
        assertEquals(0.5, saved.liters, 0.0)
        assertNotNull(saved.id)
    }
}
