package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

class SettingsViewModel(
    private val repository: JournalRepository,
    private val getAppVersion: () -> String,
    private val prefManager: PrefManager,
    private val journalEntryDao: JournalEntryDao?,
    private val conflictDao: EntryConflictDao?,
) : ViewModel() {
    private val uploadLoading = MutableStateFlow(false)
    private val statsRequested: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val state =
        combine(
            uploadLoading,
            prefManager.showEmptyTags(),
            prefManager.copyWithEmptyTags(),
            prefManager.showConflictDiffInline(),
            statsRequested,
            prefManager.getDataHostProperties(),
        ) {
                uploadLoading,
                showEmptyTags,
                copyWithEmptyTags,
                showConflictDiffInline,
                statsRequested,
                dataHostProperties,
            ->
            SettingsViewState(
                uploadLoading = uploadLoading,
                dataHostProperties = dataHostProperties,
                appVersion = getAppVersion(),
                showConflictDiffInline = showConflictDiffInline,
                showEmptyTags = showEmptyTags,
                copyWithEmptyTags = copyWithEmptyTags,
                allowDelete = journalEntryDao != null && conflictDao != null,
                stats = if (statsRequested) repository.getStats() else null,
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
        viewModelScope.launch {
            val newDataHostProperties =
                prefManager.getDataHostProperties().first().let { existing ->
                    val newOtherHosts =
                        existing
                            .otherHosts
                            .plus(existing.dataHost)
                            .filter { it.isNotEmpty() && it != "http://" }
                            .toSet()
                    existing.copy(
                        deviceName = deviceName.name,
                        exchangeName = exchangeName.name,
                        dataHost = dataHost.host,
                        username = username.username,
                        password = password.password,
                        otherHosts = newOtherHosts,
                    )
                }
            prefManager.setDataHostProperties(newDataHostProperties)
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

    fun onStatsRequestToggled() {
        statsRequested.update { !it }
    }

    companion object {
        fun factory() =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return SettingsViewModel(
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
    val dataHostProperties: DataHostProperties = DataHostProperties(),
    val appVersion: String = "",
    val showConflictDiffInline: Boolean = false,
    val showEmptyTags: Boolean = false,
    val copyWithEmptyTags: Boolean = false,
    val showJournalImportButton: Boolean = ServiceLocator.allowJournalImport,
    val allowDelete: Boolean = false,
    val stats: EntryStats? = null,
)
