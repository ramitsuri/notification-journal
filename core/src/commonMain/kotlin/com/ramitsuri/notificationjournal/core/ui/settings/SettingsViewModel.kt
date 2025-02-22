package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

class SettingsViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val getAppVersion: () -> String,
    private val prefManager: PrefManager,
    private val journalEntryDao: JournalEntryDao?,
    private val conflictDao: EntryConflictDao?,
) : ViewModel() {
    private val uploadLoading = MutableStateFlow(false)

    // Because some prefs are stored in non reactive storage
    private val prefUpdated = MutableStateFlow(0)

    val state =
        combine(
            prefUpdated,
            uploadLoading,
            prefManager.showEmptyTags(),
            prefManager.copyWithEmptyTags(),
            prefManager.showReconciled(),
            prefManager.showConflictDiffInline(),
        ) { _, uploadLoading, showEmptyTags, copyWithEmptyTags, showReconciled, showConflictDiffInline,
            ->
            SettingsViewState(
                uploadLoading = uploadLoading,
                dataHost = DataHost(getDataHost()),
                exchangeName = ExchangeName(getExchangeName()),
                deviceName = DeviceName(getDeviceName()),
                username = Username(getUsername()),
                password = Password(getPassword()),
                appVersion = getAppVersion(),
                showReconciled = showReconciled,
                showConflictDiffInline = showConflictDiffInline,
                showEmptyTags = showEmptyTags,
                copyWithEmptyTags = copyWithEmptyTags,
                allowDelete = journalEntryDao != null && conflictDao != null,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsViewState(),
        )

    fun upload() {
        uploadLoading.update { true }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.uploadAll()
                uploadLoading.update { false }
            }
        }
    }

    fun setDataSharingProperties(
        dataHost: DataHost,
        exchangeName: ExchangeName,
        deviceName: DeviceName,
        username: Username,
        password: Password,
    ) {
        keyValueStore.putString(Constants.PREF_KEY_DATA_HOST, dataHost.host)
        keyValueStore.putString(Constants.PREF_KEY_EXCHANGE_NAME, exchangeName.name)
        keyValueStore.putString(Constants.PREF_KEY_DEVICE_NAME, deviceName.name)
        keyValueStore.putString(Constants.PREF_KEY_USERNAME, username.username)
        keyValueStore.putString(Constants.PREF_KEY_PASSWORD, password.password)
        prefUpdated.update { it + 1 }
    }

    fun toggleShowReconciled() {
        viewModelScope.launch {
            prefManager.setShowReconciled(state.value.showReconciled.not())
        }
    }

    fun toggleShowConflictDiffInline() {
        viewModelScope.launch {
            prefManager.setShowConflictDiffInline(state.value.showConflictDiffInline.not())
        }
    }

    fun toggleShowEmptyTags() {
        viewModelScope.launch {
            prefManager.setShowEmptyTags(state.value.showEmptyTags.not())
        }
    }

    fun toggleCopyWithEmptyTags() {
        viewModelScope.launch {
            prefManager.setCopyWithEmptyTags(state.value.copyWithEmptyTags.not())
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            journalEntryDao?.deleteAll()
            conflictDao?.deleteAll()
        }
    }

    private fun getDeviceName() = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "") ?: ""

    private fun getExchangeName() = keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "") ?: ""

    private fun getDataHost() = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "") ?: ""

    private fun getUsername() = keyValueStore.getString(Constants.PREF_KEY_USERNAME, "") ?: ""

    private fun getPassword() = keyValueStore.getString(Constants.PREF_KEY_PASSWORD, "") ?: ""

    companion object {
        fun factory() =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return SettingsViewModel(
                        keyValueStore = ServiceLocator.keyValueStore,
                        repository = ServiceLocator.repository,
                        getAppVersion = ServiceLocator::getAppVersion,
                        prefManager = ServiceLocator.prefManager,
                        journalEntryDao = ServiceLocator.journalEntryDao,
                        conflictDao = ServiceLocator.conflictDao,
                    ) as T
                }
            }
    }
}

data class SettingsViewState(
    val uploadLoading: Boolean = false,
    val dataHost: DataHost = DataHost(""),
    val exchangeName: ExchangeName = ExchangeName(""),
    val deviceName: DeviceName = DeviceName(""),
    val username: Username = Username(""),
    val password: Password = Password(""),
    val appVersion: String = "",
    val showReconciled: Boolean = false,
    val showConflictDiffInline: Boolean = false,
    val showEmptyTags: Boolean = false,
    val copyWithEmptyTags: Boolean = false,
    val showJournalImportButton: Boolean = ServiceLocator.allowJournalImport,
    val allowDelete: Boolean = false,
)
