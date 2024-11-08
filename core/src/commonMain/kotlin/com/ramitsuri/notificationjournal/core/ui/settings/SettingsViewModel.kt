package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

class SettingsViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val getAppVersion: () -> String,
) : ViewModel() {
    private val _state: MutableStateFlow<SettingsViewState>
    val state: StateFlow<SettingsViewState>

    init {
        _state = MutableStateFlow(
            SettingsViewState(
                uploadLoading = false,
                error = null,
                dataHost = DataHost(getDataHost()),
                exchangeName = ExchangeName(getExchangeName()),
                deviceName = DeviceName(getDeviceName()),
                username = Username(getUsername()),
                password = Password(getPassword()),
                appVersion = getAppVersion(),
                showReconciled = getShowReconciled(),
                showConflictDiffInline = getShowConflictDiffInline(),
            )
        )
        state = _state
    }

    fun upload() {
        _state.update {
            it.copy(uploadLoading = true)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.sync()
                _state.update {
                    it.copy(uploadLoading = false)
                }
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
        _state.update {
            it.copy(
                exchangeName = exchangeName,
                dataHost = dataHost,
                deviceName = deviceName
            )
        }
    }

    fun toggleShowReconciled() {
        keyValueStore.putBoolean(Constants.PREF_KEY_SHOW_RECONCILED, !getShowReconciled())
        _state.update {
            it.copy(showReconciled = getShowReconciled())
        }
    }

    fun toggleShowConflictDiffInline() {
        keyValueStore.putBoolean(
            Constants.PREF_SHOW_CONFLICT_DIFF_INLINE,
            !getShowConflictDiffInline()
        )
        _state.update {
            it.copy(showConflictDiffInline = getShowConflictDiffInline())
        }
    }

    fun onErrorAcknowledged() {
        _state.update {
            it.copy(error = null)
        }
    }

    private fun getDeviceName() = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "") ?: ""

    private fun getExchangeName() =
        keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "") ?: ""

    private fun getDataHost() = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "") ?: ""

    private fun getUsername() = keyValueStore.getString(Constants.PREF_KEY_USERNAME, "") ?: ""

    private fun getPassword() = keyValueStore.getString(Constants.PREF_KEY_PASSWORD, "") ?: ""

    private fun getShowReconciled() =
        keyValueStore.getBoolean(Constants.PREF_KEY_SHOW_RECONCILED, false)

    private fun getShowConflictDiffInline() =
        keyValueStore.getBoolean(Constants.PREF_SHOW_CONFLICT_DIFF_INLINE, false)

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                return SettingsViewModel(
                    keyValueStore = ServiceLocator.keyValueStore,
                    repository = ServiceLocator.repository,
                    getAppVersion = ServiceLocator::getAppVersion,
                ) as T
            }
        }
    }
}

data class SettingsViewState(
    val uploadLoading: Boolean,
    val error: String? = null,
    val dataHost: DataHost = DataHost(""),
    val exchangeName: ExchangeName = ExchangeName(""),
    val deviceName: DeviceName = DeviceName(""),
    val username: Username = Username(""),
    val password: Password = Password(""),
    val appVersion: String = "",
    val showReconciled: Boolean = false,
    val showConflictDiffInline: Boolean = false,
)
