package com.kiwicar.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiApplication
import com.kiwicar.domain.Calculations
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.KiwiRepository
import com.kiwicar.data.SettingsStore
import com.kiwicar.ui.formatKm
import com.kiwicar.ui.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class OilChoice { HALF, ONE, OTHER }

class AddEntryViewModel(
    private val repository: KiwiRepository,
    settingsStore: SettingsStore,
    /** When set, the form edits that entry instead of creating a new one. */
    private val editingId: Long? = null,
    today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _type = MutableStateFlow(EntryType.BENZINA)
    val type: StateFlow<EntryType> = _type

    private val _dateEpochDay = MutableStateFlow(today().toEpochDay())
    val dateEpochDay: StateFlow<Long> = _dateEpochDay

    private val _kmText = MutableStateFlow("")
    val kmText: StateFlow<String> = _kmText

    private val _litersText = MutableStateFlow("")
    val litersText: StateFlow<String> = _litersText

    private val _oilChoice = MutableStateFlow(OilChoice.HALF)
    val oilChoice: StateFlow<OilChoice> = _oilChoice

    private var editingEntry: Entry? = null

    init {
        if (editingId != null) viewModelScope.launch {
            val e = repository.entriesWithStats().first().firstOrNull { it.entry.id == editingId }?.entry
                ?: return@launch
            editingEntry = e
            _type.value = e.type
            _dateEpochDay.value = e.dateEpochDay
            _kmText.value = e.odometerKm.toString()
            _oilChoice.value = when {
                e.type != EntryType.OLIO || e.liters == 0.5 -> OilChoice.HALF
                e.liters == 1.0 -> OilChoice.ONE
                else -> OilChoice.OTHER
            }
            _litersText.value = formatLitersInput(e.liters)
        }
    }

    private fun formatLitersInput(l: Double): String =
        if (l == l.toLong().toDouble()) l.toLong().toString() else l.toString().replace('.', ',')

    private val liters = _litersText.map { parseDecimal(it) }
    private val km = _kmText.map { it.trim().toIntOrNull() }

    /** Highest odometer already stored per type; null until the history is loaded. */
    private val maxKmByType: StateFlow<Map<EntryType, Int>?> = repository.entriesWithStats()
        .map { list -> list.groupBy { it.entry.type }.mapValues { (_, v) -> v.maxOf { it.entry.odometerKm } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Short Italian hint when km is lower than the last entry of the same type. */
    val kmError: StateFlow<String?> = combine(km, _type, maxKmByType) { km, type, max ->
        lastKmBelow(km, type, max)?.let { "Km inferiori all'ultimo inserimento (${formatKm(it)})" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val estimatedCost: StateFlow<Double> = combine(liters, _type, settingsStore.settings) { l, type, s ->
        val price = if (type == EntryType.BENZINA) s.fuelPricePerLiter else s.oilPricePerLiter
        if (l != null && l > 0) Calculations.estimatedCost(l, price) else 0.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Computed from the sources (not from kmError) so it never lags behind the km rule.
    val canSave: StateFlow<Boolean> = combine(liters, km, _type, maxKmByType) { l, km, type, max ->
        max != null && l != null && l > 0 && km != null && km > 0 && lastKmBelow(km, type, max) == null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The last stored km of [type] when [km] is lower than it (violating the non-decreasing rule), else null. */
    private fun lastKmBelow(km: Int?, type: EntryType, max: Map<EntryType, Int>?): Int? =
        if (editingId != null) null else max?.get(type)?.takeIf { km != null && km < it }

    fun setType(type: EntryType) {
        if (type == _type.value) return
        _type.value = type
        if (type == EntryType.OLIO) setOilChoice(OilChoice.HALF) else _litersText.value = ""
    }

    fun setDate(epochDay: Long) { _dateEpochDay.value = epochDay }
    fun setKm(text: String) { _kmText.value = text }
    fun setLiters(text: String) { _litersText.value = text }

    fun setOilChoice(choice: OilChoice) {
        _oilChoice.value = choice
        _litersText.value = when (choice) {
            OilChoice.HALF -> "0,5"
            OilChoice.ONE -> "1"
            OilChoice.OTHER -> ""
        }
    }

    private val _isSaving = MutableStateFlow(false)
    /** True while a save is in flight and after it succeeded (the screen is about to close). */
    val isSaving: StateFlow<Boolean> = _isSaving

    /** Persists the entry; returns true only if this call saved it. Idempotent while in flight / after success. */
    suspend fun save(): Boolean {
        if (_isSaving.value) return false
        val l = parseDecimal(_litersText.value)
        val km = _kmText.value.trim().toIntOrNull()
        val type = _type.value
        if (editingId != null && editingEntry == null) return false
        val valid = l != null && l > 0 && km != null && km > 0 &&
            maxKmByType.value != null && lastKmBelow(km, type, maxKmByType.value) == null
        if (!valid) return false
        _isSaving.value = true
        try {
            repository.save(Entry(id = editingEntry?.id ?: 0, type = type, dateEpochDay = _dateEpochDay.value, odometerKm = km, liters = l))
        } catch (e: Throwable) {
            _isSaving.value = false
            throw e
        }
        return true
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiApplication).container
                AddEntryViewModel(container.repository, container.settingsStore)
            }
        }

        fun factoryForEdit(id: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiApplication).container
                AddEntryViewModel(container.repository, container.settingsStore, editingId = id)
            }
        }
    }
}
