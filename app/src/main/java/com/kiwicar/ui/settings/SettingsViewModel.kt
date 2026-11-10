package com.kiwicar.ui.settings

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiCarApplication
import com.kiwicar.data.KiwiCarRepository
import com.kiwicar.data.SettingsStore
import com.kiwicar.export.CsvExporter
import com.kiwicar.ui.parseDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val repository: KiwiCarRepository,
) : ViewModel() {

    var carName by mutableStateOf("")
    var initialKm by mutableStateOf("")
    var fuelPrice by mutableStateOf("")
    var oilPrice by mutableStateOf("")
    var carError by mutableStateOf<String?>(null)
        private set
    var pricesError by mutableStateOf<String?>(null)
        private set

    private val _loaded = MutableStateFlow(false)
    /** True once the fields have been prefilled from the store (done once, never overwritten afterwards). */
    val loaded: StateFlow<Boolean> = _loaded

    init {
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            carName = s.carName
            initialKm = s.initialKm.toString()
            fuelPrice = formatPrice(s.fuelPricePerLiter)
            oilPrice = formatPrice(s.oilPricePerLiter)
            _loaded.value = true
        }
    }

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-shot user-facing messages ("Salvato" or a failure) for the Snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onSaveCar() { viewModelScope.launch { saveCar() } }
    fun onSavePrices() { viewModelScope.launch { savePrices() } }

    /** Runs a store write; posts "Salvato" on success or an error message on IO failure (input stays as typed). */
    private suspend fun persist(block: suspend () -> Unit): Boolean = try {
        block()
        _messages.trySend("Salvato")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("SettingsViewModel", "Save failed", e)
        _messages.trySend("Salvataggio non riuscito, riprova")
        false
    }

    /** Persists marca/modello and initial km; false (with [carError]) when the km is not a non-negative integer. */
    suspend fun saveCar(): Boolean {
        val km = initialKm.trim().toIntOrNull()?.takeIf { it >= 0 }
        if (km == null) {
            carError = "Inserisci un chilometraggio valido"
            return false
        }
        carError = null
        val name = carName.trim()
        if (!persist { settingsStore.update { it.copy(carName = name, initialKm = km) } }) return false
        carName = name
        return true
    }

    /** Persists both prices; false (with [pricesError]) and nothing persisted when either is invalid. */
    suspend fun savePrices(): Boolean {
        val fuel = parseDecimal(fuelPrice)?.takeIf { it >= 0 }
        val oil = parseDecimal(oilPrice)?.takeIf { it >= 0 }
        if (fuel == null || oil == null) {
            pricesError = "Inserisci prezzi validi (es. 1,72)"
            return false
        }
        pricesError = null
        return persist { settingsStore.update { it.copy(fuelPricePerLiter = fuel, oilPricePerLiter = oil) } }
    }

    suspend fun buildCsv(): String = CsvExporter.toCsv(repository.allEntries())

    private fun formatPrice(value: Double): String =
        NumberFormat.getNumberInstance(Locale.ITALY).apply {
            isGroupingUsed = false
            minimumFractionDigits = 2
            maximumFractionDigits = 3
        }.format(value)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as KiwiCarApplication).container
                SettingsViewModel(c.settingsStore, c.repository)
            }
        }
    }
}
