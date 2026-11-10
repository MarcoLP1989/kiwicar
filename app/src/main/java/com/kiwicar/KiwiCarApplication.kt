package com.kiwicar

import android.app.Application
import com.kiwicar.data.KiwiCarDatabase
import com.kiwicar.data.KiwiCarRepository
import com.kiwicar.data.SettingsStore

class AppContainer(private val app: Application) {
    val database: KiwiCarDatabase by lazy { KiwiCarDatabase.get(app) }
    val settingsStore: SettingsStore by lazy { SettingsStore(app) }
    val repository: KiwiCarRepository by lazy { KiwiCarRepository(database.kiwiDao(), settingsStore) }
}

class KiwiCarApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
