package com.example.final_project

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesRepository private constructor(private val dataStore: DataStore<Preferences>) {

    private val COUNTER_KEY = intPreferencesKey("counter")

    val counter: Flow<Int> = this.dataStore.data.map { prefs ->
        prefs[COUNTER_KEY] ?: INITIAL_COUNTER_VALUE
    }.distinctUntilChanged()

    private suspend fun saveIntValue(key: Preferences.Key<Int>, value: Int) {
        this.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    suspend fun saveCounter(value: Int) {
        saveIntValue(COUNTER_KEY, value)
    }

    companion object {
        private const val PREFERENCES_DATA_FILE_NAME = "settings"
        private var INSTANCE: PreferencesRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                val dataStore = PreferenceDataStoreFactory.create {
                    context.preferencesDataStoreFile(PREFERENCES_DATA_FILE_NAME)
                }
                INSTANCE = PreferencesRepository(dataStore)
            }
        }
        fun getRepository(): PreferencesRepository {
            return INSTANCE ?: throw IllegalStateException("AppPreferencesRepository not initialized yet")
        }
    }
}