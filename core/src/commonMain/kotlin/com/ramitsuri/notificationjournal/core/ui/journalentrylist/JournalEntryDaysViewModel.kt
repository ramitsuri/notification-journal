package com.ramitsuri.notificationjournal.core.ui.journalentrylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.ReconcileCheckDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckEvaluator
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.combine
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class JournalEntryDaysViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        private val reconcileCheckDao: ReconcileCheckDao,
        private val tagsDao: TagsDao,
        private val verifyEntriesHelper: VerifyEntriesHelper,
        webSocketHelper: WebSocketHelper,
        private val lifecycleCoordinator: com.ramitsuri.notificationjournal.core.sync.AppLifecycleCoordinator,
        clock: Clock = Clock.System,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) : ViewModel() {
        private val selectedDate = MutableStateFlow<LocalDate?>(null)
        private val verificationMap = MutableStateFlow(mapOf<LocalDate, DateWithCount.Verification>())
        private val isVerifying = MutableStateFlow(false)
        private val reconcileAllDialogState = MutableStateFlow<ReconcileAllDialogState?>(null)
        private var verifyJob: Job? = null

        val state: StateFlow<ViewState> =
            combine(
                selectedDate,
                repository.getNotReconciledDateWithCountsFlow(),
                repository.getForUploadCountFlow(),
                verificationMap,
                webSocketHelper.isConnected,
                isVerifying,
                reconcileAllDialogState,
            ) {
                selectedDate,
                countAndDates,
                forUploadCount,
                verificationMap,
                isConnected,
                isVerifying,
                reconcileAllDialogState,
                ->
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
                    isVerifying = isVerifying,
                    reconcileAllDialogState = reconcileAllDialogState,
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
            if (verifyJob?.isActive == true) {
                Logger.withTag(TAG).d { "Verification already in progress, skipping" }
                return
            }
            val datesForVerification = state.value.dateWithCountList.map { it.date }
            if (datesForVerification.isEmpty()) {
                return
            }
            verifyJob =
                viewModelScope.launch {
                    isVerifying.value = true
                    try {
                        val jobs =
                            datesForVerification.map { date ->
                                launch { verifyForDate(date) }
                            }
                        jobs.joinAll()
                    } finally {
                        isVerifying.value = false
                    }
                }
        }

        fun onReconcileAll(upload: Boolean) {
            val blocker = hardBlockerOrNull()
            if (blocker != null) {
                Logger.withTag(TAG).w { "Entries have conflicts or untagged entries, cannot reconcile" }
                reconcileAllDialogState.value = blocker
                return
            }
            viewModelScope.launch {
                val enabledChecks = reconcileCheckDao.getEnabled()
                if (enabledChecks.isNotEmpty()) {
                    val tags = tagsDao.getAll().map { it.value }
                    val dayGroups = repository.getAll().toDayGroups(tags)
                    val failedDays =
                        dayGroups.mapNotNull { dayGroup ->
                            val failedChecks =
                                ReconcileCheckEvaluator.evaluate(
                                    dayGroup = dayGroup,
                                    checks = enabledChecks,
                                )
                            if (failedChecks.isNotEmpty()) {
                                FailedDayChecks(
                                    date = dayGroup.date,
                                    failedCheckSummaries = failedChecks.map { it.summary() },
                                )
                            } else {
                                null
                            }
                        }
                    if (failedDays.isNotEmpty()) {
                        reconcileAllDialogState.value =
                            ReconcileAllDialogState.FailedChecks(
                                failedDays = failedDays,
                                upload = upload,
                            )
                        return@launch
                    }
                }
                reconcileAllNow(upload)
            }
        }

        /** Proceeds with reconciling all unreconciled days after the user overrides failed Reconcile Checks. */
        fun confirmReconcileAllAnyway() {
            val failedDialog = reconcileAllDialogState.value as? ReconcileAllDialogState.FailedChecks ?: return
            reconcileAllDialogState.value = null
            val blocker = hardBlockerOrNull()
            if (blocker != null) {
                reconcileAllDialogState.value = blocker
                return
            }
            viewModelScope.launch {
                reconcileAllNow(failedDialog.upload)
            }
        }

        /** Dismisses any active Reconcile all blocker or warning dialog. */
        fun dismissReconcileAllDialog() {
            reconcileAllDialogState.value = null
        }

        private suspend fun reconcileAllNow(upload: Boolean) {
            repository.markAllReconciled()
            selectedDate.value = null
            if (upload) {
                repository.uploadAll()
            }
        }

        private fun hardBlockerOrNull(): ReconcileAllDialogState.HardBlocker? {
            val dateWithCounts = state.value.dateWithCountList
            val untaggedCount = dateWithCounts.sumOf { it.untaggedCount }
            val conflictCount = dateWithCounts.sumOf { it.conflictCount }
            return if (untaggedCount > 0 || conflictCount > 0) {
                ReconcileAllDialogState.HardBlocker(
                    untaggedCount = untaggedCount,
                    conflictCount = conflictCount,
                )
            } else {
                null
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
    val isVerifying: Boolean = false,
    val reconcileAllDialogState: ReconcileAllDialogState? = null,
)

sealed interface ReconcileAllDialogState {
    /** Built-in blockers (untagged entries and/or unresolved conflicts) across unreconciled days. */
    data class HardBlocker(
        val untaggedCount: Int,
        val conflictCount: Int,
    ) : ReconcileAllDialogState

    /** Soft warnings for days whose active [com.ramitsuri.notificationjournal.core.model.ReconcileCheck]s failed. */
    data class FailedChecks(
        val failedDays: List<FailedDayChecks>,
        val upload: Boolean,
    ) : ReconcileAllDialogState
}

/** Failed [com.ramitsuri.notificationjournal.core.model.ReconcileCheck] summaries for a single unreconciled day. */
data class FailedDayChecks(
    val date: LocalDate,
    val failedCheckSummaries: List<String>,
)
