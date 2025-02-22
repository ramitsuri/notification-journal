package com.ramitsuri.notificationjournal.core.utils

import kotlinx.coroutines.flow.Flow

interface KeyValueStoreV2 {
    fun getBooleanFlow(
        key: Key,
        defaultValue: Boolean,
    ): Flow<Boolean>

    suspend fun getBoolean(
        key: Key,
        defaultValue: Boolean,
    ): Boolean

    suspend fun putBoolean(
        key: Key,
        value: Boolean,
    )

    fun getStringFlow(
        key: Key,
        fallback: String,
    ): Flow<String?>

    suspend fun getString(
        key: Key,
        fallback: String,
    ): String

    suspend fun putString(
        key: Key,
        value: String,
    )

    fun getIntFlow(
        key: Key,
        fallback: Int,
    ): Flow<Int>

    suspend fun getInt(
        key: Key,
        fallback: Int,
    ): Int

    suspend fun putInt(
        key: Key,
        value: Int,
    )

    suspend fun removeLegacy(
        stringPrefs: List<String>,
        booleanPrefs: List<String>,
        intPrefs: List<String>,
    )
}

enum class Key(val value: String) {
    SHOW_EMPTY_TAGS("show_empty_tags"),
    COPY_WITH_EMPTY_TAGS("copy_with_empty_tags"),
    CONFLICT_DIFF_INLINE("show_conflict_diff_inline"),
    LAST_IMPORT_DATE("last_import_date"),
    LAST_IMPORT_DIRECTORY("last_import_directory"),
    DEFAULT_TAG("default_tag"),
}
