package com.kiwicar.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiApplication
import com.kiwicar.data.EntryType
import com.kiwicar.data.HomeStats
import com.kiwicar.data.KiwiRepository
import com.kiwicar.data.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val loaded: Boolean = false,
    val carName: String = "",
    val currentKm: Int = 0,
    val stats: HomeStats = HomeStats(null, null, 0.0, 0.0),
    /** Liters of the most recent OLIO top-ups, oldest first (max [MAX_OIL_BARS]). */
    val oilTopUps: List<Double> = emptyList(),
)

private const val MAX_OIL_BARS = 10

class HomeViewModel(
    repository: KiwiRepository,
    settingsStore: SettingsStore,
    today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    // today() is re-evaluated on every upstream emission (and on re-subscription), not frozen at creation.
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(
        settingsStore.settings,
        repository.entriesWithStats(),
    ) { settings, entries ->
        HomeUiState(
            loaded = true,
            carName = settings.carName,
            currentKm = maxOf(settings.initialKm, entries.maxOfOrNull { it.entry.odometerKm } ?: 0),
            oilTopUps = entries
                .filter { it.entry.type == EntryType.OLIO }
                .takeLast(MAX_OIL_BARS)
                .map { it.entry.liters },
        )
    }.flatMapLatest { base ->
        repository.homeStats(today()).map { base.copy(stats = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiApplication).container
                HomeViewModel(container.repository, container.settingsStore)
            }
        }
    }
}
