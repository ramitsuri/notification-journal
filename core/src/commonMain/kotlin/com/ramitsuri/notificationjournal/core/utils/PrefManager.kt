package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.WindowPosition
import com.ramitsuri.notificationjournal.core.model.WindowSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class PrefManager(
    private val json: Json,
    private val keyValueStore: KeyValueStoreV2,
) {
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

    fun showSuggestions(): Flow<Boolean> {
        return keyValueStore.getBooleanFlow(Key.SHOW_SUGGESTIONS, false)
    }

    suspend fun setShowSuggestions(showSuggestions: Boolean) {
        keyValueStore.putBoolean(Key.SHOW_SUGGESTIONS, showSuggestions)
    }

    fun getExportDirectory(): Flow<String> {
        return keyValueStore
            .getStringFlow(Key.EXPORT_DIRECTORY, "")
            .map { it ?: "" }
    }

    suspend fun setExportDirectory(directory: String) {
        keyValueStore.putString(Key.EXPORT_DIRECTORY, directory)
    }

    fun getDataHostProperties(): Flow<DataHostProperties> {
        return keyValueStore
            .getStringFlow(Key.DATA_HOST_PROPERTIES, "")
            .map { string ->
                runCatching {
                    json.decodeFromString<DataHostProperties>(
                        string ?: "",
                    )
                }.getOrDefault(DataHostProperties())
            }
    }

    suspend fun setDataHostProperties(dataHostProperties: DataHostProperties) {
        keyValueStore.putString(Key.DATA_HOST_PROPERTIES, json.encodeToString(dataHostProperties))
    }

    suspend fun getWindowSize(): WindowSize? {
        return keyValueStore
            .getStringFlow(Key.WINDOW_SIZE, "")
            .map { string ->
                runCatching { json.decodeFromString<WindowSize>(string ?: "") }.getOrNull()
            }
            .first()
    }

    suspend fun setWindowSize(windowSize: WindowSize) {
        keyValueStore.putString(Key.WINDOW_SIZE, json.encodeToString(windowSize))
    }

    suspend fun getWindowPosition(): WindowPosition? {
        return keyValueStore
            .getStringFlow(Key.WINDOW_POSITION, "")
            .map { string ->
                runCatching { json.decodeFromString<WindowPosition>(string ?: "") }.getOrNull()
            }
            .first()
    }

    suspend fun setWindowPosition(windowPosition: WindowPosition) {
        keyValueStore.putString(Key.WINDOW_POSITION, json.encodeToString(windowPosition))
    }
}
