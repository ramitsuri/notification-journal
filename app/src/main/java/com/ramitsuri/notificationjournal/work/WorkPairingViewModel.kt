package com.ramitsuri.notificationjournal.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.di.IoDispatcher
import com.ramitsuri.notificationjournal.core.utils.QrCodeParser
import com.ramitsuri.notificationjournal.core.utils.WorkProfileManager
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class WorkPairingViewModel(
    private val workProfileManager: WorkProfileManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private var isHandled: Boolean = false

    fun onQrCodeScanned(qrCodeContent: String): Boolean {
        if (isHandled) {
            return false
        }
        val profile = QrCodeParser.parseWorkQr(qrCodeContent) ?: return false
        isHandled = true
        viewModelScope.launch(ioDispatcher) {
            workProfileManager.setProfile(profile)
            _isPaired.value = true
        }
        return true
    }
}
