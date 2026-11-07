package com.ramitsuri.notificationjournal.core.ui.journalentrylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class JournalEntryDaysViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        private val verifyEntriesHelper: VerifyEntriesHelper,
        webSocketHelper: WebSocketHelper,
        private val lifecycleCoordinator: com.ramitsuri.notificationjournal.core.sync.AppLifecycleCoordinator,
        clock: Clock = Clock.System,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) : ViewModel() {
        private val selectedDate = MutableStateFlow<LocalDate?>(null)
        private val verificationMap = MutableStateFlow(mapOf<LocalDate, DateWithCount.Verification>())

        val state: StateFlow<ViewState> =
            combine(
                selectedDate,
                repository.getNotReconciledDateWithCountsFlow(),
                repository.getForUploadCountFlow(),
                verificationMap,
                webSocketHelper.isConnected,
            ) { selectedDate, countAndDates, forUploadCount, verificationMap, isConnected ->
                ViewState(
                    selectedDate = selectedDate,
                    dateWithCountList =
                        countAndDates.map {
                            it.copy(
                                verification = verificationMap[it.date] ?: DateWithCount.Verification.NotVerified,
                            )
                        },
                    isConnected = isConnected,
                    notUploadedCount = forUploadCount,
                    todayDate = clock.now().toLocalDateTime(timeZone).date,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ViewState(),
            )

        fun onDateSelected(date: LocalDate?) {
            selectedDate.update { date }
        }

        fun resetReceiveHelper() {
            lifecycleCoordinator.resetReceiveHelper(resetWebsocket = true)
        }

        fun onVerifyEntriesRequested() {
            val datesForVerification = state.value.dateWithCountList.map { it.date }
            viewModelScope.launch {
                datesForVerification.forEach { date ->
                    launch { verifyForDate(date) }
                }
            }
        }

        fun onReconcileAll(upload: Boolean) {
            val anyConflictsOrUntagged =
                state.value.dateWithCountList.any {
                    it.untaggedCount > 0 ||
                        it.conflictCount > 0
                }
            if (anyConflictsOrUntagged) {
                Logger.withTag(TAG).w { "Entries have conflicts or untagged entries, cannot reconcile" }
                return
            }
            viewModelScope.launch {
                repository.markAllReconciled()
                selectedDate.value = null
                if (upload) {
                    repository.uploadAll()
                }
            }
        }

        fun sync() {
            Logger.withTag(TAG).d { "Attempting to sync" }
            viewModelScope.launch {
                repository.uploadAll()
            }
        }

        private suspend fun verifyForDate(date: LocalDate) {
            verificationMap.update {
                it + (date to DateWithCount.Verification.InProgress)
            }
            val result = verifyEntriesHelper.requestVerifyEntries(date)
            if (result == null) {
                verificationMap.update {
                    it + (date to DateWithCount.Verification.NotVerified)
                }
            } else {
                verificationMap.update {
                    it + (date to DateWithCount.Verification.Verified(result))
                }
            }
        }

        companion object {
            private const val TAG = "JournalEntryDaysViewModel"
        }
    }

data class ViewState(
    val selectedDate: LocalDate? = null,
    val dateWithCountList: List<DateWithCount> = listOf(),
    val todayDate: LocalDate? = null,
    val isConnected: Boolean = false,
    val notUploadedCount: Int = 0,
)
