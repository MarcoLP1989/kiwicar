package com.kiwicar.ui.entry

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.KiwiCarDao
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
        store.update { KiwiCarSettings(fuelPricePerLiter = 2.0, oilPricePerLiter = 10.0) }
        repo = KiwiCarRepository(db.kiwiDao(), store)
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
        vm.onKmFocusLost()
        val hint = vm.kmError.first { it != null }
        assertEquals("Km inferiori al rifornimento del 08/11/2026 (181.560 km)", hint)
        assertFalse(vm.canSave.value)
    }

    private suspend fun seedOn(type: EntryType, day: Long, km: Int) =
        repo.save(Entry(type = type, dateEpochDay = day, odometerKm = km, liters = 10.0))

    private val d = LocalDate.of(2026, 11, 1).toEpochDay()

    @Test
    fun backdatedEntryBetweenTwoExistingOnesIsAllowed() = runBlocking {
        seedOn(EntryType.BENZINA, d, 1000)
        seedOn(EntryType.BENZINA, d + 10, 2000)
        val vm = vm()
        vm.setDate(d + 5); vm.setKm("1500"); vm.setLiters("30")
        assertEquals(true, vm.canSave.first { it })
        assertNull(vm.kmError.value)
        assertEquals(true, vm.save())
    }

    @Test
    fun backdatedEntryAboveNextDatedEntryIsRejectedNamingTheUpperBound() = runBlocking {
        seedOn(EntryType.BENZINA, d, 1000)
        seedOn(EntryType.BENZINA, d + 10, 2000)
        val vm = vm()
        vm.setDate(d + 5); vm.setKm("2500"); vm.setLiters("30")
        assertEquals("Km superiori al rifornimento del 11/11/2026 (2.000 km)", vm.kmError.first { it != null })
        assertFalse(vm.canSave.value)
        assertFalse(vm.save())
    }

    @Test
    fun backdatedEntryBelowPreviousDatedEntryIsRejectedNamingTheLowerBound() = runBlocking {
        seedOn(EntryType.BENZINA, d, 1000)
        seedOn(EntryType.BENZINA, d + 10, 2000)
        val vm = vm()
        vm.setDate(d + 5); vm.setKm("900"); vm.setLiters("30")
        vm.onKmFocusLost()
        assertEquals("Km inferiori al rifornimento del 01/11/2026 (1.000 km)", vm.kmError.first { it != null })
        assertFalse(vm.canSave.value)
    }

    @Test
    fun backdatedBoundsAreInclusiveAndIgnoreOtherType() = runBlocking {
        seedOn(EntryType.BENZINA, d, 1000)
        seedOn(EntryType.BENZINA, d + 10, 2000)
        seedOn(EntryType.OLIO, d + 5, 1)
        val vm = vm()
        vm.setDate(d + 5); vm.setLiters("30")
        vm.setKm("1000"); assertEquals(true, vm.canSave.first { it })
        vm.setKm("2000"); assertEquals(true, vm.canSave.first { it })
    }

    @Test
    fun typingACorrectValueDigitByDigitNeverShowsTheError() = runBlocking {
        seedOn(EntryType.BENZINA, d, 99000)
        val vm = vm()
        vm.setDate(d + 1); vm.setLiters("30")
        for (prefix in listOf("1", "10", "100", "1000", "10000", "100000")) {
            vm.setKm(prefix)
            assertNull("hint shown while typing '$prefix'", vm.kmError.value)
        }
        assertEquals(true, vm.canSave.first { it })
        // A wrong value only gets flagged once the field loses focus, and typing hides it again.
        vm.setKm("1")
        assertFalse(vm.canSave.value)
        vm.onKmFocusLost()
        assertNotNull(vm.kmError.value)
        vm.setKm("10")
        assertNull(vm.kmError.value)
    }

    @Test
    fun saveFailureKeepsInputPostsMessageAndAllowsRetry() = runBlocking {
        val failing = object : KiwiCarDao by db.kiwiDao() {
            override suspend fun upsert(e: Entry): Long = throw java.io.IOException("disk full")
        }
        val vm = AddEntryViewModel(KiwiCarRepository(failing, store), store) { today }
        vm.setKm("1000"); vm.setLiters("40")
        vm.canSave.first { it }
        assertFalse(vm.save())
        assertEquals("Salvataggio non riuscito, riprova", vm.messages.first())
        assertFalse(vm.isSaving.value)
        assertEquals("1000", vm.kmText.value)
        assertFalse(vm.saved.value)
    }

    @Test
    fun submitSetsSavedOnlyOnSuccess() = runBlocking {
        val vm = vm()
        vm.setKm("1000"); vm.setLiters("40")
        vm.canSave.first { it }
        vm.submit()
        assertEquals(true, vm.saved.first { it })
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
        assertEquals(true, vm.save())
        val saved = repo.entriesWithStats().first { it.isNotEmpty() }.single().entry
        assertEquals(EntryType.OLIO, saved.type)
        assertEquals(today.toEpochDay() - 3, saved.dateEpochDay)
        assertEquals(1234, saved.odometerKm)
        assertEquals(0.5, saved.liters, 0.0)
        assertNotNull(saved.id)
    }

    @Test
    fun saveTwiceInARowPersistsOnlyOneEntry() = runBlocking {
        val vm = vm()
        vm.setKm("1000"); vm.setLiters("40")
        vm.canSave.first { it }
        assertEquals(true, vm.save())
        assertFalse(vm.save())
        assertEquals(1, repo.entriesWithStats().first { it.isNotEmpty() }.size)
    }

    @Test
    fun saveWithInvalidInputReturnsFalseAndPersistsNothing() = runBlocking {
        val vm = vm()
        vm.setKm("1000"); vm.setLiters("abc")
        assertFalse(vm.save())
        vm.setLiters("0")
        assertFalse(vm.save())
        assertEquals(0, repo.entriesWithStats().first().size)
    }

    private fun editVm(id: Long) = AddEntryViewModel(repo, store, editingId = id) { today }

    private suspend fun entries() = repo.entriesWithStats().first { it.isNotEmpty() }

    @Test
    fun editModePrefillsFromExistingEntry() = runBlocking {
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = 5, odometerKm = 4321, liters = 1.0))
        val id = entries().single().entry.id
        val vm = editVm(id)
        vm.loaded.first { it }
        assertEquals("4321", vm.kmText.value)
        assertEquals(EntryType.OLIO, vm.type.value)
        assertEquals(5L, vm.dateEpochDay.value)
        assertEquals(OilChoice.ONE, vm.oilChoice.value)
        assertEquals("1", vm.litersText.value)
    }

    @Test
    fun editModeOilWithCustomLitersSelectsOther() = runBlocking {
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = 5, odometerKm = 4321, liters = 0.75))
        val vm = editVm(entries().single().entry.id)
        vm.loaded.first { it }
        assertEquals(OilChoice.OTHER, vm.oilChoice.value)
        assertEquals("0,75", vm.litersText.value)
    }

    @Test
    fun editModeAllowsKmBelowMaxAndUpdatesInsteadOfInserting() = runBlocking {
        seed(EntryType.BENZINA, 1000)
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = 7, odometerKm = 5000, liters = 20.0))
        val target = entries().first { it.entry.odometerKm == 1000 }.entry
        val vm = editVm(target.id)
        vm.loaded.first { it }
        vm.setKm("4000")
        assertNull(vm.kmError.value)
        assertEquals(true, vm.canSave.first { it })
        assertEquals(true, vm.save())
        val list = repo.entriesWithStats().first { l -> l.any { it.entry.odometerKm == 4000 } }
        assertEquals(2, list.size)
        assertEquals(target.id, list.first { it.entry.odometerKm == 4000 }.entry.id)
    }

    @Test
    fun editModeStillRejectsNonPositiveLiters() = runBlocking {
        seed(EntryType.BENZINA, 1000)
        val vm = editVm(entries().single().entry.id)
        vm.loaded.first { it }
        vm.setLiters("0")
        assertFalse(vm.canSave.value)
        assertFalse(vm.save())
    }

    @Test
    fun editModeWithMissingIdIsNotFoundAndCannotSave() = runBlocking {
        val vm = editVm(9999)
        assertEquals(true, vm.notFound.first { it })
        vm.setKm("1000"); vm.setLiters("10")
        assertFalse(vm.canSave.value)
        assertFalse(vm.save())
    }
}
