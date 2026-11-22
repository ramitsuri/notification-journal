package com.ramitsuri.notificationjournal.work

import com.ramitsuri.notificationjournal.core.utils.Key
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeKeyValueStore : KeyValueStoreV2 {
    private val values = MutableStateFlow<Map<Key, Any>>(emptyMap())

    override fun getBooleanFlow(
        key: Key,
        defaultValue: Boolean,
    ): Flow<Boolean> = values.map { (it[key] as? Boolean) ?: defaultValue }

    override suspend fun getBoolean(
        key: Key,
        defaultValue: Boolean,
    ): Boolean = (values.value[key] as? Boolean) ?: defaultValue

    override suspend fun putBoolean(
        key: Key,
        value: Boolean,
    ) {
        values.value = values.value + (key to value)
    }

    override fun getStringFlow(
        key: Key,
        fallback: String,
    ): Flow<String?> = values.map { (it[key] as? String) ?: fallback }

    override suspend fun getString(
        key: Key,
        fallback: String,
    ): String = (values.value[key] as? String) ?: fallback

    override suspend fun putString(
        key: Key,
        value: String,
    ) {
        values.value = values.value + (key to value)
    }

    override fun getIntFlow(
        key: Key,
        fallback: Int,
    ): Flow<Int> = values.map { (it[key] as? Int) ?: fallback }

    override suspend fun getInt(
        key: Key,
        fallback: Int,
    ): Int = (values.value[key] as? Int) ?: fallback

    override suspend fun putInt(
        key: Key,
        value: Int,
    ) {
        values.value = values.value + (key to value)
    }

    override suspend fun removeLegacy(
        stringPrefs: List<String>,
        booleanPrefs: List<String>,
        intPrefs: List<String>,
    ) {
        // No-op
    }
}
