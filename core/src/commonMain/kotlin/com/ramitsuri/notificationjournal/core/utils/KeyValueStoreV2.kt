package com.ramitsuri.notificationjournal.core.utils

import kotlinx.coroutines.flow.Flow

interface KeyValueStoreV2 {
    fun getBooleanFlow(key: Key, defaultValue: Boolean): Flow<Boolean>
    suspend fun getBoolean(key: Key, defaultValue: Boolean): Boolean
    suspend fun putBoolean(key: Key, value: Boolean)
}

enum class Key(val value: String) {
    SHOW_EMPTY_TAGS("show_empty_tags"),
    COPY_WITH_EMPTY_TAGS("copy_with_empty_tags"),
    ;
}
