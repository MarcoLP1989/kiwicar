package com.kiwicar.ui.charts

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.EntryWithStats
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 11, 9)

private fun ews(
    type: EntryType,
    daysAgo: Long,
    km: Int = 1000,
    liters: Double = 10.0,
    consumption: Double? = null,
    cost: Double = 0.0,
) = EntryWithStats(
    Entry(id = daysAgo, type = type, dateEpochDay = TODAY.toEpochDay() - daysAgo, odometerKm = km, liters = liters),
    consumption, cost, null,
)

class BuildChartDataTest {
    @Test
    fun meseKeepsOnlyLast30Days() {
        val entries = listOf(
            ews(EntryType.OLIO, 31, liters = 1.0),
            ews(EntryType.OLIO, 30, liters = 2.0),
            ews(EntryType.OLIO, 0, liters = 3.0),
        )
        val d = buildChartData(entries, ChartWindow.MESE, TODAY)
        assertEquals(listOf(2.0, 3.0), d.oilTopUps.points.map { it.value })
    }

    @Test
    fun annoIncludesElevenMonthsAndExcludes366Days() {
        val entries = listOf(
            ews(EntryType.OLIO, 366, liters = 1.0),
            ews(EntryType.OLIO, 365, liters = 2.0),
            ews(EntryType.OLIO, 335, liters = 3.0),
        )
        val d = buildChartData(entries, ChartWindow.ANNO, TODAY)
        assertEquals(listOf(2.0, 3.0), d.oilTopUps.points.map { it.value })
    }

    @Test
    fun consumptionSeriesExcludeNullAndSplitByType() {
        val entries = listOf(
            ews(EntryType.BENZINA, 20, consumption = null),
            ews(EntryType.BENZINA, 10, consumption = 6.5),
            ews(EntryType.OLIO, 8, consumption = null),
            ews(EntryType.OLIO, 5, consumption = 0.4),
        )
        val d = buildChartData(entries, ChartWindow.ANNO, TODAY)
        assertEquals(listOf(6.5), d.fuelConsumption.points.map { it.value })
        assertEquals(listOf(0.4), d.oilConsumption.points.map { it.value })
        assertNotNull(d.fuelConsumption.points[0].source)
    }

    @Test
    fun monthlyFuelCostSumsByCalendarMonthAndIgnoresOil() {
        val entries = listOf(
            ews(EntryType.BENZINA, 70, cost = 10.0), // 2026-08-31
            ews(EntryType.BENZINA, 40, cost = 20.0), // 2026-09-30
            ews(EntryType.BENZINA, 35, cost = 5.0), // 2026-10-05
            ews(EntryType.BENZINA, 3, cost = 7.0), // 2026-11-06
            ews(EntryType.BENZINA, 1, cost = 8.0), // 2026-11-08
            ews(EntryType.OLIO, 2, cost = 99.0),
        )
        val d = buildChartData(entries, ChartWindow.ANNO, TODAY)
        assertEquals(listOf(10.0, 20.0, 5.0, 15.0), d.monthlyFuelCost.points.map { it.value })
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), d.monthlyFuelCost.points.last().epochDay)
        assertNull(d.monthlyFuelCost.points.last().source)
    }

    @Test
    fun todayIsExposedWithSeries() {
        assertEquals(TODAY, buildChartData(emptyList(), ChartWindow.MESE, TODAY).today)
    }

    @Test
    fun unitLabelsPerSeries() {
        val d = buildChartData(emptyList(), ChartWindow.ANNO, TODAY)
        assertEquals("L", d.oilTopUps.unit)
        assertEquals("L/100 km", d.fuelConsumption.unit)
        assertEquals("L/1.000 km", d.oilConsumption.unit)
        assertEquals("€", d.monthlyFuelCost.unit)
        assertEquals(true, d.oilTopUps.points.isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChartsViewModelTest {
    private lateinit var db: KiwiCarDatabase
    private lateinit var repo: KiwiCarRepository

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiCarDatabase::class.java).allowMainThreadQueries().build()
        val store = SettingsStore(context)
        store.update { KiwiCarSettings() }
        repo = KiwiCarRepository(db.kiwiDao(), store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun defaultsToAnnoAndSwitchingWindowFiltersSeries() = runBlocking {
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = TODAY.toEpochDay() - 100, odometerKm = 500, liters = 1.0))
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = TODAY.toEpochDay() - 2, odometerKm = 900, liters = 0.5))
        val vm = ChartsViewModel(repo) { TODAY }
        assertEquals(ChartWindow.ANNO, vm.window.value)
        assertEquals(2, vm.series.first { it.oilTopUps.points.size == 2 }.oilTopUps.points.size)
        vm.setWindow(ChartWindow.MESE)
        assertEquals(1, vm.series.first { it.oilTopUps.points.size == 1 }.oilTopUps.points.size)
    }

    @Test
    fun deleteRemovesEntry() = runBlocking {
        repo.save(Entry(type = EntryType.OLIO, dateEpochDay = TODAY.toEpochDay(), odometerKm = 900, liters = 0.5))
        val vm = ChartsViewModel(repo) { TODAY }
        val e = vm.series.first { it.oilTopUps.points.isNotEmpty() }.oilTopUps.points[0].source!!.entry
        vm.delete(e)
        assertEquals(0, vm.series.first { it.oilTopUps.points.isEmpty() }.oilTopUps.points.size)
    }
}

