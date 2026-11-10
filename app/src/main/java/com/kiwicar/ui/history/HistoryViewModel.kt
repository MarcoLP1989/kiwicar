package com.kiwicar.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kiwicar.KiwiCarApplication
import com.kiwicar.data.Entry
import com.kiwicar.data.EntryWithStats
import com.kiwicar.data.KiwiCarRepository
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(private val repository: KiwiCarRepository) : ViewModel() {

    /** Newest first (date DESC, ties by km DESC); consumption is derived reactively by the repository. */
    val items: StateFlow<List<EntryWithStats>> = repository.entriesWithStats()
        .map { list ->
            list.sortedWith(
                compareByDescending<EntryWithStats> { it.entry.dateEpochDay }
                    .thenByDescending { it.entry.odometerKm }
                    .thenByDescending { it.entry.id },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun delete(entry: Entry) = repository.delete(entry)

    /** Fire-and-forget delete that survives the screen leaving composition; a failure is logged, not thrown. */
    fun deleteAsync(entry: Entry) {
        viewModelScope.launch {
            try {
                repository.delete(entry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Delete failed", e)
            }
        }
    }

    suspend fun update(entry: Entry) = repository.save(entry)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel((this[APPLICATION_KEY] as KiwiCarApplication).container.repository)
            }
        }
    }
}
