package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

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

    fun showReconciled(): Flow<Boolean> {
        return keyValueStore.getBooleanFlow(Key.SHOW_RECONCILED, false)
    }

    suspend fun setShowReconciled(showReconciled: Boolean) {
        keyValueStore.putBoolean(Key.SHOW_RECONCILED, showReconciled)
    }

    fun showConflictDiffInline(): Flow<Boolean> {
        return keyValueStore.getBooleanFlow(Key.CONFLICT_DIFF_INLINE, true)
    }

    suspend fun setShowConflictDiffInline(showConflictDiffInline: Boolean) {
        keyValueStore.putBoolean(Key.CONFLICT_DIFF_INLINE, showConflictDiffInline)
    }

    fun getLastImportDate(): Flow<Instant?> {
        return keyValueStore
            .getStringFlow(Key.LAST_IMPORT_DATE, "")
            .map { timeString ->
                timeString
                    ?.let {
                        runCatching { Instant.parse(it) }.getOrNull()
                    }
            }
    }

    suspend fun setLastImportDate(time: Instant) {
        keyValueStore.putString(Key.LAST_IMPORT_DATE, time.toString())
    }

    fun getLastImportDir(): Flow<String> {
        return keyValueStore
            .getStringFlow(Key.LAST_IMPORT_DIRECTORY, "")
            .map { it ?: "" }
    }

    suspend fun setLastImportDir(dir: String) {
        keyValueStore.putString(Key.LAST_IMPORT_DIRECTORY, dir)
    }

    suspend fun getDefaultTag(): String {
        return keyValueStore.getString(Key.DEFAULT_TAG, Tag.NO_TAG.value)
    }

    fun getDefaultTagFlow(): Flow<String> {
        return keyValueStore.getStringFlow(Key.DEFAULT_TAG, Tag.NO_TAG.value).map {
            it ?: Tag.NO_TAG.value
        }
    }

    suspend fun setDefaultTag(tag: String) {
        keyValueStore.putString(Key.DEFAULT_TAG, tag)
    }

    suspend fun removeLegacy(
        stringPrefs: List<String> = listOf(),
        booleanPrefs: List<String> = listOf(),
        intPrefs: List<String> = listOf(),
    ) {
        keyValueStore.removeLegacy(stringPrefs = stringPrefs, booleanPrefs = booleanPrefs, intPrefs = intPrefs)
    }
}
