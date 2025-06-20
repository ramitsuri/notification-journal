package com.ramitsuri.notificationjournal.core.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStoreKeyValueStore(
    private val dataStore: DataStore<Preferences>,
) : KeyValueStoreV2 {
    override fun getBooleanFlow(
        key: Key,
        defaultValue: Boolean,
    ): Flow<Boolean> {
        return dataStore
            .data
            .mapDistinct {
                it[booleanPreferencesKey(key.value)] ?: defaultValue
            }
    }

    override suspend fun getBoolean(
        key: Key,
        defaultValue: Boolean,
    ): Boolean {
        return getBooleanFlow(key, defaultValue).firstOrNull() ?: defaultValue
    }

    override suspend fun putBoolean(
        key: Key,
        value: Boolean,
    ) {
        dataStore.edit {
            it[booleanPreferencesKey(key.value)] = value
        }
    }

    override fun getStringFlow(
        key: Key,
        fallback: String,
    ): Flow<String?> {
        return dataStore
            .data
            .mapDistinct {
                it[stringPreferencesKey(key.value)] ?: fallback
            }
    }

    override suspend fun getString(
        key: Key,
        fallback: String,
    ): String {
        return getStringFlow(key, fallback).firstOrNull() ?: fallback
    }

    override suspend fun putString(
        key: Key,
        value: String,
    ) {
        dataStore.edit {
            it[stringPreferencesKey(key.value)] = value
        }
    }

    override fun getIntFlow(
        key: Key,
        fallback: Int,
    ): Flow<Int> {
        return dataStore
            .data
            .mapDistinct {
                it[intPreferencesKey(key.value)] ?: fallback
            }
    }

    override suspend fun getInt(
        key: Key,
        fallback: Int,
    ): Int {
        return getIntFlow(key, fallback).firstOrNull() ?: fallback
    }

    override suspend fun putInt(
        key: Key,
        value: Int,
    ) {
        dataStore.edit {
            it[intPreferencesKey(key.value)] = value
        }
    }

    override suspend fun removeLegacy(
        stringPrefs: List<String>,
        booleanPrefs: List<String>,
        intPrefs: List<String>,
    ) {
        dataStore.edit { ds ->
            stringPrefs.forEach {
                ds.remove(stringPreferencesKey(it))
            }
            booleanPrefs.forEach {
                ds.remove(booleanPreferencesKey(it))
            }
            intPrefs.forEach {
                ds.remove(intPreferencesKey(it))
            }
        }
    }

    private inline fun <T, R> Flow<T>.mapDistinct(crossinline transform: suspend (value: T) -> R) =
        map(transform).distinctUntilChanged()

    companion object {
        const val FILE = "datastore.preferences_pb"
    }
}
