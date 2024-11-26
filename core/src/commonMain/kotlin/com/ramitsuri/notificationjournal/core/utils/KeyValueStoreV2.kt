package com.ramitsuri.notificationjournal.core.utils

import kotlinx.coroutines.flow.Flow

interface KeyValueStoreV2 {
    fun getBooleanFlow(key: Key, defaultValue: Boolean): Flow<Boolean>
    suspend fun getBoolean(key: Key, defaultValue: Boolean): Boolean
    suspend fun putBoolean(key: Key, value: Boolean)

    fun getStringFlow(key: String, fallback: String): Flow<String?>
    suspend fun getString(key: String, fallback: String): String
    suspend fun putString(key: String, value: String)

    fun getIntFlow(key: String, fallback: Int): Flow<Int>
    suspend fun getInt(key: String, fallback: Int): Int
    suspend fun putInt(key: String, value: Int)

    suspend fun hasKey(key: String): Boolean
}

enum class Key(val value: String) {
    SHOW_EMPTY_TAGS("show_empty_tags"),
    COPY_WITH_EMPTY_TAGS("copy_with_empty_tags"),
    SHOW_RECONCILED("show_reconciled"),
    CONFLICT_DIFF_INLINE("show_conflict_diff_inline"),
    ;
}
