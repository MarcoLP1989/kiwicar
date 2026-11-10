package com.kiwicar.ui.entry

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiCarApplication
import com.kiwicar.domain.Calculations
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryType
import com.kiwicar.data.KiwiCarRepository
import com.kiwicar.data.SettingsStore
import com.kiwicar.ui.formatDate
import com.kiwicar.ui.formatKm
import com.kiwicar.ui.parseDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val repository: KiwiCarRepository,
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

    private val _loaded = MutableStateFlow(editingId == null)
    /** True once the form is ready: immediately for a new entry, after the prefill (set last) in edit mode. */
    val loaded: StateFlow<Boolean> = _loaded

    private val _notFound = MutableStateFlow(false)
    /** Edit mode only: the entry to edit no longer exists (the screen should close). */
    val notFound: StateFlow<Boolean> = _notFound

    init {
        if (editingId != null) viewModelScope.launch {
            val e = repository.entriesWithStats().first().firstOrNull { it.entry.id == editingId }?.entry
            if (e == null) {
                _notFound.value = true
                return@launch
            }
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
            _loaded.value = true
        }
    }

    private fun formatLitersInput(l: Double): String =
        if (l == l.toLong().toDouble()) l.toLong().toString() else l.toString().replace('.', ',')

    private val liters = _litersText.map { parseDecimal(it) }
    private val km = _kmText.map { it.trim().toIntOrNull() }

    private data class KmRef(val type: EntryType, val date: Long, val km: Int)

    /** Where the typed km falls outside its neighbours: [tooHigh] false = below [ref], true = above [ref]. */
    private data class KmViolation(val tooHigh: Boolean, val ref: KmRef) {
        val message: String get() = (if (tooHigh) "Km superiori" else "Km inferiori") +
            " al rifornimento del ${formatDate(ref.date)} (${formatKm(ref.km)})"
    }

    /** Stored (type, date, km) of every entry; null until the history is loaded. */
    private val refs: StateFlow<List<KmRef>?> = repository.entriesWithStats()
        .map { list -> list.map { KmRef(it.entry.type, it.entry.dateEpochDay, it.entry.odometerKm) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * New entries only: km must be >= the max km of same-type entries dated on/before [date] and
     * <= the min km of same-type entries dated after it (equal allowed), so out-of-order inserts stay coherent.
     */
    private fun kmViolation(km: Int?, type: EntryType, date: Long, refs: List<KmRef>?): KmViolation? {
        if (editingId != null || km == null || refs == null) return null
        val same = refs.filter { it.type == type }
        val lower = same.filter { it.date <= date }.maxWithOrNull(compareBy<KmRef> { it.km }.thenBy { it.date })
        if (lower != null && km < lower.km) return KmViolation(false, lower)
        val upper = same.filter { it.date > date }.minWithOrNull(compareBy<KmRef> { it.km }.thenBy { it.date })
        if (upper != null && km > upper.km) return KmViolation(true, upper)
        return null
    }

    private val _kmTouched = MutableStateFlow(false)

    /** The km field lost focus with a value in it: from now until the next keystroke a "too low" hint may show. */
    fun onKmFocusLost() { if (_kmText.value.isNotBlank()) _kmTouched.value = true }

    /**
     * Short Italian hint naming the violated bound. A "too high" value can only get worse while typing, so it shows
     * at once; a "too low" one may just be an unfinished number, so it waits for focus loss (never red mid-typing).
     */
    val kmError: StateFlow<String?> = combine(km, _type, _dateEpochDay, refs, _kmTouched) { km, type, date, refs, touched ->
        kmViolation(km, type, date, refs)?.takeIf { it.tooHigh || touched }?.message
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val estimatedCost: StateFlow<Double> = combine(liters, _type, settingsStore.settings) { l, type, s ->
        val price = if (type == EntryType.BENZINA) s.fuelPricePerLiter else s.oilPricePerLiter
        if (l != null && l > 0) Calculations.estimatedCost(l, price) else 0.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Computed from the sources (not from kmError) so it never lags behind the km rule.
    val canSave: StateFlow<Boolean> = combine(liters, km, _type, _dateEpochDay, refs, _loaded) { a ->
        val l = a[0] as Double?
        val km = a[1] as Int?
        @Suppress("UNCHECKED_CAST") val refs = a[4] as List<KmRef>?
        val loaded = a[5] as Boolean
        loaded && refs != null && l != null && l > 0 && km != null && km > 0 &&
            kmViolation(km, a[2] as EntryType, a[3] as Long, refs) == null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setType(type: EntryType) {
        if (type == _type.value) return
        _type.value = type
        if (type == EntryType.OLIO) setOilChoice(OilChoice.HALF) else _litersText.value = ""
    }

    fun setDate(epochDay: Long) { _dateEpochDay.value = epochDay }
    fun setKm(text: String) { _kmText.value = text; _kmTouched.value = false }
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

    private val _saved = MutableStateFlow(false)
    /** Becomes true once a save succeeded (the screen then closes). */
    val saved: StateFlow<Boolean> = _saved

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-shot user-facing messages (save failures) for the Snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Fire-and-forget save that survives the screen leaving composition; sets [saved] on success. */
    fun submit() {
        viewModelScope.launch { if (save()) _saved.value = true }
    }

    /**
     * Persists the entry; returns true only if this call saved it. Idempotent while in flight / after success.
     * An IO failure keeps the typed input, re-enables saving and posts a message instead of crashing.
     */
    suspend fun save(): Boolean {
        if (_isSaving.value) return false
        val l = parseDecimal(_litersText.value)
        val km = _kmText.value.trim().toIntOrNull()
        val type = _type.value
        if (!_loaded.value) return false
        val valid = l != null && l > 0 && km != null && km > 0 &&
            refs.value != null && kmViolation(km, type, _dateEpochDay.value, refs.value) == null
        if (!valid) return false
        _isSaving.value = true
        try {
            repository.save(Entry(id = editingEntry?.id ?: 0, type = type, dateEpochDay = _dateEpochDay.value, odometerKm = km, liters = l))
        } catch (e: CancellationException) {
            _isSaving.value = false
            throw e
        } catch (e: Exception) {
            Log.e("AddEntryViewModel", "Save failed", e)
            _isSaving.value = false
            _messages.trySend("Salvataggio non riuscito, riprova")
            return false
        }
        return true
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiCarApplication).container
                AddEntryViewModel(container.repository, container.settingsStore)
            }
        }

        fun factoryForEdit(id: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KiwiCarApplication).container
                AddEntryViewModel(container.repository, container.settingsStore, editingId = id)
            }
        }
    }
}
