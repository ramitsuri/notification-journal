package com.ramitsuri.notificationjournal.core.utils

import kotlinx.coroutines.flow.Flow

class PrefManager(private val keyValueStore: KeyValueStoreV2) {
    fun showEmptyTags(): Flow<Boolean> {
        return keyValueStore.getBooleanFlow(Key.SHOW_EMPTY_TAGS, false)
    }

    suspend fun setShowEmptyTags(showEmptyTags: Boolean) {
        keyValueStore.putBoolean(Key.SHOW_EMPTY_TAGS, showEmptyTags)
    }

    fun copyWithEmptyTags(): Flow<Boolean> {
        return keyValueStore.getBooleanFlow(Key.COPY_WITH_EMPTY_TAGS, false)
    }

    suspend fun setCopyWithEmptyTags(copyWithEmptyTags: Boolean) {
        keyValueStore.putBoolean(Key.COPY_WITH_EMPTY_TAGS, copyWithEmptyTags)
    }
}
