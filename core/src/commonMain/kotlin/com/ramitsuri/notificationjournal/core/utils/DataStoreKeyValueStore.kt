package com.ramitsuri.notificationjournal.core.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    override fun getStringFlow(key: String, fallback: String): Flow<String?> {
        return dataStore
            .data
            .map {
                it[stringPreferencesKey(key)] ?: fallback
            }
    }

    override suspend fun getString(key: String, fallback: String): String {
        return getStringFlow(key, fallback).firstOrNull() ?: fallback
    }

    override suspend fun putString(key: String, value: String) {
        dataStore.edit {
            it[stringPreferencesKey(key)] = value
        }
    }

    override fun getIntFlow(key: String, fallback: Int): Flow<Int> {
        return dataStore
            .data
            .map {
                it[intPreferencesKey(key)] ?: fallback
            }
    }

    override suspend fun getInt(key: String, fallback: Int): Int {
        return getIntFlow(key, fallback).firstOrNull() ?: fallback
    }

    override suspend fun putInt(key: String, value: Int) {
        dataStore.edit {
            it[intPreferencesKey(key)] = value
        }
    }

    override suspend fun hasKey(key: String): Boolean {
        return dataStore.data.map {
            it.contains(stringPreferencesKey(key))
        }.firstOrNull() ?: false
    }

    companion object {
        const val FILE = "datastore.preferences_pb"
    }
}
