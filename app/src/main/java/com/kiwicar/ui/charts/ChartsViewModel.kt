package com.kiwicar.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiApplication
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.EntryWithStats
import com.kiwicar.data.KiwiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class ChartWindow(val label: String, val days: Long) {
    MESE("Mese", 30),
    ANNO("Anno", 365),
}

/** [source] is null for aggregated points (monthly cost), which are not tappable. */
data class ChartPoint(val epochDay: Long, val value: Double, val source: EntryWithStats? = null)

data class ChartSeries(val unit: String, val points: List<ChartPoint> = emptyList())

data class ChartData(
    val oilTopUps: ChartSeries = ChartSeries("L"),
    val fuelConsumption: ChartSeries = ChartSeries("L/100 km"),
    val oilConsumption: ChartSeries = ChartSeries("L/1.000 km"),
    val monthlyFuelCost: ChartSeries = ChartSeries("€"),
    /** Last day of the X axis; the same date used to filter the series. */
    val today: LocalDate = LocalDate.now(),
)

/** Builds the four series from [entries] (ascending km), keeping dates in [today - window.days, today]. */
fun buildChartData(entries: List<EntryWithStats>, window: ChartWindow, today: LocalDate): ChartData {
    val to = today.toEpochDay()
    val from = to - window.days
    val inWindow = entries
        .filter { it.entry.dateEpochDay in from..to }
        .sortedBy { it.entry.dateEpochDay }
    fun point(e: EntryWithStats, value: Double) = ChartPoint(e.entry.dateEpochDay, value, e)

    val fuel = inWindow.filter { it.entry.type == EntryType.BENZINA }
    val oil = inWindow.filter { it.entry.type == EntryType.OLIO }
    val monthly = fuel
        .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.entry.dateEpochDay)) }
        .toSortedMap()
        .map { (month, list) -> ChartPoint(month.atDay(1).toEpochDay(), list.sumOf { it.estimatedCost }) }
    val base = ChartData(today = today)
    return ChartData(
        today = today,
        oilTopUps = base.oilTopUps.copy(points = oil.map { point(it, it.entry.liters) }),
        fuelConsumption = base.fuelConsumption.copy(
            points = fuel.filter { it.consumption != null }.map { point(it, it.consumption!!) },
        ),
        oilConsumption = base.oilConsumption.copy(
            points = oil.filter { it.consumption != null }.map { point(it, it.consumption!!) },
        ),
        monthlyFuelCost = base.monthlyFuelCost.copy(points = monthly),
    )
}

class ChartsViewModel(
    private val repository: KiwiRepository,
    today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _window = MutableStateFlow(ChartWindow.ANNO)
    val window: StateFlow<ChartWindow> = _window

    val series: StateFlow<ChartData> = combine(repository.entriesWithStats(), _window) { entries, w ->
        buildChartData(entries, w, today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartData())

    fun setWindow(window: ChartWindow) = _window.update { window }

    fun delete(entry: Entry) {
        viewModelScope.launch {
            try {
                repository.delete(entry)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChartsViewModel", "Delete failed", e)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiApplication).container
                ChartsViewModel(container.repository)
            }
        }
    }
}
