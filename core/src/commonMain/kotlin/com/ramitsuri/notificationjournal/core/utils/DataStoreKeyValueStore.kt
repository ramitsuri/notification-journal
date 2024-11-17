package com.ramitsuri.notificationjournal.core.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStoreKeyValueStore(
    private val dataStore: DataStore<Preferences>,
) : KeyValueStoreV2 {

    override fun getBooleanFlow(key: Key, defaultValue: Boolean): Flow<Boolean> {
        return dataStore
            .data
            .map {
                it[booleanPreferencesKey(key.value)] ?: defaultValue
            }
    }

    override suspend fun getBoolean(key: Key, defaultValue: Boolean): Boolean {
        return getBooleanFlow(key, defaultValue).firstOrNull() ?: defaultValue
    }

    override suspend fun putBoolean(key: Key, value: Boolean) {
        dataStore.edit {
            it[booleanPreferencesKey(key.value)] = value
        }
    }

    companion object {
        const val FILE = "datastore.preferences_pb"
    }
}
