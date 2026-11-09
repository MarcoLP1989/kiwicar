package com.kiwicar

import android.app.Application
import com.kiwicar.data.KiwiDatabase
import com.kiwicar.data.KiwiRepository
import com.kiwicar.data.SettingsStore

class AppContainer(private val app: Application) {
    val database: KiwiDatabase by lazy { KiwiDatabase.get(app) }
    val settingsStore: SettingsStore by lazy { SettingsStore(app) }
    val repository: KiwiRepository by lazy { KiwiRepository(database.kiwiDao(), settingsStore) }
}

class KiwiApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
