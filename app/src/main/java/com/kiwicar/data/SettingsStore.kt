package com.kiwicar.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class KiwiSettings(
    val carName: String = "",
    val initialKm: Int = 0,
    val fuelPricePerLiter: Double = 0.0,
    val oilPricePerLiter: Double = 0.0,
)

// Single process-wide instance, as required by DataStore.
private val Context.kiwiDataStore by preferencesDataStore(name = "kiwi_settings")

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.kiwiDataStore

    val settings: Flow<KiwiSettings> = dataStore.data.map { it.toSettings() }

    suspend fun update(block: (KiwiSettings) -> KiwiSettings) {
        dataStore.edit { prefs ->
            val new = block(prefs.toSettings())
            prefs[CAR_NAME] = new.carName
            prefs[INITIAL_KM] = new.initialKm
            prefs[FUEL_PRICE] = new.fuelPricePerLiter
            prefs[OIL_PRICE] = new.oilPricePerLiter
        }
    }

    private fun Preferences.toSettings() = KiwiSettings(
        carName = this[CAR_NAME] ?: "",
        initialKm = this[INITIAL_KM] ?: 0,
        fuelPricePerLiter = this[FUEL_PRICE] ?: 0.0,
        oilPricePerLiter = this[OIL_PRICE] ?: 0.0,
    )

    private companion object {
        val CAR_NAME = stringPreferencesKey("car_name")
        val INITIAL_KM = intPreferencesKey("initial_km")
        val FUEL_PRICE = doublePreferencesKey("fuel_price_per_liter")
        val OIL_PRICE = doublePreferencesKey("oil_price_per_liter")
    }
}
