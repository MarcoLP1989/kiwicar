package com.kiwicar.ui.history

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryViewModelTest {
    private lateinit var db: KiwiDatabase
    private lateinit var repo: KiwiRepository

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiDatabase::class.java).allowMainThreadQueries().build()
        val store = SettingsStore(context)
        store.update { KiwiSettings(fuelPricePerLiter = 2.0, oilPricePerLiter = 10.0) }
        repo = KiwiRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun fuel(day: Long, km: Int, l: Double) =
        repo.save(Entry(type = EntryType.BENZINA, dateEpochDay = day, odometerKm = km, liters = l))

    private suspend fun items(vm: HistoryViewModel, size: Int) = vm.items.first { it.size == size }

    @Test
    fun orderedByDateDescendingTiesByKmDescending() = runBlocking {
        fuel(10, 1000, 40.0); fuel(30, 2000, 40.0); fuel(30, 2500, 40.0); fuel(20, 1500, 40.0)
        val list = items(HistoryViewModel(repo), 4)
        assertEquals(listOf(2500, 2000, 1500, 1000), list.map { it.entry.odometerKm })
    }

    @Test
    fun deletingMiddleEntryRecalculatesNextConsumptionAgainstNewPrevious() = runBlocking {
        fuel(1, 1000, 40.0); fuel(2, 1500, 30.0); fuel(3, 2000, 50.0)
        val vm = HistoryViewModel(repo)
        val before = items(vm, 3)
        assertEquals(10.0, before.first { it.entry.odometerKm == 2000 }.consumption!!, 0.001)
        vm.delete(before.first { it.entry.odometerKm == 1500 }.entry)
        val after = items(vm, 2)
        assertEquals(5.0, after.first { it.entry.odometerKm == 2000 }.consumption!!, 0.001)
    }

    @Test
    fun deletingOnlyReferenceMakesNextFirst() = runBlocking {
        fuel(1, 1000, 40.0); fuel(2, 1500, 30.0)
        val vm = HistoryViewModel(repo)
        val before = items(vm, 2)
        vm.delete(before.first { it.entry.odometerKm == 1000 }.entry)
        assertNull(items(vm, 1).single().consumption)
    }

    @Test
    fun updateRecalculatesNextEntryConsumption() = runBlocking {
        fuel(1, 1000, 40.0); fuel(2, 1500, 30.0); fuel(3, 2000, 50.0)
        val vm = HistoryViewModel(repo)
        val middle = items(vm, 3).first { it.entry.odometerKm == 1500 }.entry
        vm.update(middle.copy(odometerKm = 1800, liters = 20.0))
        val list = vm.items.first { l -> l.any { it.entry.odometerKm == 1800 } }
        assertEquals(3, list.size)
        // 2000 vs previous 1800: 50 L over 200 km
        assertEquals(25.0, list.first { it.entry.odometerKm == 2000 }.consumption!!, 0.001)
    }
}