class ChartGeometryTest {
    private val area = PlotArea(left = 40f, width = 300f, height = 100f)
    private fun p(day: Long, v: Double, tappable: Boolean = true) =
        ChartPoint(day, v, if (tappable) ews(EntryType.OLIO, 0) else null)

    @Test
    fun singlePointAndZeroRangesProduceFiniteOffsets() {
        val o = pointOffset(p(5, 0.0), area, 5, 5, 0.0)
        assertEquals(true, o.x.isFinite() && o.y.isFinite())
        assertEquals(100f, o.y, 0.001f)
        val all = pointOffset(p(5, 3.0), area, 0, 10, 3.0)
        assertEquals(0f, all.y, 0.001f)
        assertEquals(190f, all.x, 0.001f)
    }

    @Test
    fun tapInsideTallBarHitsItAndOutsideMisses() {
        val bar = p(5, 10.0)
        val o = pointOffset(bar, area, 0, 10, 10.0) // x=190, top y=0
        val mid = o.copy(y = 60f)
        assertEquals(bar, hitTest(listOf(bar), mid, area, 0, 10, 10.0, ChartKind.BAR, 24f, 7f))
        assertNull(hitTest(listOf(bar), mid.copy(x = 190f + 30f), area, 0, 10, 10.0, ChartKind.BAR, 24f, 7f))
        val short = p(5, 2.0) // top at y=80
        assertNull(hitTest(listOf(short), Offset(190f, 20f), area, 0, 10, 10.0, ChartKind.BAR, 24f, 7f))
    }

    @Test
    fun nonTappableBarsAreIgnored() {
        val bar = p(5, 10.0, tappable = false)
        assertNull(hitTest(listOf(bar), Offset(190f, 60f), area, 0, 10, 10.0, ChartKind.BAR, 24f, 7f))
    }

    @Test
    fun lineHitsNearestWithinRadiusOnly() {
        val a = p(2, 5.0)
        val b = p(8, 5.0)
        val ob = pointOffset(b, area, 0, 10, 10.0)
        assertEquals(b, hitTest(listOf(a, b), ob.copy(x = ob.x + 10f), area, 0, 10, 10.0, ChartKind.LINE, 24f, 7f))
        assertNull(hitTest(listOf(a, b), Offset(ob.x, ob.y - 60f), area, 0, 10, 10.0, ChartKind.LINE, 24f, 7f))
    }
}
